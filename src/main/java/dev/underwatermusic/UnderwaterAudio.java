package dev.underwatermusic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import dev.underwatermusic.mixin.ChannelAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.EXTEfx;

/**
 * Applies an OpenAL low-pass filter (EFX), gain reduction and pitch reduction to
 * background-music sources only. All OpenAL calls run on Minecraft's sound thread
 * via ChannelHandle.execute().
 */
public final class UnderwaterAudio {
    private record Tracked(SoundInstance instance, ChannelAccess.ChannelHandle handle) {}

    // Only touched from the client (render) thread.
    private static final List<Tracked> TRACKED = new ArrayList<>();
    private static float progress = 0f;
    private static long lastNanos = System.nanoTime();
    private static float lastEffective = 0f;

    // Only touched from the sound thread.
    private static volatile Boolean efxSupported = null;
    private static volatile int sharedFilter = 0;

    private UnderwaterAudio() {}

    public static void track(SoundInstance instance, ChannelAccess.ChannelHandle handle) {
        // play() may be called from the sound/other thread; synchronise the list.
        synchronized (TRACKED) {
            TRACKED.add(new Tracked(instance, handle));
        }
    }

    public static void tick(Minecraft mc) {
        UMConfig cfg = UMConfig.get();
        long now = System.nanoTime();
        float dt = Math.min(0.25f, (now - lastNanos) / 1_000_000_000f);
        lastNanos = now;

        LocalPlayer p = mc.player;
        boolean under = cfg.enabled && p != null
                && ((cfg.detectEyes && p.isEyeInFluid(FluidTags.WATER))
                || (cfg.detectSwimming && p.isSwimming()));

        float rate = dt / (float) cfg.transitionSeconds;
        if (under) {
            progress = cfg.fadeIn ? Math.min(1f, progress + rate) : 1f;
        } else {
            progress = cfg.fadeOut ? Math.max(0f, progress - rate) : 0f;
        }

        float eased = progress * progress * (3f - 2f * progress); // smoothstep
        float effective = eased * (float) (cfg.effectIntensity / 100.0);

        if (effective <= 0.0005f && lastEffective <= 0.0005f) {
            prune();
            return;
        }

        float gain = cfg.volumeReductionEnabled
                ? Mth.lerp(effective, 1f, (float) (cfg.underwaterVolume / 100.0)) : 1f;

        // OpenAL Soft's GAINHF is the gain at 5 kHz. Approximate a 1-pole low-pass at cutoff fc.
        float hfTarget = (float) (1.0 / Math.sqrt(1.0 + Math.pow(5000.0 / cfg.lowPassFrequency, 2)));
        float hf = cfg.lowPassEnabled ? Mth.lerp(effective, 1f, hfTarget) : 1f;

        float pitchMul = cfg.pitchReductionEnabled
                ? 1f - (float) (cfg.pitchReduction / 100.0) * effective : 1f;

        boolean reset = effective <= 0.0005f;
        lastEffective = effective;

        synchronized (TRACKED) {
            Iterator<Tracked> it = TRACKED.iterator();
            while (it.hasNext()) {
                Tracked t = it.next();
                if (t.handle().isStopped()) {
                    it.remove();
                    continue;
                }
                float basePitch = Mth.clamp(t.instance().getPitch(), 0.5f, 2.0f);
                final float g = reset ? 1f : gain;
                final float h = reset ? 1f : hf;
                final float pm = reset ? 1f : pitchMul;
                t.handle().execute(channel -> {
                    try {
                        int src = ((ChannelAccessor) channel).um$getSource();
                        applyToSource(src, g, h, basePitch * pm);
                    } catch (Throwable e) {
                        UnderwaterMusicClient.LOGGER.warn("Failed to apply underwater effect", e);
                    }
                });
            }
        }
    }

    private static void prune() {
        synchronized (TRACKED) {
            TRACKED.removeIf(t -> t.handle().isStopped());
        }
    }

    // ---- sound thread ----
    private static void applyToSource(int src, float gain, float hf, float pitch) {
        int f = ensureFilter();
        if (f != 0 && (gain < 0.9999f || hf < 0.9999f)) {
            EXTEfx.alFilterf(f, EXTEfx.AL_LOWPASS_GAIN, Mth.clamp(gain, 0f, 1f));
            EXTEfx.alFilterf(f, EXTEfx.AL_LOWPASS_GAINHF, Mth.clamp(hf, 0f, 1f));
            AL10.alSourcei(src, EXTEfx.AL_DIRECT_FILTER, f);
        } else if (f != 0) {
            AL10.alSourcei(src, EXTEfx.AL_DIRECT_FILTER, EXTEfx.AL_FILTER_NULL);
        }
        AL10.alSourcef(src, AL10.AL_PITCH, pitch);
    }

    private static int ensureFilter() {
        if (Boolean.FALSE.equals(efxSupported)) return 0;
        try {
            if (efxSupported == null) {
                long ctx = ALC10.alcGetCurrentContext();
                long dev = ALC10.alcGetContextsDevice(ctx);
                efxSupported = ALC10.alcIsExtensionPresent(dev, "ALC_EXT_EFX");
                if (!efxSupported) {
                    UnderwaterMusicClient.LOGGER.warn("OpenAL EFX not supported; low-pass/volume disabled");
                    return 0;
                }
            }
            if (sharedFilter == 0 || !EXTEfx.alIsFilter(sharedFilter)) {
                sharedFilter = EXTEfx.alGenFilters();
                EXTEfx.alFilteri(sharedFilter, EXTEfx.AL_FILTER_TYPE, EXTEfx.AL_FILTER_LOWPASS);
            }
            return sharedFilter;
        } catch (Throwable t) {
            efxSupported = false;
            UnderwaterMusicClient.LOGGER.warn("EFX setup failed", t);
            return 0;
        }
    }
}
