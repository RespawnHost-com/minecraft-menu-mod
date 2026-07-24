package com.respawnhost.integration.fabric.ui;

import com.respawnhost.core.LangKeys;
import com.respawnhost.core.api.FallbackPlans;
import com.respawnhost.core.api.RespawnApiClient;
import com.respawnhost.core.model.FixedTerm;
import com.respawnhost.core.model.ModpackInfo;
import com.respawnhost.core.model.ServerPlan;
import com.respawnhost.core.recommend.PlanRecommender;
import com.respawnhost.integration.fabric.config.RespawnConfig;
import com.respawnhost.integration.fabric.modpack.ModpackDetector;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.TranslatableText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class OrderScreen extends Screen {
    private static final int ROW_HEIGHT = 30;
    private static final int LIST_LEFT_MARGIN = 40;
    private static final int ORDER_BUTTON_WIDTH = 110;
    private static final List<Integer> TERM_OPTIONS = Arrays.asList(30, 90, 180, 360);
    private static final List<String> REGION_OPTIONS = Arrays.asList("eu", "us");

    private final Screen parent;
    private final RespawnApiClient apiClient;
    private final String modpackSlug;
    private volatile List<ServerPlan> plans;
    private volatile ModpackInfo modpackInfo;
    private volatile boolean offline;
    private boolean hourlySelected;
    private int termDaysSelected = 30;
    private String regionSelected;
    private ServerPlan recommended;
    private int listTop;
    private ButtonWidget termButton;
    private final List<ButtonWidget> orderButtons = new ArrayList<>();

    public OrderScreen(Screen parent) {
        super(new TranslatableText(LangKeys.ORDER_TITLE));
        this.parent = parent;
        RespawnConfig config = RespawnConfig.get();
        this.apiClient = new RespawnApiClient(config.getApiBaseUrl(), config.getGameShort(), config.getPanelBaseUrl());
        this.modpackSlug = ModpackDetector.detectModpackName();
        String configRegion = config.getRegion();
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
                if (MinecraftClient.getInstance().currentScreen == this && this.client != null) {
                    this.init(this.client, this.width, this.height);
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
        return MinecraftClient.getInstance().getLanguageManager().getLanguage().getCode().startsWith("de") ? "de" : "en";
    }

    private static int loadedModCount() {
        try {
            return FabricLoader.getInstance().getAllMods().size();
        } catch (RuntimeException e) {
            return 0;
        }
    }

    private TranslatableText billingLabel() {
        return new TranslatableText(hourlySelected ? LangKeys.ORDER_MODEL_HOURLY : LangKeys.ORDER_MODEL_FIXED);
    }

    private TranslatableText termLabel() {
        return new TranslatableText(LangKeys.ORDER_TERM_DAYS, termDaysSelected);
    }

    private TranslatableText regionLabel() {
        return new TranslatableText(LangKeys.ORDER_REGION, regionSelected.toUpperCase(Locale.ROOT));
    }

    @Override
    protected void init() {
        boolean showModpackLine = modpackSlug != null;
        listTop = showModpackLine ? 78 : 66;
        orderButtons.clear();

        int controlY = 26;
        int controlWidth = 100;
        int controlGap = 8;
        int controlsTotal = controlWidth * 3 + controlGap * 2;
        int controlX = this.width / 2 - controlsTotal / 2;

        this.addButton(new ButtonWidget(controlX, controlY, controlWidth, 20, billingLabel(), button -> {
            this.hourlySelected = !this.hourlySelected;
            button.setMessage(billingLabel());
            this.termButton.active = !this.hourlySelected;
            updateOrderButtonStates();
        }));

        this.termButton = this.addButton(new ButtonWidget(controlX + controlWidth + controlGap, controlY,
                controlWidth, 20, termLabel(), button -> {
            int index = TERM_OPTIONS.indexOf(this.termDaysSelected);
            this.termDaysSelected = TERM_OPTIONS.get((index + 1) % TERM_OPTIONS.size());
            button.setMessage(termLabel());
        }));
        this.termButton.active = !this.hourlySelected;

        this.addButton(new ButtonWidget(controlX + (controlWidth + controlGap) * 2, controlY, controlWidth, 20,
                regionLabel(), button -> {
            int index = REGION_OPTIONS.indexOf(this.regionSelected);
            this.regionSelected = REGION_OPTIONS.get((index + 1) % REGION_OPTIONS.size());
            button.setMessage(regionLabel());
        }));

        this.addButton(new ButtonWidget(this.width / 2 - 102, this.height - 28, 99, 20,
                new TranslatableText(LangKeys.ORDER_BACK), button -> onClose()));
        this.addButton(new ButtonWidget(this.width / 2 + 3, this.height - 28, 99, 20,
                new TranslatableText(LangKeys.CONFIG_TITLE),
                button -> MinecraftClient.getInstance().openScreen(new ConfigScreen(this))));

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
            ButtonWidget orderButton = this.addButton(new ButtonWidget(buttonX, y, ORDER_BUTTON_WIDTH, 20,
                    new TranslatableText(LangKeys.ORDER_ORDER_NOW),
                    button -> BrowserUtil.open(apiClient.buildOrderUrl(plan, hourlySelected, termDaysSelected,
                            regionSelected, uiLang()))));
            orderButton.active = hourlySelected ? plan.isAvailableHourly() : plan.isAvailableFixed();
            orderButtons.add(orderButton);
            y += ROW_HEIGHT;
        }
    }

    private void updateOrderButtonStates() {
        List<ServerPlan> current = plans;
        if (current == null) {
            return;
        }
        for (int i = 0; i < orderButtons.size() && i < current.size(); i++) {
            ServerPlan plan = current.get(i);
            orderButtons.get(i).active = hourlySelected ? plan.isAvailableHourly() : plan.isAvailableFixed();
        }
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);
        DrawableHelper.drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 10, 0xFFFFFF);

        if (modpackSlug != null) {
            String displayName = modpackInfo != null && modpackInfo.getRecommendedRamMb() != null
                    ? modpackInfo.getName()
                    : modpackSlug;
            DrawableHelper.drawCenteredText(matrices, this.textRenderer,
                    new TranslatableText(LangKeys.ORDER_MODPACK_DETECTED, displayName),
                    this.width / 2, 52, 0x55FF55);
        }

        if (offline) {
            DrawableHelper.drawCenteredText(matrices, this.textRenderer,
                    new TranslatableText(LangKeys.ORDER_OFFLINE),
                    this.width / 2, modpackSlug != null ? 64 : 52, 0xFFAA00);
        }

        List<ServerPlan> current = plans;
        if (current == null) {
            DrawableHelper.drawCenteredText(matrices, this.textRenderer,
                    new TranslatableText(LangKeys.ORDER_LOADING),
                    this.width / 2, this.height / 2 - 4, 0xAAAAAA);
            return;
        }

        int y = listTop;
        for (ServerPlan plan : current) {
            if (y + ROW_HEIGHT > this.height - 52) {
                break;
            }
            MutableText nameLine = plan == recommended
                    ? new LiteralText(plan.getName()).append("  ")
                            .append(new TranslatableText(LangKeys.ORDER_RECOMMENDED))
                    : new LiteralText(plan.getName());
            DrawableHelper.drawTextWithShadow(matrices, this.textRenderer, nameLine, LIST_LEFT_MARGIN, y, 0xFFFFFF);

            MutableText details = new TranslatableText(LangKeys.ORDER_RAM, plan.ramDisplay());
            if (plan.slotsOrDefault() > 0) {
                details = details.append("   ")
                        .append(new TranslatableText(LangKeys.ORDER_SLOTS, plan.slotsOrDefault()));
            }
            TranslatableText price = hourlySelected
                    ? new TranslatableText(LangKeys.ORDER_PRICE_HOURLY, plan.getPriceHourly(), "EUR")
                    : new TranslatableText(LangKeys.ORDER_PRICE, plan.getPriceMonthly(), "EUR");
            details = details.append("   ").append(price);
            if (!hourlySelected && plan.getFixedTerms() != null) {
                for (FixedTerm term : plan.getFixedTerms()) {
                    if (term.getTermDays() == termDaysSelected && term.getDiscountPercent() > 0) {
                        details = details.append("   ")
                                .append(new TranslatableText(LangKeys.ORDER_EFFECTIVE_MONTHLY,
                                        term.getEffectiveMonthly(), "EUR"));
                        break;
                    }
                }
            }
            DrawableHelper.drawTextWithShadow(matrices, this.textRenderer, details, LIST_LEFT_MARGIN, y + 12, 0xAAAAAA);
            y += ROW_HEIGHT;
        }

        DrawableHelper.drawCenteredText(matrices, this.textRenderer,
                new TranslatableText(LangKeys.ORDER_CHECKOUT_HINT),
                this.width / 2, this.height - 44, 0x777777);
    }

    @Override
    public void onClose() {
        if (this.client != null) {
            this.client.openScreen(parent);
        }
    }
}
