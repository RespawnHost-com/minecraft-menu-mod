package com.respawnhost.integration;

import com.respawnhost.integration.ui.MultiplayerButtonInjector;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = RespawnHostIntegration.MODID, name = "RespawnHost Integration Menu", version = "1.0.0", clientSideOnly = true)
public class RespawnHostIntegration {
    public static final String MODID = "respawnhost_integration";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new MultiplayerButtonInjector());
    }
}
