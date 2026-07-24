package com.respawnhost.integration;

import com.respawnhost.integration.ui.MultiplayerButtonInjector;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(RespawnHostIntegrationMod.MOD_ID)
public final class RespawnHostIntegrationMod {
    public static final String MOD_ID = "respawnhost_integration";
    public static final Logger LOGGER = LogManager.getLogger("RespawnHost Integration Menu");

    public RespawnHostIntegrationMod() {
        DistExecutor.runWhenOn(Dist.CLIENT, () -> MultiplayerButtonInjector::register);
    }

    public static int loadedModCount() {
        try {
            return ModList.get().size();
        } catch (RuntimeException e) {
            return 0;
        }
    }
}
