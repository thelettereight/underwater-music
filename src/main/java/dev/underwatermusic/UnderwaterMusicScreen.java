package dev.underwatermusic;

import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class UnderwaterMusicScreen extends Screen {
    private final Screen parent;
    private static final int W = 150, H = 20, GAP = 4;

    public UnderwaterMusicScreen(Screen parent) {
        super(Component.literal("Underwater Music"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        UMConfig c = UMConfig.get();
        int left = this.width / 2 - W - GAP / 2;
        int right = this.width / 2 + GAP / 2;
        int y0 = 34;
        int row = H + GAP;

        StringWidget title = new StringWidget(this.width / 2 - 100, 12, 200, 9, this.title, this.font);
        title.alignCenter();
        this.addRenderableWidget(title);

        // Left column: toggles
        toggle(left, y0, "Mod enabled", c.enabled, v -> c.enabled = v);
        toggle(left, y0 + row, "Low-pass filter", c.lowPassEnabled, v -> c.lowPassEnabled = v);
        toggle(left, y0 + row * 2, "Volume reduction", c.volumeReductionEnabled, v -> c.volumeReductionEnabled = v);
        toggle(left, y0 + row * 3, "Pitch reduction", c.pitchReductionEnabled, v -> c.pitchReductionEnabled = v);
        toggle(left, y0 + row * 4, "Detect eyes underwater", c.detectEyes, v -> c.detectEyes = v);
        toggle(left, y0 + row * 5, "Detect swimming", c.detectSwimming, v -> c.detectSwimming = v);
        toggle(left, y0 + row * 6, "Fade in", c.fadeIn, v -> c.fadeIn = v);
        toggle(left, y0 + row * 7, "Fade out", c.fadeOut, v -> c.fadeOut = v);

        // Right column: sliders
        this.addRenderableWidget(new Opt(right, y0, "Underwater volume", 0, 100, c.underwaterVolume, "%.0f%%", v -> c.underwaterVolume = v));
        this.addRenderableWidget(new Opt(right, y0 + row, "Low-pass", 200, 5000, c.lowPassFrequency, "%.0f Hz", v -> c.lowPassFrequency = v));
        this.addRenderableWidget(new Opt(right, y0 + row * 2, "Pitch reduction", 0, 20, c.pitchReduction, "%.0f%%", v -> c.pitchReduction = v));
        this.addRenderableWidget(new Opt(right, y0 + row * 3, "Transition", 0.1, 5.0, c.transitionSeconds, "%.1f s", v -> c.transitionSeconds = v));
        this.addRenderableWidget(new Opt(right, y0 + row * 4, "Effect intensity", 0, 100, c.effectIntensity, "%.0f%%", v -> c.effectIntensity = v));

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> this.onClose())
                .bounds(this.width / 2 - 100, y0 + row * 8 + 6, 200, H).build());
    }

    private void toggle(int x, int y, String label, boolean init, Consumer<Boolean> setter) {
        this.addRenderableWidget(CycleButton.onOffBuilder(init)
                .create(x, y, W, H, Component.literal(label), (btn, val) -> {
                    setter.accept(val);
                    UMConfig.save();
                }));
    }

    @Override
    public void onClose() {
        UMConfig.save();
        if (this.minecraft != null) this.minecraft.setScreen(parent);
    }

    @Override
    public void removed() {
        UMConfig.save();
    }

    private static final class Opt extends AbstractSliderButton {
        private final String label, fmt;
        private final double min, max;
        private final DoubleConsumer setter;

        Opt(int x, int y, String label, double min, double max, double cur, String fmt, DoubleConsumer setter) {
            super(x, y, W, H, Component.empty(), (cur - min) / (max - min));
            this.label = label; this.fmt = fmt; this.min = min; this.max = max; this.setter = setter;
            updateMessage();
        }

        private double val() { return min + (max - min) * this.value; }

        @Override
        protected void updateMessage() {
            if (label == null) return;
            setMessage(Component.literal(label + ": " + String.format(fmt, val())));
        }

        @Override
        protected void applyValue() {
            setter.accept(val());
            UMConfig.save();
        }
    }
}
