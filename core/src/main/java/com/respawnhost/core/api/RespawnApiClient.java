package com.respawnhost.core.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.respawnhost.core.model.CurrencyInfo;
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
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.logging.Logger;

public final class RespawnApiClient {
    private static final Logger LOGGER = Logger.getLogger(RespawnApiClient.class.getName());
    private static final Gson GSON = new Gson();
    private static final Type PLAN_LIST_TYPE = new TypeToken<List<ServerPlan>>() {
    }.getType();
    private static final Type CURRENCY_LIST_TYPE = new TypeToken<List<CurrencyInfo>>() {
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
        return getJson("/games/short/" + encode(gameShort) + "/packages", FallbackPlans.get(), body -> {
            List<ServerPlan> plans = GSON.fromJson(body, PLAN_LIST_TYPE);
            return plans == null || plans.isEmpty()
                    ? FallbackPlans.get()
                    : Collections.unmodifiableList(new ArrayList<>(plans));
        });
    }

    public CompletableFuture<ModpackInfo> fetchModpackInfo(String slug) {
        final ModpackInfo fallback = new ModpackInfo(slug, slug, null, null, null, null);
        return getJson("/modpacks/" + encode(slug), fallback, body -> {
            ModpackInfo info = GSON.fromJson(body, ModpackInfo.class);
            return info != null ? info : fallback;
        });
    }

    /**
     * Display currency: the configured ISO code, otherwise the visitor's currency
     * from GeoIP, otherwise EUR.
     */
    public CompletableFuture<CurrencyInfo> fetchCurrency(String preferredCode) {
        CompletableFuture<String> code = preferredCode != null && !preferredCode.trim().isEmpty()
                ? CompletableFuture.completedFuture(preferredCode.trim().toUpperCase(Locale.ROOT))
                : getJson("/geo/currency", "EUR", body -> GSON.fromJson(body, JsonObject.class)
                        .getAsJsonObject("currency").get("code").getAsString());
        CompletableFuture<List<CurrencyInfo>> all = getJson("/currencies",
                Collections.<CurrencyInfo>emptyList(), body -> GSON.fromJson(body, CURRENCY_LIST_TYPE));
        return code.thenCombine(all, (wanted, currencies) -> {
            for (CurrencyInfo currency : currencies) {
                if (currency.getCode().equalsIgnoreCase(wanted)) {
                    return currency;
                }
            }
            return CurrencyInfo.EUR;
        });
    }

    /** Whether an Eco node is free in the region. Unknown counts as unavailable, like the panel. */
    public CompletableFuture<Boolean> fetchEcoAvailable(String region) {
        return getJson("/capacity/" + encode(region) + "/tiers", Boolean.FALSE, body -> {
            JsonObject json = GSON.fromJson(body, JsonObject.class);
            return json.has("eco") && json.get("eco").getAsBoolean();
        });
    }

    /** Prepaid/subscription discount of a creator code as a fraction (0.2 = 20 %), 0 if unknown. */
    public CompletableFuture<Double> fetchCreatorDiscount(String code) {
        if (code == null || code.trim().isEmpty()) {
            return CompletableFuture.completedFuture(0.0);
        }
        return getJson("/affiliate/validate/" + encode(code.trim()), 0.0, body -> {
            JsonObject affiliate = GSON.fromJson(body, JsonObject.class).getAsJsonObject("affiliate");
            return affiliate != null && affiliate.has("fixed_discount_percent")
                    ? affiliate.get("fixed_discount_percent").getAsDouble()
                    : 0.0;
        });
    }

    public void trackCreatorCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return;
        }
        final String url = apiBaseUrl + "/affiliate/track/" + encode(code.trim());
        CompletableFuture.runAsync(() -> post(url,
                "{\"utm_source\":\"minecraft-mod\",\"utm_medium\":\"ingame\",\"utm_campaign\":\"respawnhost_integration\"}"), EXECUTOR);
    }

    /**
     * Deep link into the panel checkout. The panel reads plan, model, term, region,
     * performance_tier and ref (creator code) from the query.
     */
    public String buildOrderUrl(ServerPlan plan, String model, int termDays, String region, boolean eco,
            String creatorCode, String lang) {
        StringBuilder url = new StringBuilder(panelBaseUrl)
                .append('/').append(encode(lang))
                .append("/order/").append(encode(gameShort))
                .append("?plan=").append(plan.getId())
                .append("&model=").append(encode(model));
        if ("fixed".equals(model)) {
            url.append("&term=").append(termDays);
        }
        if (region != null && !region.trim().isEmpty()) {
            url.append("&region=").append(encode(region));
        }
        url.append("&performance_tier=").append(eco ? "eco" : "performance");
        if (creatorCode != null && !creatorCode.trim().isEmpty()) {
            url.append("&ref=").append(encode(creatorCode.trim()));
        }
        return url.toString();
    }

    private <T> CompletableFuture<T> getJson(String path, final T fallback, final Function<String, T> parse) {
        final String url = apiBaseUrl + path;
        return CompletableFuture.supplyAsync(() -> get(url), EXECUTOR)
                .thenApply(body -> {
                    if (body == null) {
                        return fallback;
                    }
                    try {
                        return parse.apply(body);
                    } catch (RuntimeException e) {
                        LOGGER.warning("Failed to parse response of " + url + ": " + e);
                        return fallback;
                    }
                })
                .exceptionally(throwable -> {
                    LOGGER.warning("Request to " + url + " failed: " + throwable);
                    return fallback;
                });
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

    private static void post(String url, String jsonBody) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            connection.getOutputStream().write(jsonBody.getBytes(StandardCharsets.UTF_8));
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                LOGGER.warning("POST to " + url + " returned HTTP " + status);
            }
        } catch (IOException e) {
            LOGGER.warning("POST to " + url + " failed: " + e);
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
