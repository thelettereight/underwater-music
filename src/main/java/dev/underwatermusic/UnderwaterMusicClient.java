package dev.underwatermusic;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnderwaterMusicClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("UnderwaterMusic");

    @Override
    public void onInitializeClient() {
        UMConfig.load();
        ClientTickEvents.END_CLIENT_TICK.register(client -> UnderwaterAudio.tick(client));
        LOGGER.info("Underwater Music initialised");
    }
}
