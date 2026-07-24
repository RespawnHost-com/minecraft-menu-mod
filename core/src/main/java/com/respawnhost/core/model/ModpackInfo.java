package com.respawnhost.core.model;

public final class ModpackInfo {
    private String slug;
    private String name;
    private Integer recommendedRamMb;
    private String defaultDownloadUrl;
    private String defaultMcVersion;
    private Integer defaultJavaVersion;

    public ModpackInfo() {
    }

    public ModpackInfo(String slug, String name, Integer recommendedRamMb,
            String defaultDownloadUrl, String defaultMcVersion, Integer defaultJavaVersion) {
        this.slug = slug;
        this.name = name;
        this.recommendedRamMb = recommendedRamMb;
        this.defaultDownloadUrl = defaultDownloadUrl;
        this.defaultMcVersion = defaultMcVersion;
        this.defaultJavaVersion = defaultJavaVersion;
    }

    public String getSlug() {
        return slug;
    }

    public String getName() {
        return name;
    }

    public Integer getRecommendedRamMb() {
        return recommendedRamMb;
    }

    public String getDefaultDownloadUrl() {
        return defaultDownloadUrl;
    }

    public String getDefaultMcVersion() {
        return defaultMcVersion;
    }

    public Integer getDefaultJavaVersion() {
        return defaultJavaVersion;
    }
}
