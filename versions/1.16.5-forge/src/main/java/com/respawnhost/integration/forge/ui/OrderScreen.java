package com.respawnhost.integration.forge.ui;

import com.respawnhost.core.LangKeys;
import com.respawnhost.core.api.FallbackPlans;
import com.respawnhost.core.api.RespawnApiClient;
import com.respawnhost.core.model.FixedTerm;
import com.respawnhost.core.model.ModpackInfo;
import com.respawnhost.core.model.ServerPlan;
import com.respawnhost.core.recommend.PlanRecommender;
import com.respawnhost.integration.forge.config.RespawnConfig;
import com.respawnhost.integration.forge.modpack.ModpackDetector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fml.ModList;

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
    private final RespawnApiClient client;
    private final String modpackSlug;
    private volatile List<ServerPlan> plans;
    private volatile ModpackInfo modpackInfo;
    private volatile boolean offline;
    private boolean hourlySelected;
    private int termDaysSelected = 30;
    private String regionSelected;
    private ServerPlan recommended;
    private int listTop;
    private Button termButton;
    private final List<Button> orderButtons = new ArrayList<>();

    public OrderScreen(Screen parent) {
        super(new TranslationTextComponent(LangKeys.ORDER_TITLE));
        this.parent = parent;
        RespawnConfig config = RespawnConfig.get();
        this.client = new RespawnApiClient(config.getApiBaseUrl(), config.getGameShort(), config.getPanelBaseUrl());
        this.modpackSlug = ModpackDetector.detectModpackName();
        String configRegion = config.getRegion();
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
                if (Minecraft.getInstance().screen == this && this.minecraft != null) {
                    this.init(this.minecraft, this.width, this.height);
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
        return Minecraft.getInstance().getLanguageManager().getSelected().getCode().startsWith("de") ? "de" : "en";
    }

    private static int loadedModCount() {
        try {
            ModList modList = ModList.get();
            return modList != null ? modList.size() : 0;
        } catch (RuntimeException e) {
            return 0;
        }
    }

    private TranslationTextComponent billingLabel() {
        return new TranslationTextComponent(hourlySelected ? LangKeys.ORDER_MODEL_HOURLY : LangKeys.ORDER_MODEL_FIXED);
    }

    private TranslationTextComponent termLabel() {
        return new TranslationTextComponent(LangKeys.ORDER_TERM_DAYS, termDaysSelected);
    }

    private TranslationTextComponent regionLabel() {
        return new TranslationTextComponent(LangKeys.ORDER_REGION, regionSelected.toUpperCase(Locale.ROOT));
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

        Button billingButton = this.addButton(new Button(controlX, controlY, controlWidth, 20, billingLabel(), button -> {
            this.hourlySelected = !this.hourlySelected;
            button.setMessage(billingLabel());
            this.termButton.active = !this.hourlySelected;
            updateOrderButtonStates();
        }));

        this.termButton = this.addButton(new Button(controlX + controlWidth + controlGap, controlY, controlWidth, 20,
                termLabel(), button -> {
            int index = TERM_OPTIONS.indexOf(this.termDaysSelected);
            this.termDaysSelected = TERM_OPTIONS.get((index + 1) % TERM_OPTIONS.size());
            button.setMessage(termLabel());
        }));
        this.termButton.active = !this.hourlySelected;

        this.addButton(new Button(controlX + (controlWidth + controlGap) * 2, controlY, controlWidth, 20,
                regionLabel(), button -> {
            int index = REGION_OPTIONS.indexOf(this.regionSelected);
            this.regionSelected = REGION_OPTIONS.get((index + 1) % REGION_OPTIONS.size());
            button.setMessage(regionLabel());
        }));

        this.addButton(new Button(this.width / 2 - 102, this.height - 28, 99, 20,
                new TranslationTextComponent(LangKeys.ORDER_BACK), button -> onClose()));
        this.addButton(new Button(this.width / 2 + 3, this.height - 28, 99, 20,
                new TranslationTextComponent(LangKeys.CONFIG_TITLE),
                button -> Minecraft.getInstance().setScreen(new ConfigScreen(this))));

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
            Button orderButton = this.addButton(new Button(buttonX, y, ORDER_BUTTON_WIDTH, 20,
                    new TranslationTextComponent(LangKeys.ORDER_ORDER_NOW),
                    button -> {
                        client.trackCreatorCode(RespawnConfig.get().creatorCode());
                        BrowserUtil.open(client.buildOrderUrl(plan, hourlySelected, termDaysSelected,
                                regionSelected, uiLang()));
                    }));
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
    public void render(com.mojang.blaze3d.matrix.MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        drawCenteredString(matrixStack, this.font, this.title, this.width / 2, 10, 0xFFFFFF);

        if (modpackSlug != null) {
            String displayName = modpackInfo != null && modpackInfo.getRecommendedRamMb() != null
                    ? modpackInfo.getName()
                    : modpackSlug;
            drawCenteredString(matrixStack, this.font,
                    new TranslationTextComponent(LangKeys.ORDER_MODPACK_DETECTED, displayName),
                    this.width / 2, 52, 0x55FF55);
        }

        if (offline) {
            drawCenteredString(matrixStack, this.font,
                    new TranslationTextComponent(LangKeys.ORDER_OFFLINE),
                    this.width / 2, modpackSlug != null ? 64 : 52, 0xFFAA00);
        }

        List<ServerPlan> current = plans;
        if (current == null) {
            drawCenteredString(matrixStack, this.font,
                    new TranslationTextComponent(LangKeys.ORDER_LOADING),
                    this.width / 2, this.height / 2 - 4, 0xAAAAAA);
            return;
        }

        int y = listTop;
        for (ServerPlan plan : current) {
            if (y + ROW_HEIGHT > this.height - 52) {
                break;
            }
            IFormattableTextComponent nameLine = plan == recommended
                    ? new StringTextComponent(plan.getName()).append("  ")
                            .append(new TranslationTextComponent(LangKeys.ORDER_RECOMMENDED))
                    : new StringTextComponent(plan.getName());
            drawString(matrixStack, this.font, nameLine, LIST_LEFT_MARGIN, y, 0xFFFFFF);

            IFormattableTextComponent details = new TranslationTextComponent(LangKeys.ORDER_RAM, plan.ramDisplay());
            if (plan.slotsOrDefault() > 0) {
                details = details.append("   ")
                        .append(new TranslationTextComponent(LangKeys.ORDER_SLOTS, plan.slotsOrDefault()));
            }
            TranslationTextComponent price = hourlySelected
                    ? new TranslationTextComponent(LangKeys.ORDER_PRICE_HOURLY, plan.getPriceHourly(), "EUR")
                    : new TranslationTextComponent(LangKeys.ORDER_PRICE, plan.getPriceMonthly(), "EUR");
            details = details.append("   ").append(price);
            if (!hourlySelected && plan.getFixedTerms() != null) {
                for (FixedTerm term : plan.getFixedTerms()) {
                    if (term.getTermDays() == termDaysSelected && term.getDiscountPercent() > 0) {
                        details = details.append("   ")
                                .append(new TranslationTextComponent(LangKeys.ORDER_EFFECTIVE_MONTHLY,
                                        term.getEffectiveMonthly(), "EUR"));
                        break;
                    }
                }
            }
            drawString(matrixStack, this.font, details, LIST_LEFT_MARGIN, y + 12, 0xAAAAAA);
            y += ROW_HEIGHT;
        }

        drawCenteredString(matrixStack, this.font,
                new TranslationTextComponent(LangKeys.ORDER_CHECKOUT_HINT),
                this.width / 2, this.height - 44, 0x777777);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }
}
