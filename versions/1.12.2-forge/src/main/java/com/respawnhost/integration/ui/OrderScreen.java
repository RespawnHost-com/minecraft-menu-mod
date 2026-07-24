package com.respawnhost.integration.ui;

import com.respawnhost.core.LangKeys;
import com.respawnhost.core.api.FallbackPlans;
import com.respawnhost.core.api.RespawnApiClient;
import com.respawnhost.core.model.FixedTerm;
import com.respawnhost.core.model.ModpackInfo;
import com.respawnhost.core.model.ServerPlan;
import com.respawnhost.core.recommend.PlanRecommender;
import com.respawnhost.integration.config.RespawnConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiConfirmOpenLink;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.common.Loader;

import java.awt.Desktop;
import java.net.URI;
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
        String packId = config.packId();
        this.modpackSlug = packId == null || packId.trim().isEmpty() ? null : packId.trim();
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
            return Minecraft.getMinecraft().getLanguageManager().getCurrentLanguage().getLanguageCode()
                    .startsWith("de") ? "de" : "en";
        } catch (RuntimeException e) {
            return "en";
        }
    }

    private static int loadedModCount() {
        try {
            return Loader.instance().getModList().size();
        } catch (RuntimeException e) {
            return 0;
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
            Minecraft.getMinecraft().addScheduledTask(() -> {
                if (Minecraft.getMinecraft().currentScreen == this) {
                    initGui();
                }
            });
        });
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        startFetchIfNeeded();

        boolean showModpackLine = modpackSlug != null;
        listTop = showModpackLine ? 78 : 66;

        int controlY = 26;
        int controlWidth = 100;
        int controlGap = 8;
        int controlsTotal = controlWidth * 3 + controlGap * 2;
        int controlX = this.width / 2 - controlsTotal / 2;

        this.buttonList.add(new GuiButton(ID_BILLING, controlX, controlY, controlWidth, 20,
                I18n.format(hourlySelected ? LangKeys.ORDER_MODEL_HOURLY : LangKeys.ORDER_MODEL_FIXED)));

        GuiButton termButton = new GuiButton(ID_TERM, controlX + controlWidth + controlGap, controlY,
                controlWidth, 20, I18n.format(LangKeys.ORDER_TERM_DAYS, TERM_OPTIONS[termIndex]));
        termButton.enabled = !hourlySelected;
        this.buttonList.add(termButton);

        this.buttonList.add(new GuiButton(ID_REGION, controlX + (controlWidth + controlGap) * 2, controlY,
                controlWidth, 20,
                I18n.format(LangKeys.ORDER_REGION, regionSelected.toUpperCase(Locale.ROOT))));

        this.buttonList.add(new GuiButton(ID_CONFIG, this.width - 116, 6, 110, 20,
                I18n.format(LangKeys.CONFIG_TITLE)));

        this.buttonList.add(new GuiButton(ID_BACK, this.width / 2 - 100, this.height - 28, 200, 20,
                I18n.format(LangKeys.ORDER_BACK)));

        List<ServerPlan> current = plans;
        if (current == null || current.isEmpty()) {
            return;
        }
        recommended = PlanRecommender.recommend(current,
                modpackInfo != null ? modpackInfo.getRecommendedRamMb() : null, loadedModCount());
        int y = listTop;
        int buttonX = Math.max(this.width - ORDER_BUTTON_WIDTH - LIST_LEFT_MARGIN, this.width / 2 + 40);
        for (int i = 0; i < current.size(); i++) {
            if (y + ROW_HEIGHT > this.height - 52) {
                break;
            }
            ServerPlan plan = current.get(i);
            GuiButton orderButton = new GuiButton(ID_ORDER_BASE + i, buttonX, y, ORDER_BUTTON_WIDTH, 20,
                    I18n.format(LangKeys.ORDER_ORDER_NOW));
            orderButton.enabled = hourlySelected ? plan.isAvailableHourly() : plan.isAvailableFixed();
            this.buttonList.add(orderButton);
            y += ROW_HEIGHT;
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (!button.enabled) {
            return;
        }
        switch (button.id) {
            case ID_BILLING:
                hourlySelected = !hourlySelected;
                initGui();
                break;
            case ID_TERM:
                termIndex = (termIndex + 1) % TERM_OPTIONS.length;
                initGui();
                break;
            case ID_REGION:
                regionSelected = REGION_OPTIONS[0].equals(regionSelected) ? REGION_OPTIONS[1] : REGION_OPTIONS[0];
                initGui();
                break;
            case ID_BACK:
                this.mc.displayGuiScreen(parent);
                break;
            case ID_CONFIG:
                this.mc.displayGuiScreen(new ConfigScreen(this));
                break;
            default:
                int index = button.id - ID_ORDER_BASE;
                List<ServerPlan> current = plans;
                if (current != null && index >= 0 && index < current.size()) {
                    openUrl(client.buildOrderUrl(current.get(index), hourlySelected,
                            TERM_OPTIONS[termIndex], regionSelected, uiLang()));
                }
                break;
        }
    }

    private void openUrl(String url) {
        try {
            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                return;
            }
        } catch (Exception ignored) {
        }
        this.mc.displayGuiScreen(new GuiConfirmOpenLink(this, url, 0, true));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
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
        } else {
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

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
