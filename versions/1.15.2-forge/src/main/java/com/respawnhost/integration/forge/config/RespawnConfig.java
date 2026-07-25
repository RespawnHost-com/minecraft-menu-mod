package com.respawnhost.integration.forge.config;

import com.respawnhost.core.config.ConfigStore;
import com.respawnhost.core.config.RespawnConfigData;
import net.minecraftforge.fml.loading.FMLPaths;

public final class RespawnConfig {
    private static final String FILE_NAME = "respawnhost_integration.json";
    private static volatile RespawnConfig instance;

    private final ConfigStore store;
    private final RespawnConfigData data;

    private RespawnConfig(ConfigStore store, RespawnConfigData data) {
        this.store = store;
        this.data = data;
    }

    public static RespawnConfig get() {
        RespawnConfig current = instance;
        if (current == null) {
            synchronized (RespawnConfig.class) {
                current = instance;
                if (current == null) {
                    ConfigStore store = new ConfigStore(FMLPaths.CONFIGDIR.get().resolve(FILE_NAME));
                    current = new RespawnConfig(store, store.load());
                    instance = current;
                }
            }
        }
        return current;
    }

    public synchronized void save() {
        store.save(data);
    }

    public String creatorCode() {
        return data.getCreatorCode();
    }

    public void creatorCode(String creatorCode) {
        data.setCreatorCode(creatorCode);
    }

    public String getPackId() {
        return data.getPackId();
    }

    public void setPackId(String packId) {
        data.setPackId(packId);
    }

    public String getApiBaseUrl() {
        return data.getApiBaseUrl();
    }

    public String getOrderBaseUrl() {
        return data.getOrderBaseUrl();
    }

    public String getPanelBaseUrl() {
        return data.getPanelBaseUrl();
    }

    public String getGameShort() {
        return data.getGameShort();
    }

    public String getRegion() {
        return data.getRegion();
    }

    public void setRegion(String region) {
        data.setRegion(region);
    }

    public boolean isShowOrderButton() {
        return data.isShowOrderButton();
    }

    public void setShowOrderButton(boolean showOrderButton) {
        data.setShowOrderButton(showOrderButton);
    }
}
