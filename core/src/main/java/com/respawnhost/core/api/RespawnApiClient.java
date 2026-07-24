package com.respawnhost.core.api;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.respawnhost.core.model.ModpackInfo;
import com.respawnhost.core.model.ServerPlan;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public final class RespawnApiClient {
    private static final Logger LOGGER = Logger.getLogger(RespawnApiClient.class.getName());
    private static final Gson GSON = new Gson();
    private static final Type PLAN_LIST_TYPE = new TypeToken<List<ServerPlan>>() {
    }.getType();
    private static final int TIMEOUT_MS = 10000;
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "respawnhost-core-http");
        thread.setDaemon(true);
        return thread;
    });

    private final String apiBaseUrl;
    private final String gameShort;
    private final String panelBaseUrl;

    public RespawnApiClient(String apiBaseUrl, String gameShort, String panelBaseUrl) {
        this.apiBaseUrl = stripTrailingSlash(apiBaseUrl);
        this.gameShort = gameShort;
        this.panelBaseUrl = stripTrailingSlash(panelBaseUrl);
    }

    public CompletableFuture<List<ServerPlan>> fetchPlans() {
        final String url;
        try {
            url = apiBaseUrl + "/games/short/" + encode(gameShort) + "/packages";
            new URL(url);
        } catch (RuntimeException | java.net.MalformedURLException e) {
            LOGGER.warning("Invalid plans URL, using fallback plans: " + e);
            return CompletableFuture.completedFuture(FallbackPlans.get());
        }
        return CompletableFuture.supplyAsync(() -> get(url), EXECUTOR)
                .thenApply(body -> {
                    if (body == null) {
                        return FallbackPlans.get();
                    }
                    try {
                        List<ServerPlan> plans = GSON.fromJson(body, PLAN_LIST_TYPE);
                        if (plans == null || plans.isEmpty()) {
                            LOGGER.warning("Plans response was empty, using fallback plans");
                            return FallbackPlans.get();
                        }
                        return Collections.unmodifiableList(new ArrayList<>(plans));
                    } catch (RuntimeException e) {
                        LOGGER.warning("Failed to parse plans response, using fallback plans: " + e);
                        return FallbackPlans.get();
                    }
                })
                .exceptionally(throwable -> {
                    LOGGER.warning("Failed to fetch plans, using fallback plans: " + throwable);
                    return FallbackPlans.get();
                });
    }

    public CompletableFuture<ModpackInfo> fetchModpackInfo(String slug) {
        final ModpackInfo fallback = new ModpackInfo(slug, slug, null, null, null, null);
        final String url;
        try {
            url = apiBaseUrl + "/modpacks/" + encode(slug);
            new URL(url);
        } catch (RuntimeException | java.net.MalformedURLException e) {
            LOGGER.warning("Invalid modpack info URL: " + e);
            return CompletableFuture.completedFuture(fallback);
        }
        return CompletableFuture.supplyAsync(() -> get(url), EXECUTOR)
                .thenApply(body -> {
                    if (body == null) {
                        return fallback;
                    }
                    try {
                        ModpackInfo info = GSON.fromJson(body, ModpackInfo.class);
                        return info != null ? info : fallback;
                    } catch (RuntimeException e) {
                        LOGGER.warning("Failed to parse modpack info for '" + slug + "': " + e);
                        return fallback;
                    }
                })
                .exceptionally(throwable -> {
                    LOGGER.warning("Failed to fetch modpack info for '" + slug + "': " + throwable);
                    return fallback;
                });
    }

    public String buildOrderUrl(ServerPlan plan, boolean hourly, int termDays, String region, String lang) {
        StringBuilder url = new StringBuilder(panelBaseUrl)
                .append('/').append(encode(lang))
                .append("/order/").append(encode(gameShort))
                .append("?plan=").append(plan.getId());
        if (hourly) {
            url.append("&model=hourly");
        } else {
            url.append("&model=fixed&term=").append(termDays);
        }
        if (region != null && !region.trim().isEmpty()) {
            url.append("&region=").append(encode(region));
        }
        return url.toString();
    }

    private static String get(String url) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            int status = connection.getResponseCode();
            if (status != 200) {
                LOGGER.warning("Request to " + url + " returned HTTP " + status);
                return null;
            }
            InputStream in = connection.getInputStream();
            try {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] chunk = new byte[8192];
                int read;
                while ((read = in.read(chunk)) != -1) {
                    buffer.write(chunk, 0, read);
                }
                return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
            } finally {
                in.close();
            }
        } catch (IOException e) {
            LOGGER.warning("Request to " + url + " failed: " + e);
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String stripTrailingSlash(String url) {
        String result = url;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
