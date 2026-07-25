package com.respawnhost.integration.ui;

import com.respawnhost.core.LangKeys;
import com.respawnhost.core.api.FallbackPlans;
import com.respawnhost.core.api.RespawnApiClient;
import com.respawnhost.core.model.FixedTerm;
import com.respawnhost.core.model.ModpackInfo;
import com.respawnhost.core.model.ServerPlan;
import com.respawnhost.core.recommend.PlanRecommender;
import com.respawnhost.integration.config.RespawnConfig;
import com.respawnhost.integration.modpack.ModpackDetector;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class OrderScreen extends Screen {
    private static final int ROW_HEIGHT = 30;
    private static final int LIST_LEFT_MARGIN = 40;
    private static final int ORDER_BUTTON_WIDTH = 110;
    private static final List<Integer> TERM_OPTIONS = List.of(30, 90, 180, 360);
    private static final List<String> REGION_OPTIONS = List.of("eu", "us");

    private final Screen parent;
    private final RespawnApiClient apiClient;
    private final @Nullable String modpackSlug;
    private volatile @Nullable List<ServerPlan> plans;
    private volatile @Nullable ModpackInfo modpackInfo;
    private volatile boolean offline;
    private boolean hourlySelected;
    private int termDaysSelected = 30;
    private String regionSelected;
    private @Nullable ServerPlan recommended;
    private int listTop;

    public OrderScreen(Screen parent) {
        super(Text.translatable(LangKeys.ORDER_TITLE));
        this.parent = parent;
        RespawnConfig config = RespawnConfig.get();
        this.apiClient = new RespawnApiClient(config.apiBaseUrl(), config.gameShort(), config.panelBaseUrl());
        this.modpackSlug = ModpackDetector.detectModpackName();
        String configRegion = config.region();
        this.regionSelected = configRegion != null && REGION_OPTIONS.contains(configRegion.toLowerCase(Locale.ROOT))
                ? configRegion.toLowerCase(Locale.ROOT)
                : "eu";

        CompletableFuture<List<ServerPlan>> plansFuture = apiClient.fetchPlans();
        CompletableFuture<ModpackInfo> infoFuture = modpackSlug != null ? apiClient.fetchModpackInfo(modpackSlug) : null;
        CompletableFuture<Void> done = infoFuture != null
                ? CompletableFuture.allOf(plansFuture, infoFuture)
                : plansFuture.thenAccept(ignored -> {
                });
        done.thenRun(() -> {
            this.plans = plansFuture.join();
            this.modpackInfo = infoFuture != null ? infoFuture.join() : null;
            this.offline = isFallbackContent(this.plans);
            MinecraftClient.getInstance().execute(() -> {
                if (MinecraftClient.getInstance().currentScreen == this) {
                    clearAndInit();
                }
            });
        });
    }

    private static boolean isFallbackContent(List<ServerPlan> list) {
        List<ServerPlan> fallback = FallbackPlans.get();
        if (list.size() != fallback.size()) {
            return false;
        }
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId() != fallback.get(i).getId()) {
                return false;
            }
        }
        return true;
    }

    private static String uiLang() {
        return MinecraftClient.getInstance().getLanguageManager().getLanguage().startsWith("de") ? "de" : "en";
    }

    private static int loadedModCount() {
        try {
            return FabricLoader.getInstance().getAllMods().size();
        } catch (RuntimeException e) {
            return 0;
        }
    }

    @Override
    protected void init() {
        boolean showModpackLine = modpackSlug != null;
        listTop = showModpackLine ? 78 : 66;

        int controlY = 26;
        int controlWidth = 100;
        int controlGap = 8;
        int controlsTotal = controlWidth * 3 + controlGap * 2;
        int controlX = this.width / 2 - controlsTotal / 2;

        addDrawableChild(CyclingButtonWidget.<Boolean>builder(hourly -> Text.translatable(hourly
                        ? LangKeys.ORDER_MODEL_HOURLY
                        : LangKeys.ORDER_MODEL_FIXED))
                .values(Boolean.FALSE, Boolean.TRUE)
                .initially(hourlySelected)
                .omitKeyText()
                .build(controlX, controlY, controlWidth, 20, Text.empty(), (button, value) -> {
                    this.hourlySelected = value;
                    clearAndInit();
                }));

        CyclingButtonWidget<Integer> termButton = CyclingButtonWidget.<Integer>builder(days ->
                        Text.translatable(LangKeys.ORDER_TERM_DAYS, days))
                .values(TERM_OPTIONS)
                .initially(termDaysSelected)
                .omitKeyText()
                .build(controlX + controlWidth + controlGap, controlY, controlWidth, 20, Text.empty(),
                        (button, value) -> this.termDaysSelected = value);
        termButton.active = !hourlySelected;
        addDrawableChild(termButton);

        addDrawableChild(CyclingButtonWidget.<String>builder(region ->
                        Text.translatable(LangKeys.ORDER_REGION, region.toUpperCase(Locale.ROOT)))
                .values(REGION_OPTIONS)
                .initially(regionSelected)
                .omitKeyText()
                .build(controlX + (controlWidth + controlGap) * 2, controlY, controlWidth, 20, Text.empty(),
                        (button, value) -> this.regionSelected = value));

        addDrawableChild(ButtonWidget.builder(Text.translatable(LangKeys.CONFIG_TITLE),
                        button -> MinecraftClient.getInstance().setScreen(new ConfigScreen(this)))
                .dimensions(this.width - 116, 6, 110, 20)
                .build());

        addDrawableChild(ButtonWidget.builder(Text.translatable(LangKeys.ORDER_BACK), button -> close())
                .dimensions(this.width / 2 - 100, this.height - 28, 200, 20)
                .build());

        List<ServerPlan> current = plans;
        if (current == null || current.isEmpty()) {
            return;
        }
        recommended = PlanRecommender.recommend(current,
                modpackInfo != null ? modpackInfo.getRecommendedRamMb() : null, loadedModCount());
        int y = listTop;
        int buttonX = Math.max(this.width - ORDER_BUTTON_WIDTH - LIST_LEFT_MARGIN, this.width / 2 + 40);
        for (ServerPlan plan : current) {
            if (y + ROW_HEIGHT > this.height - 52) {
                break;
            }
            ButtonWidget orderButton = ButtonWidget.builder(Text.translatable(LangKeys.ORDER_ORDER_NOW),
                            button -> {
                                apiClient.trackCreatorCode(RespawnConfig.get().creatorCode());
                                Util.getOperatingSystem().open(apiClient.buildOrderUrl(plan, hourlySelected, termDaysSelected, regionSelected, uiLang()));
                            })
                    .dimensions(buttonX, y, ORDER_BUTTON_WIDTH, 20)
                    .build();
            orderButton.active = hourlySelected ? plan.isAvailableHourly() : plan.isAvailableFixed();
            addDrawableChild(orderButton);
            y += ROW_HEIGHT;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, 0xFFFFFF);

        if (modpackSlug != null) {
            String displayName = modpackInfo != null && modpackInfo.getRecommendedRamMb() != null
                    ? modpackInfo.getName()
                    : modpackSlug;
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.translatable(LangKeys.ORDER_MODPACK_DETECTED, displayName),
                    this.width / 2, 52, 0x55FF55);
        }

        if (offline) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.translatable(LangKeys.ORDER_OFFLINE),
                    this.width / 2, modpackSlug != null ? 64 : 52, 0xFFAA00);
        }

        List<ServerPlan> current = plans;
        if (current == null) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.translatable(LangKeys.ORDER_LOADING),
                    this.width / 2, this.height / 2 - 4, 0xAAAAAA);
            return;
        }

        int y = listTop;
        for (ServerPlan plan : current) {
            if (y + ROW_HEIGHT > this.height - 52) {
                break;
            }
            MutableText nameLine = plan == recommended
                    ? Text.literal(plan.getName()).append("  ")
                            .append(Text.translatable(LangKeys.ORDER_RECOMMENDED))
                    : Text.literal(plan.getName());
            context.drawTextWithShadow(this.textRenderer, nameLine, LIST_LEFT_MARGIN, y, 0xFFFFFF);

            MutableText details = Text.translatable(LangKeys.ORDER_RAM, plan.ramDisplay());
            if (plan.slotsOrDefault() > 0) {
                details = details
                        .append("   ")
                        .append(Text.translatable(LangKeys.ORDER_SLOTS, plan.slotsOrDefault()));
            }
            MutableText price = hourlySelected
                    ? Text.translatable(LangKeys.ORDER_PRICE_HOURLY, plan.getPriceHourly(), "EUR")
                    : Text.translatable(LangKeys.ORDER_PRICE, plan.getPriceMonthly(), "EUR");
            details = details.append("   ").append(price);
            if (!hourlySelected && plan.getFixedTerms() != null) {
                for (FixedTerm term : plan.getFixedTerms()) {
                    if (term.getTermDays() == termDaysSelected && term.getDiscountPercent() > 0) {
                        details = details
                                .append("   ")
                                .append(Text.translatable(LangKeys.ORDER_EFFECTIVE_MONTHLY, term.getEffectiveMonthly(), "EUR"));
                        break;
                    }
                }
            }
            context.drawTextWithShadow(this.textRenderer, details, LIST_LEFT_MARGIN, y + 12, 0xAAAAAA);
            y += ROW_HEIGHT;
        }

        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.translatable(LangKeys.ORDER_CHECKOUT_HINT),
                this.width / 2, this.height - 44, 0x777777);
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }
}
