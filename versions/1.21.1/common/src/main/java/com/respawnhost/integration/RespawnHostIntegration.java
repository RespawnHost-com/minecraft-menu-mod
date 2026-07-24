package com.respawnhost.integration;

import com.respawnhost.integration.ui.MultiplayerButtonInjector;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RespawnHostIntegration {
    public static final String MOD_ID = "respawnhost_integration";
    public static final Logger LOGGER = LoggerFactory.getLogger("RespawnHost Integration Menu");

    private RespawnHostIntegration() {
    }

    public static void init() {
        EnvExecutor.runInEnv(Env.CLIENT, () -> MultiplayerButtonInjector::register);
    }
}
