package com.respawnhost.core.order;

import com.respawnhost.core.LangKeys;
import com.respawnhost.core.api.FallbackPlans;
import com.respawnhost.core.api.RespawnApiClient;
import com.respawnhost.core.model.CurrencyInfo;
import com.respawnhost.core.model.FixedTerm;
import com.respawnhost.core.model.ModpackInfo;
import com.respawnhost.core.model.PriceOverride;
import com.respawnhost.core.model.ServerPlan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Order selection plus everything needed to price it. Shared by every loader/version
 * screen, which only render the lines and buttons this class describes.
 *
 * <p>Prices follow the panel checkout: the cheapest bookable hardware tier (Eco when a
 * node is free in the region), currency overrides before exchange rates, and the
 * largest single discount (term or creator code), never stacked.
 */
public final class OrderSession {
    public static final String SUBSCRIPTION = "subscription";
    public static final String FIXED = "fixed";
    public static final String HOURLY = "hourly";
    public static final List<String> MODELS = Collections.unmodifiableList(Arrays.asList(SUBSCRIPTION, FIXED, HOURLY));
    public static final List<Integer> TERMS = Collections.unmodifiableList(Arrays.asList(30, 90, 180, 360));
    public static final List<String> REGIONS = Collections.unmodifiableList(Arrays.asList("eu", "us", "clt", "in"));

    /** A translatable text: lang key plus its already formatted arguments. */
    public static final class Line {
        public final String key;
        public final Object[] args;

        Line(String key, Object... args) {
            this.key = key;
            this.args = args;
        }
    }

    private final RespawnApiClient client;
    private final String creatorCode;
    private final String preferredCurrency;
    private final String modpackSlug;

    private String model = SUBSCRIPTION;
    private int termDays = 30;
    private String region;

    private volatile List<ServerPlan> plans;
    private volatile ModpackInfo modpackInfo;
    private volatile CurrencyInfo currency = CurrencyInfo.EUR;
    private volatile List<CurrencyInfo> currencies = Collections.singletonList(CurrencyInfo.EUR);
    private volatile Map<String, Boolean> ecoByRegion = Collections.emptyMap();
    private volatile double creatorDiscount;

    public OrderSession(String apiBaseUrl, String panelBaseUrl, String gameShort, String region,
            String currency, String creatorCode, String modpackSlug) {
        this.client = new RespawnApiClient(apiBaseUrl, gameShort, panelBaseUrl);
        String wanted = region == null ? "" : region.trim().toLowerCase(Locale.ROOT);
        this.region = REGIONS.contains(wanted) ? wanted : "eu";
        this.preferredCurrency = currency;
        this.creatorCode = creatorCode == null ? "" : creatorCode.trim();
        this.modpackSlug = modpackSlug;
    }

    /** Loads plans and pricing context; never fails, missing data falls back to defaults. */
    public CompletableFuture<Void> load() {
        CompletableFuture<List<ServerPlan>> plansFuture = client.fetchPlans();
        CompletableFuture<ModpackInfo> infoFuture = modpackSlug != null
                ? client.fetchModpackInfo(modpackSlug)
                : CompletableFuture.<ModpackInfo>completedFuture(null);
        CompletableFuture<String> currencyCodeFuture = client.fetchCurrencyCode(preferredCurrency);
        CompletableFuture<List<CurrencyInfo>> currenciesFuture = client.fetchCurrencies();
        CompletableFuture<Double> discountFuture = client.fetchCreatorDiscount(creatorCode);
        final Map<String, CompletableFuture<Boolean>> ecoFutures = new HashMap<>();
        for (String r : REGIONS) {
            ecoFutures.put(r, client.fetchEcoAvailable(r));
        }
        List<CompletableFuture<?>> all = new ArrayList<>(ecoFutures.values());
        all.add(plansFuture);
        all.add(infoFuture);
        all.add(currencyCodeFuture);
        all.add(currenciesFuture);
        all.add(discountFuture);
        return CompletableFuture.allOf(all.toArray(new CompletableFuture<?>[0])).thenRun(() -> {
            Map<String, Boolean> eco = new HashMap<>();
            for (Map.Entry<String, CompletableFuture<Boolean>> entry : ecoFutures.entrySet()) {
                eco.put(entry.getKey(), entry.getValue().join());
            }
            ecoByRegion = eco;
            currencies = currenciesFuture.join();
            currency(currencyCodeFuture.join());
            creatorDiscount = Math.max(0.0, Math.min(1.0, discountFuture.join()));
            modpackInfo = infoFuture.join();
            plans = plansFuture.join();
        });
    }

    /** Test hook: pricing context without network. */
    void context(List<ServerPlan> plans, CurrencyInfo currency, Map<String, Boolean> ecoByRegion, double creatorDiscount) {
        this.plans = plans;
        this.currency = currency;
        this.ecoByRegion = ecoByRegion;
        this.creatorDiscount = creatorDiscount;
    }

    /** Test hook. */
    void currencies(List<CurrencyInfo> currencies) {
        this.currencies = currencies;
    }

    /** Null while loading. */
    public List<ServerPlan> plans() {
        return plans;
    }

    public ModpackInfo modpackInfo() {
        return modpackInfo;
    }

    public boolean isOffline() {
        return plans == FallbackPlans.get();
    }

    public String model() {
        return model;
    }

    public void model(String model) {
        if (MODELS.contains(model)) {
            this.model = model;
        }
    }

