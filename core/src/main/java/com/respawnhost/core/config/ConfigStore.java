package com.respawnhost.core.config;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

public final class ConfigStore {
    private static final Logger LOGGER = Logger.getLogger(ConfigStore.class.getName());
    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .setPrettyPrinting()
            .create();

    private final Path file;

    public ConfigStore(Path file) {
        this.file = file;
    }

    public RespawnConfigData load() {
        if (Files.exists(file)) {
            Reader reader = null;
            try {
                reader = Files.newBufferedReader(file, StandardCharsets.UTF_8);
                RespawnConfigData data = GSON.fromJson(reader, RespawnConfigData.class);
                if (data != null) {
                    return sanitize(data);
                }
                LOGGER.warning("Config file " + file + " was empty, using defaults");
            } catch (Exception e) {
                LOGGER.warning("Failed to read config file " + file + ", using defaults: " + e);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        }
        RespawnConfigData defaults = new RespawnConfigData();
        save(defaults);
        return defaults;
    }

    public void save(RespawnConfigData data) {
        Writer writer = null;
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8);
            GSON.toJson(data, writer);
        } catch (Exception e) {
            LOGGER.warning("Failed to save config file " + file + ": " + e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static RespawnConfigData sanitize(RespawnConfigData data) {
        data.setPartnerId(data.getPartnerId());
        data.setPackId(data.getPackId());
        data.setApiBaseUrl(data.getApiBaseUrl());
        data.setOrderBaseUrl(data.getOrderBaseUrl());
        data.setPanelBaseUrl(data.getPanelBaseUrl());
        data.setGameShort(data.getGameShort());
        data.setRegion(data.getRegion());
        return data;
    }
}
