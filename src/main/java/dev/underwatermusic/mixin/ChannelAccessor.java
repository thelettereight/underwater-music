package dev.underwatermusic.mixin;

import com.mojang.blaze3d.audio.Channel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Channel.class)
public interface ChannelAccessor {
    /** The raw OpenAL source id of this channel. */
    @Accessor("source")
    int um$getSource();
}