    public int termDays() {
        return termDays;
    }

    public void termDays(int termDays) {
        if (TERMS.contains(termDays)) {
            this.termDays = termDays;
        }
    }

    public String region() {
        return region;
    }

    public void region(String region) {
        if (REGIONS.contains(region)) {
            this.region = region;
        }
    }

    public List<String> currencyCodes() {
        List<String> codes = new ArrayList<>();
        for (CurrencyInfo c : currencies) {
            codes.add(c.getCode());
        }
        return codes;
    }

    public String currencyCode() {
        return currency.getCode();
    }

    /** Display currency only; the checkout charges in the account currency. Unknown codes are ignored. */
    public void currency(String code) {
        for (CurrencyInfo c : currencies) {
            if (c.getCode().equalsIgnoreCase(code)) {
                currency = c;
                return;
            }
        }
    }

    /** Only prepaid has a selectable term; subscriptions renew monthly, pay-per-use has none. */
    public boolean termSelectable() {
        return FIXED.equals(model);
    }

    public boolean orderable(ServerPlan plan) {
        return HOURLY.equals(model) ? plan.isAvailableHourly() : plan.isAvailableFixed();
    }

    public static String modelKey(String model) {
        if (FIXED.equals(model)) {
            return LangKeys.ORDER_MODEL_FIXED;
        }
        return HOURLY.equals(model) ? LangKeys.ORDER_MODEL_HOURLY : LangKeys.ORDER_MODEL_SUBSCRIPTION;
    }

    public static String regionLabel(String region) {
        if ("us".equals(region)) {
            return "US West";
        }
        if ("clt".equals(region)) {
            return "US East";
        }
        return "in".equals(region) ? "India" : "EU";
    }

    /** Price texts for one plan row in the current model, term, region and currency. */
    public List<Line> priceLines(ServerPlan plan, String lang) {
        List<Line> lines = new ArrayList<>();
        boolean eco = usesEco(plan);
        if (HOURLY.equals(model)) {
            double hourlyEur = effectiveEur(plan.getPriceHourly(), plan, "hourly");
            if (eco) {
                hourlyEur *= plan.getPriceHourlyEco() / plan.getPriceHourly();
            }
            lines.add(new Line(LangKeys.ORDER_PRICE_HOURLY,
                    currency.format(hourlyEur * currency.getRateFromEur(), 4, lang)));
            return lines;
        }
        double monthlyEur = monthlyEur(plan, eco);
        if (SUBSCRIPTION.equals(model)) {
            lines.add(new Line(LangKeys.ORDER_PRICE, money(monthlyEur, lang)));
            double firstMonth = round2(monthlyEur - round2(monthlyEur * creatorDiscount));
            if (firstMonth < monthlyEur) {
                lines.add(new Line(LangKeys.ORDER_FIRST_MONTH, money(firstMonth, lang)));
            }
            return lines;
        }
        int months = termDays / 30;
        double gross = round2(monthlyEur * months);
        double discount = Math.max(round2(gross * termDiscountPercent(plan) / 100.0), round2(gross * creatorDiscount));
        double total = round2(gross - discount);
        lines.add(new Line(LangKeys.ORDER_PRICE_TERM, money(total, lang), termDays));
        if (months > 1) {
            lines.add(new Line(LangKeys.ORDER_EFFECTIVE_MONTHLY, money(total / months, lang)));
        }
        return lines;
    }

    public String orderUrl(ServerPlan plan, String lang) {
        return client.buildOrderUrl(plan, model, termDays, region, usesEco(plan), creatorCode, lang);
    }

    /** Reports the click to the affiliate system; call right before opening {@link #orderUrl}. */
    public void trackOrderClick() {
        client.trackCreatorCode(creatorCode);
    }

    private boolean usesEco(ServerPlan plan) {
        Boolean free = ecoByRegion.get(region);
        boolean tierExists = HOURLY.equals(model)
                ? plan.getPriceHourlyEco() != null && plan.getPriceHourly() > 0
                : plan.getPriceMonthlyEco() != null && plan.getPriceMonthly() > 0;
        return free != null && free && tierExists;
    }

    /** Monthly list price in EUR as the checkout uses it: override-adjusted, tier applied, cent-rounded. */
    private double monthlyEur(ServerPlan plan, boolean eco) {
        double monthly = effectiveEur(plan.getPriceMonthly(), plan, "monthly");
        return round2(eco ? monthly * plan.getPriceMonthlyEco() / plan.getPriceMonthly() : monthly);
    }

    /** A maintained fixed price in the display currency wins over the exchange rate. */
    private double effectiveEur(double baseEur, ServerPlan plan, String key) {
        Map<String, PriceOverride> overrides = plan.getPriceOverrides();
        PriceOverride override = overrides != null ? overrides.get(currency.getCode()) : null;
        Double value = override == null ? null : "hourly".equals(key) ? override.getHourly() : override.getMonthly();
        return value != null && value > 0 ? value / currency.getRateFromEur() : baseEur;
    }

    private double termDiscountPercent(ServerPlan plan) {
        if (plan.getFixedTerms() != null) {
            for (FixedTerm term : plan.getFixedTerms()) {
                if (term.getTermDays() == termDays) {
                    return term.getDiscountPercent();
                }
            }
        }
        return 0;
    }

    private String money(double eur, String lang) {
        return currency.format(eur * currency.getRateFromEur(), currency.getDecimalPlaces(), lang);
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
