package com.respawnhost.integration.ui;

import com.respawnhost.core.LangKeys;
import com.respawnhost.core.model.ModpackInfo;
import com.respawnhost.core.model.ServerPlan;
import com.respawnhost.core.order.OrderSession;
import com.respawnhost.core.recommend.PlanRecommender;
import com.respawnhost.integration.RespawnHostIntegrationMod;
import com.respawnhost.integration.config.RespawnConfig;
import com.respawnhost.integration.modpack.ModpackDetector;
import com.respawnhost.integration.util.BrowserUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

import java.util.List;

public class OrderScreen extends GuiScreen {
    private static final int ROW_HEIGHT = 30;
    private static final int LIST_LEFT_MARGIN = 40;
    private static final int ORDER_BUTTON_WIDTH = 110;

    private static final int ID_BILLING = 1;
    private static final int ID_TERM = 2;
    private static final int ID_REGION = 3;
    private static final int ID_BACK = 4;
    private static final int ID_CONFIG = 5;
    private static final int ID_ORDER_BASE = 100;

    private final GuiScreen parent;
    private final OrderSession session;
    private final String modpackSlug;
    private ServerPlan recommended;
    private int listTop;
    private boolean fetchStarted;

    public OrderScreen(GuiScreen parent) {
        this.parent = parent;
        RespawnConfig config = RespawnConfig.get();
        this.modpackSlug = ModpackDetector.detectModpackName();
        this.session = new OrderSession(config.apiBaseUrl(), config.panelBaseUrl(), config.gameShort(),
                config.region(), config.currency(), config.creatorCode(), modpackSlug);
    }

    private static <T> T next(List<T> values, T current) {
        return values.get((values.indexOf(current) + 1) % values.size());
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
        session.load().thenRun(() -> {
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
                I18n.format(OrderSession.modelKey(session.model())), () -> {
        });
        billingButton.setAction(() -> {
            session.model(next(OrderSession.MODELS, session.model()));
            rebuildContent();
        });
        addButton(billingButton);

        final RunnableButton termButton = new RunnableButton(ID_TERM, controlX + controlWidth + controlGap, controlY,
                controlWidth, 20, I18n.format(LangKeys.ORDER_TERM_DAYS, session.termDays()), () -> {
        });
        termButton.setAction(() -> {
            session.termDays(next(OrderSession.TERMS, session.termDays()));
            termButton.setMessage(I18n.format(LangKeys.ORDER_TERM_DAYS, session.termDays()));
        });
        termButton.enabled = session.termSelectable();
        addButton(termButton);

        final RunnableButton regionButton = new RunnableButton(ID_REGION,
                controlX + (controlWidth + controlGap) * 2, controlY, controlWidth, 20,
                I18n.format(LangKeys.ORDER_REGION, OrderSession.regionLabel(session.region())), () -> {
        });
        regionButton.setAction(() -> {
            session.region(next(OrderSession.REGIONS, session.region()));
            regionButton.setMessage(I18n.format(LangKeys.ORDER_REGION, OrderSession.regionLabel(session.region())));
        });
        addButton(regionButton);

        addButton(new RunnableButton(ID_BACK, this.width / 2 - 100, this.height - 28, 200, 20,
                I18n.format(LangKeys.ORDER_BACK),
                () -> this.mc.displayGuiScreen(parent)));

        addButton(new RunnableButton(ID_CONFIG, 4, this.height - 28, ORDER_BUTTON_WIDTH, 20,
                I18n.format(LangKeys.CONFIG_TITLE),
                () -> this.mc.displayGuiScreen(new ConfigScreen(this))));

        List<ServerPlan> current = session.plans();
        if (current == null || current.isEmpty()) {
            return;
        }
        ModpackInfo modpackInfo = session.modpackInfo();
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
                        session.trackOrderClick();
                        BrowserUtil.open(session.orderUrl(plan, uiLang()));
                    });
            orderButton.enabled = session.orderable(plan);
            addButton(orderButton);
            y += ROW_HEIGHT;
        }
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.render(mouseX, mouseY, partialTicks);
        drawCenteredString(this.fontRenderer, I18n.format(LangKeys.ORDER_TITLE), this.width / 2, 10, 0xFFFFFF);

        ModpackInfo modpackInfo = session.modpackInfo();
        if (modpackSlug != null) {
            String displayName = modpackInfo != null && modpackInfo.getRecommendedRamMb() != null
                    ? modpackInfo.getName()
                    : modpackSlug;
            drawCenteredString(this.fontRenderer,
                    I18n.format(LangKeys.ORDER_MODPACK_DETECTED, displayName),
                    this.width / 2, 52, 0x55FF55);
        }

        if (session.isOffline()) {
            drawCenteredString(this.fontRenderer, I18n.format(LangKeys.ORDER_OFFLINE),
                    this.width / 2, modpackSlug != null ? 64 : 52, 0xFFAA00);
        }

        List<ServerPlan> current = session.plans();
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
                    ? plan.displayName() + "  " + I18n.format(LangKeys.ORDER_RECOMMENDED)
                    : plan.displayName();
            drawString(this.fontRenderer, nameLine, LIST_LEFT_MARGIN, y, 0xFFFFFF);

            StringBuilder details = new StringBuilder();
            if (plan.slotsOrDefault() > 0) {
                details.append(I18n.format(LangKeys.ORDER_SLOTS, plan.slotsOrDefault())).append("   ");
            }
            for (OrderSession.Line line : session.priceLines(plan, uiLang())) {
                details.append(I18n.format(line.key, line.args)).append("   ");
            }
            drawString(this.fontRenderer, details.toString(), LIST_LEFT_MARGIN, y + 12, 0xAAAAAA);
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
