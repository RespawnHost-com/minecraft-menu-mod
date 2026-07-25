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
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.Nullable;

import java.awt.Desktop;
import java.net.URI;
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
    private final RespawnApiClient client;
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
        super(Component.translatable(LangKeys.ORDER_TITLE));
        this.parent = parent;
        RespawnConfig config = RespawnConfig.get();
        this.client = new RespawnApiClient(config.apiBaseUrl(), config.gameShort(), config.panelBaseUrl());
        this.modpackSlug = ModpackDetector.detectModpackName();
        String configRegion = config.region();
        this.regionSelected = configRegion != null && REGION_OPTIONS.contains(configRegion.toLowerCase(Locale.ROOT))
                ? configRegion.toLowerCase(Locale.ROOT)
                : "eu";

        CompletableFuture<List<ServerPlan>> plansFuture = client.fetchPlans();
        CompletableFuture<ModpackInfo> infoFuture = modpackSlug != null ? client.fetchModpackInfo(modpackSlug) : null;
        CompletableFuture<Void> done = infoFuture != null
                ? CompletableFuture.allOf(plansFuture, infoFuture)
                : plansFuture.thenAccept(ignored -> {
                });
        done.thenRun(() -> {
            this.plans = plansFuture.join();
            this.modpackInfo = infoFuture != null ? infoFuture.join() : null;
            this.offline = isFallbackContent(this.plans);
            Minecraft.getInstance().execute(() -> {
                if (Minecraft.getInstance().screen == this) {
                    rebuildWidgets();
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
        return Minecraft.getInstance().getLanguageManager().getSelected().startsWith("de") ? "de" : "en";
    }

    private static int loadedModCount() {
        try {
            return ModList.get().size();
        } catch (RuntimeException e) {
            return 0;
        }
    }

    private static void openUri(String url) {
        try {
            Util.getPlatform().openUri(url);
        } catch (RuntimeException e) {
            try {
                Desktop.getDesktop().browse(new URI(url));
            } catch (Exception ignored) {
            }
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

        addRenderableWidget(CycleButton.<Boolean>builder(hourly -> Component.translatable(hourly
                        ? LangKeys.ORDER_MODEL_HOURLY
                        : LangKeys.ORDER_MODEL_FIXED))
                .withValues(Boolean.FALSE, Boolean.TRUE)
                .withInitialValue(hourlySelected)
                .displayOnlyValue()
                .create(controlX, controlY, controlWidth, 20, Component.empty(), (button, value) -> {
                    this.hourlySelected = value;
                    rebuildWidgets();
                }));

        CycleButton<Integer> termButton = CycleButton.<Integer>builder(days ->
                        Component.translatable(LangKeys.ORDER_TERM_DAYS, days))
                .withValues(TERM_OPTIONS)
                .withInitialValue(termDaysSelected)
                .displayOnlyValue()
                .create(controlX + controlWidth + controlGap, controlY, controlWidth, 20, Component.empty(),
                        (button, value) -> this.termDaysSelected = value);
        termButton.active = !hourlySelected;
        addRenderableWidget(termButton);

        addRenderableWidget(CycleButton.<String>builder(region ->
                        Component.translatable(LangKeys.ORDER_REGION, region.toUpperCase(Locale.ROOT)))
                .withValues(REGION_OPTIONS)
                .withInitialValue(regionSelected)
                .displayOnlyValue()
                .create(controlX + (controlWidth + controlGap) * 2, controlY, controlWidth, 20, Component.empty(),
                        (button, value) -> this.regionSelected = value));

        addRenderableWidget(Button.builder(Component.translatable(LangKeys.ORDER_BACK), button -> onClose())
                .bounds(this.width / 2 - 150, this.height - 28, 140, 20)
                .build());

        addRenderableWidget(Button.builder(Component.translatable(LangKeys.CONFIG_TITLE),
                        button -> {
                            if (this.minecraft != null) {
                                this.minecraft.setScreen(new ConfigScreen(this));
                            }
                        })
                .bounds(this.width / 2 + 10, this.height - 28, 140, 20)
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
            Button orderButton = Button.builder(Component.translatable(LangKeys.ORDER_ORDER_NOW),
                            button -> openUri(client.buildOrderUrl(plan, hourlySelected, termDaysSelected, regionSelected, uiLang())))
                    .bounds(buttonX, y, ORDER_BUTTON_WIDTH, 20)
                    .build();
            orderButton.active = hourlySelected ? plan.isAvailableHourly() : plan.isAvailableFixed();
            addRenderableWidget(orderButton);
            y += ROW_HEIGHT;
        }
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        super.render(poseStack, mouseX, mouseY, partialTick);
        drawCenteredString(poseStack, this.font, this.title, this.width / 2, 10, 0xFFFFFF);

        if (modpackSlug != null) {
            String displayName = modpackInfo != null && modpackInfo.getRecommendedRamMb() != null
                    ? modpackInfo.getName()
                    : modpackSlug;
            drawCenteredString(poseStack, this.font,
                    Component.translatable(LangKeys.ORDER_MODPACK_DETECTED, displayName),
                    this.width / 2, 52, 0x55FF55);
        }

        if (offline) {
            drawCenteredString(poseStack, this.font,
                    Component.translatable(LangKeys.ORDER_OFFLINE),
                    this.width / 2, modpackSlug != null ? 64 : 52, 0xFFAA00);
        }

        List<ServerPlan> current = plans;
        if (current == null) {
            drawCenteredString(poseStack, this.font,
                    Component.translatable(LangKeys.ORDER_LOADING),
                    this.width / 2, this.height / 2 - 4, 0xAAAAAA);
            return;
        }

        int y = listTop;
        for (ServerPlan plan : current) {
            if (y + ROW_HEIGHT > this.height - 52) {
                break;
            }
            Component nameLine = plan == recommended
                    ? Component.literal(plan.getName()).append("  ")
                            .append(Component.translatable(LangKeys.ORDER_RECOMMENDED))
                    : Component.literal(plan.getName());
            drawString(poseStack, this.font, nameLine, LIST_LEFT_MARGIN, y, 0xFFFFFF);

            Component details = Component.translatable(LangKeys.ORDER_RAM, plan.ramDisplay());
            if (plan.slotsOrDefault() > 0) {
                details = details.copy()
                        .append("   ")
                        .append(Component.translatable(LangKeys.ORDER_SLOTS, plan.slotsOrDefault()));
            }
            Component price = hourlySelected
                    ? Component.translatable(LangKeys.ORDER_PRICE_HOURLY, plan.getPriceHourly(), "EUR")
                    : Component.translatable(LangKeys.ORDER_PRICE, plan.getPriceMonthly(), "EUR");
            details = details.copy().append("   ").append(price);
            if (!hourlySelected && plan.getFixedTerms() != null) {
                for (FixedTerm term : plan.getFixedTerms()) {
                    if (term.getTermDays() == termDaysSelected && term.getDiscountPercent() > 0) {
                        details = details.copy()
                                .append("   ")
                                .append(Component.translatable(LangKeys.ORDER_EFFECTIVE_MONTHLY, term.getEffectiveMonthly(), "EUR"));
                        break;
                    }
                }
            }
            drawString(poseStack, this.font, details, LIST_LEFT_MARGIN, y + 12, 0xAAAAAA);
            y += ROW_HEIGHT;
        }

        drawCenteredString(poseStack, this.font,
                Component.translatable(LangKeys.ORDER_CHECKOUT_HINT),
                this.width / 2, this.height - 44, 0x777777);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }
}
