package com.respawnhost.integration;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public class RespawnHostIntegrationFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
    }

    public static int loadedModCount() {
        try {
            return FabricLoader.getInstance().getAllMods().size();
        } catch (RuntimeException e) {
            return 0;
        }
    }
}
