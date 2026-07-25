package com.respawnhost.core.config;

public class RespawnConfigData {
    private String creatorCode = "";
    private String packId = "";
    private String apiBaseUrl = "https://respawnhost.com/api";
    private String orderBaseUrl = "https://respawnhost.com/order";
    private String panelBaseUrl = "https://panel.respawnhost.com";
    private String gameShort = "minecraft";
    private String region = "eu";
    private boolean showOrderButton = true;

    public String getCreatorCode() {
        return creatorCode;
    }

    public void setCreatorCode(String creatorCode) {
        this.creatorCode = creatorCode == null ? "" : creatorCode;
    }

    public String getPackId() {
        return packId;
    }

    public void setPackId(String packId) {
        this.packId = packId == null ? "" : packId;
    }

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = isBlank(apiBaseUrl) ? "https://respawnhost.com/api" : apiBaseUrl;
    }

    public String getOrderBaseUrl() {
        return orderBaseUrl;
    }

    public void setOrderBaseUrl(String orderBaseUrl) {
        this.orderBaseUrl = isBlank(orderBaseUrl) ? "https://respawnhost.com/order" : orderBaseUrl;
    }

    public String getPanelBaseUrl() {
        return panelBaseUrl;
    }

    public void setPanelBaseUrl(String panelBaseUrl) {
        this.panelBaseUrl = isBlank(panelBaseUrl) ? "https://panel.respawnhost.com" : panelBaseUrl;
    }

    public String getGameShort() {
        return gameShort;
    }

    public void setGameShort(String gameShort) {
        this.gameShort = isBlank(gameShort) ? "minecraft" : gameShort;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = isBlank(region) ? "eu" : region;
    }

    public boolean isShowOrderButton() {
        return showOrderButton;
    }

    public void setShowOrderButton(boolean showOrderButton) {
        this.showOrderButton = showOrderButton;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
