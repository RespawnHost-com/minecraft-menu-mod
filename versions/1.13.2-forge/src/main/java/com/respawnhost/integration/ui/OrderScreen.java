package com.respawnhost.integration.ui;

import com.respawnhost.core.LangKeys;
import com.respawnhost.core.api.FallbackPlans;
import com.respawnhost.core.api.RespawnApiClient;
import com.respawnhost.core.model.FixedTerm;
import com.respawnhost.core.model.ModpackInfo;
import com.respawnhost.core.model.ServerPlan;
import com.respawnhost.core.recommend.PlanRecommender;
import com.respawnhost.integration.RespawnHostIntegrationMod;
import com.respawnhost.integration.config.RespawnConfig;
import com.respawnhost.integration.modpack.ModpackDetector;
import com.respawnhost.integration.util.BrowserUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class OrderScreen extends GuiScreen {
    private static final int ROW_HEIGHT = 30;
    private static final int LIST_LEFT_MARGIN = 40;
    private static final int ORDER_BUTTON_WIDTH = 110;
    private static final int[] TERM_OPTIONS = {30, 90, 180, 360};
    private static final String[] REGION_OPTIONS = {"eu", "us"};

    private static final int ID_BILLING = 1;
    private static final int ID_TERM = 2;
    private static final int ID_REGION = 3;
    private static final int ID_BACK = 4;
    private static final int ID_CONFIG = 5;
    private static final int ID_ORDER_BASE = 100;

    private final GuiScreen parent;
    private final RespawnApiClient client;
    private final String modpackSlug;
    private volatile List<ServerPlan> plans;
    private volatile ModpackInfo modpackInfo;
    private volatile boolean offline;
    private boolean hourlySelected;
    private int termIndex;
    private String regionSelected;
    private ServerPlan recommended;
    private int listTop;
    private boolean fetchStarted;

    public OrderScreen(GuiScreen parent) {
        this.parent = parent;
        RespawnConfig config = RespawnConfig.get();
        this.client = new RespawnApiClient(config.apiBaseUrl(), config.gameShort(), config.panelBaseUrl());
        this.modpackSlug = ModpackDetector.detectModpackName();
        String configRegion = config.region();
        this.regionSelected = "eu";
        if (configRegion != null) {
            for (String option : REGION_OPTIONS) {
                if (option.equalsIgnoreCase(configRegion.trim())) {
                    this.regionSelected = option;
                }
            }
        }
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
        try {
            return Minecraft.getInstance().getLanguageManager().getCurrentLanguage().getLanguageCode()
                    .startsWith("de") ? "de" : "en";
        } catch (RuntimeException e) {
            return "en";
        }
    }

    private void startFetchIfNeeded() {
        if (fetchStarted) {
            return;
        }
        fetchStarted = true;
        final CompletableFuture<List<ServerPlan>> plansFuture = client.fetchPlans();
        final CompletableFuture<ModpackInfo> infoFuture =
                modpackSlug != null ? client.fetchModpackInfo(modpackSlug) : null;
        CompletableFuture<Void> done = infoFuture != null
                ? CompletableFuture.allOf(plansFuture, infoFuture)
                : plansFuture.thenAccept(ignored -> {
                });
        done.thenRun(() -> {
            this.plans = plansFuture.join();
            this.modpackInfo = infoFuture != null ? infoFuture.join() : null;
            this.offline = isFallbackContent(this.plans);
            Minecraft.getInstance().addScheduledTask(() -> {
                if (Minecraft.getInstance().currentScreen == this) {
                    rebuildContent();
                }
            });
        });
    }

    private void rebuildContent() {
        this.buttons.clear();
        this.children.clear();
        initGui();
    }

    @Override
    protected void initGui() {
        startFetchIfNeeded();

        boolean showModpackLine = modpackSlug != null;
        listTop = showModpackLine ? 78 : 66;

        int controlY = 26;
        int controlWidth = 100;
        int controlGap = 8;
        int controlsTotal = controlWidth * 3 + controlGap * 2;
        int controlX = this.width / 2 - controlsTotal / 2;

        final RunnableButton billingButton = new RunnableButton(ID_BILLING, controlX, controlY, controlWidth, 20,
                I18n.format(hourlySelected ? LangKeys.ORDER_MODEL_HOURLY : LangKeys.ORDER_MODEL_FIXED), () -> {
        });
        billingButton.setAction(() -> {
            hourlySelected = !hourlySelected;
            rebuildContent();
        });
        addButton(billingButton);

        final RunnableButton termButton = new RunnableButton(ID_TERM, controlX + controlWidth + controlGap, controlY,
                controlWidth, 20, I18n.format(LangKeys.ORDER_TERM_DAYS, TERM_OPTIONS[termIndex]), () -> {
        });
        termButton.setAction(() -> {
            termIndex = (termIndex + 1) % TERM_OPTIONS.length;
            termButton.setMessage(I18n.format(LangKeys.ORDER_TERM_DAYS, TERM_OPTIONS[termIndex]));
        });
        termButton.enabled = !hourlySelected;
        addButton(termButton);

        final RunnableButton regionButton = new RunnableButton(ID_REGION,
                controlX + (controlWidth + controlGap) * 2, controlY, controlWidth, 20,
                I18n.format(LangKeys.ORDER_REGION, regionSelected.toUpperCase(Locale.ROOT)), () -> {
        });
        regionButton.setAction(() -> {
            regionSelected = REGION_OPTIONS[0].equals(regionSelected) ? REGION_OPTIONS[1] : REGION_OPTIONS[0];
            regionButton.setMessage(I18n.format(LangKeys.ORDER_REGION, regionSelected.toUpperCase(Locale.ROOT)));
        });
        addButton(regionButton);

        addButton(new RunnableButton(ID_BACK, this.width / 2 - 100, this.height - 28, 200, 20,
                I18n.format(LangKeys.ORDER_BACK),
                () -> this.mc.displayGuiScreen(parent)));

        addButton(new RunnableButton(ID_CONFIG, 4, this.height - 28, ORDER_BUTTON_WIDTH, 20,
                I18n.format(LangKeys.CONFIG_TITLE),
                () -> this.mc.displayGuiScreen(new ConfigScreen(this))));

        List<ServerPlan> current = plans;
        if (current == null || current.isEmpty()) {
            return;
        }
        recommended = PlanRecommender.recommend(current,
                modpackInfo != null ? modpackInfo.getRecommendedRamMb() : null,
                RespawnHostIntegrationMod.loadedModCount());
        int y = listTop;
        int buttonX = Math.max(this.width - ORDER_BUTTON_WIDTH - LIST_LEFT_MARGIN, this.width / 2 + 40);
        for (int i = 0; i < current.size(); i++) {
            if (y + ROW_HEIGHT > this.height - 52) {
                break;
            }
            final ServerPlan plan = current.get(i);
            RunnableButton orderButton = new RunnableButton(ID_ORDER_BASE + i, buttonX, y, ORDER_BUTTON_WIDTH, 20,
                    I18n.format(LangKeys.ORDER_ORDER_NOW),
                    () -> {
                        client.trackCreatorCode(RespawnConfig.get().creatorCode());
                        BrowserUtil.open(client.buildOrderUrl(plan, hourlySelected,
                                TERM_OPTIONS[termIndex], regionSelected, uiLang()));
                    });
            orderButton.enabled = hourlySelected ? plan.isAvailableHourly() : plan.isAvailableFixed();
            addButton(orderButton);
            y += ROW_HEIGHT;
        }
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.render(mouseX, mouseY, partialTicks);
        drawCenteredString(this.fontRenderer, I18n.format(LangKeys.ORDER_TITLE), this.width / 2, 10, 0xFFFFFF);

        if (modpackSlug != null) {
            String displayName = modpackInfo != null && modpackInfo.getRecommendedRamMb() != null
                    ? modpackInfo.getName()
                    : modpackSlug;
            drawCenteredString(this.fontRenderer,
                    I18n.format(LangKeys.ORDER_MODPACK_DETECTED, displayName),
                    this.width / 2, 52, 0x55FF55);
        }

        if (offline) {
            drawCenteredString(this.fontRenderer, I18n.format(LangKeys.ORDER_OFFLINE),
                    this.width / 2, modpackSlug != null ? 64 : 52, 0xFFAA00);
        }

        List<ServerPlan> current = plans;
        if (current == null) {
            drawCenteredString(this.fontRenderer, I18n.format(LangKeys.ORDER_LOADING),
                    this.width / 2, this.height / 2 - 4, 0xAAAAAA);
            return;
        }

        int y = listTop;
        for (ServerPlan plan : current) {
            if (y + ROW_HEIGHT > this.height - 52) {
                break;
            }
            String nameLine = plan == recommended
                    ? plan.getName() + "  " + I18n.format(LangKeys.ORDER_RECOMMENDED)
                    : plan.getName();
            drawString(this.fontRenderer, nameLine, LIST_LEFT_MARGIN, y, 0xFFFFFF);

            String details = I18n.format(LangKeys.ORDER_RAM, plan.ramDisplay());
            if (plan.slotsOrDefault() > 0) {
                details += "   " + I18n.format(LangKeys.ORDER_SLOTS, plan.slotsOrDefault());
            }
            details += "   " + (hourlySelected
                    ? I18n.format(LangKeys.ORDER_PRICE_HOURLY, plan.getPriceHourly(), "EUR")
                    : I18n.format(LangKeys.ORDER_PRICE, plan.monthlyOrEstimate(), "EUR"));
            if (!hourlySelected && plan.getFixedTerms() != null) {
                for (FixedTerm term : plan.getFixedTerms()) {
                    if (term.getTermDays() == TERM_OPTIONS[termIndex] && term.getDiscountPercent() > 0) {
                        details += "   " + I18n.format(LangKeys.ORDER_EFFECTIVE_MONTHLY,
                                term.getEffectiveMonthly(), "EUR");
                        break;
                    }
                }
            }
            drawString(this.fontRenderer, details, LIST_LEFT_MARGIN, y + 12, 0xAAAAAA);
            y += ROW_HEIGHT;
        }

        drawCenteredString(this.fontRenderer, I18n.format(LangKeys.ORDER_CHECKOUT_HINT),
                this.width / 2, this.height - 44, 0x777777);
    }

    @Override
    public void close() {
        this.mc.displayGuiScreen(parent);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
