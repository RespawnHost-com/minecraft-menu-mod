package com.respawnhost.integration.fabric;

import com.respawnhost.integration.RespawnHostIntegration;
import net.fabricmc.api.ClientModInitializer;

public final class RespawnHostIntegrationFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        RespawnHostIntegration.init();
    }
}
