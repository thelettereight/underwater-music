package dev.underwatermusic.mixin;

import dev.underwatermusic.UnderwaterAudio;
import java.util.Map;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {
    @Shadow @Final private Map<SoundInstance, ChannelAccess.ChannelHandle> instanceToChannel;

    /**
     * After a sound starts, remember it if (and only if) it belongs to the MUSIC category.
     * Music discs use SoundSource.RECORDS and are therefore never tracked.
     */
    @Inject(method = "play", at = @At("RETURN"))
    private void um$afterPlay(SoundInstance instance, CallbackInfoReturnable<?> cir) {
        if (instance.getSource() == SoundSource.MUSIC) {
            ChannelAccess.ChannelHandle handle = this.instanceToChannel.get(instance);
            if (handle != null) {
                UnderwaterAudio.track(instance, handle);
            }
        }
    }
}
