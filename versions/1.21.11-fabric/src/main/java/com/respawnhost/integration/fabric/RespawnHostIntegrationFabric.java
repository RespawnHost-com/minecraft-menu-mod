package com.respawnhost.integration.fabric;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RespawnHostIntegrationFabric implements ClientModInitializer {
    public static final String MOD_ID = "respawnhost_integration";
    public static final Logger LOGGER = LoggerFactory.getLogger("RespawnHost Integration Menu");

    @Override
    public void onInitializeClient() {
    }
}
