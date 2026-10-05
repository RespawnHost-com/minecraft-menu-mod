package com.respawnhost.integration.fabric.ui;

import com.respawnhost.core.LangKeys;
import com.respawnhost.core.model.ModpackInfo;
import com.respawnhost.core.model.ServerPlan;
import com.respawnhost.core.order.OrderSession;
import com.respawnhost.core.recommend.PlanRecommender;
import com.respawnhost.integration.fabric.config.RespawnConfig;
import com.respawnhost.integration.fabric.modpack.ModpackDetector;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.TranslatableText;

import java.util.List;

public class OrderScreen extends Screen {
    private static final int ROW_HEIGHT = 30;
    private static final int LIST_LEFT_MARGIN = 40;
    private static final int ORDER_BUTTON_WIDTH = 110;

    private final Screen parent;
    private final OrderSession session;
    private final String modpackSlug;
    private ServerPlan recommended;
    private int listTop;

    public OrderScreen(Screen parent) {
        super(new TranslatableText(LangKeys.ORDER_TITLE));
        this.parent = parent;
        RespawnConfig config = RespawnConfig.get();
        this.modpackSlug = ModpackDetector.detectModpackName();
        this.session = new OrderSession(config.getApiBaseUrl(), config.getPanelBaseUrl(), config.getGameShort(),
                config.getRegion(), config.getCurrency(), config.creatorCode(), modpackSlug);
        session.load().thenRun(() -> MinecraftClient.getInstance().execute(() -> {
            if (MinecraftClient.getInstance().currentScreen == this) {
                rebuild();
            }
        }));
    }

    private void rebuild() {
        this.init(MinecraftClient.getInstance(), this.width, this.height);
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

    private static String tr(String key, Object... args) {
        return new TranslatableText(key, args).asFormattedString();
    }

    private static <T> T next(List<T> values, T current) {
        return values.get((values.indexOf(current) + 1) % values.size());
    }

    private String termLabel() {
        return tr(LangKeys.ORDER_TERM_DAYS, session.termDays());
    }

    private String regionLabel() {
        return tr(LangKeys.ORDER_REGION, OrderSession.regionLabel(session.region()));
    }

    @Override
    protected void init() {
        boolean showModpackLine = modpackSlug != null;
        listTop = showModpackLine ? 78 : 66;

        int controlY = 26;
        int controlWidth = 80;
        int controlGap = 6;
        int controlsTotal = controlWidth * 4 + controlGap * 3;
        int controlX = this.width / 2 - controlsTotal / 2;

        this.addButton(new ButtonWidget(controlX, controlY, controlWidth, 20,
                tr(OrderSession.modelKey(session.model())), button -> {
            session.model(next(OrderSession.MODELS, session.model()));
            rebuild();
        }));

        ButtonWidget termButton = new ButtonWidget(controlX + controlWidth + controlGap, controlY,
                controlWidth, 20, termLabel(), button -> {
            session.termDays(next(OrderSession.TERMS, session.termDays()));
            button.setMessage(termLabel());
        });
        termButton.active = session.termSelectable();
        this.addButton(termButton);

        this.addButton(new ButtonWidget(controlX + (controlWidth + controlGap) * 2, controlY, controlWidth, 20,
                regionLabel(), button -> {
            session.region(next(OrderSession.REGIONS, session.region()));
            button.setMessage(regionLabel());
        }));

        this.addButton(new ButtonWidget(controlX + (controlWidth + controlGap) * 3, controlY, controlWidth, 20,
                session.currencyCode(), button -> {
            session.currency(next(session.currencyCodes(), session.currencyCode()));
            button.setMessage(session.currencyCode());
        }));

        this.addButton(new ButtonWidget(this.width / 2 - 102, this.height - 28, 99, 20,
                tr(LangKeys.ORDER_BACK), button -> onClose()));
        this.addButton(new ButtonWidget(this.width / 2 + 3, this.height - 28, 99, 20,
                tr(LangKeys.CONFIG_TITLE),
                button -> MinecraftClient.getInstance().openScreen(new ConfigScreen(this))));

        List<ServerPlan> current = session.plans();
        if (current == null || current.isEmpty()) {
            return;
        }
        ModpackInfo modpackInfo = session.modpackInfo();
        recommended = PlanRecommender.recommend(current,
                modpackInfo != null ? modpackInfo.getRecommendedRamMb() : null, loadedModCount());
        int y = listTop;
        int buttonX = Math.max(this.width - ORDER_BUTTON_WIDTH - LIST_LEFT_MARGIN, this.width / 2 + 40);
        for (ServerPlan plan : current) {
            if (y + ROW_HEIGHT > this.height - 52) {
                break;
            }
            ButtonWidget orderButton = new ButtonWidget(buttonX, y, ORDER_BUTTON_WIDTH, 20,
                    tr(LangKeys.ORDER_ORDER_NOW),
                    button -> {
                        session.trackOrderClick();
                        BrowserUtil.open(session.orderUrl(plan, uiLang()));
                    });
            orderButton.active = session.orderable(plan);
            this.addButton(orderButton);
            y += ROW_HEIGHT;
        }
    }

    @Override
    public void render(int mouseX, int mouseY, float delta) {
        this.renderBackground();
        super.render(mouseX, mouseY, delta);
        this.drawCenteredString(MinecraftClient.getInstance().textRenderer, tr(LangKeys.ORDER_TITLE),
                this.width / 2, 10, 0xFFFFFF);

        ModpackInfo modpackInfo = session.modpackInfo();
        if (modpackSlug != null) {
            String displayName = modpackInfo != null && modpackInfo.getRecommendedRamMb() != null
                    ? modpackInfo.getName()
                    : modpackSlug;
            this.drawCenteredString(MinecraftClient.getInstance().textRenderer,
                    tr(LangKeys.ORDER_MODPACK_DETECTED, displayName),
                    this.width / 2, 52, 0x55FF55);
        }

        if (session.isOffline()) {
            this.drawCenteredString(MinecraftClient.getInstance().textRenderer,
                    tr(LangKeys.ORDER_OFFLINE),
                    this.width / 2, modpackSlug != null ? 64 : 52, 0xFFAA00);
        }

        List<ServerPlan> current = session.plans();
        if (current == null) {
            this.drawCenteredString(MinecraftClient.getInstance().textRenderer,
                    tr(LangKeys.ORDER_LOADING),
                    this.width / 2, this.height / 2 - 4, 0xAAAAAA);
            return;
        }

        int y = listTop;
        for (ServerPlan plan : current) {
            if (y + ROW_HEIGHT > this.height - 52) {
                break;
            }
            String nameLine = plan == recommended
                    ? plan.displayName() + "  " + tr(LangKeys.ORDER_RECOMMENDED)
                    : plan.displayName();
            this.drawString(MinecraftClient.getInstance().textRenderer, nameLine, LIST_LEFT_MARGIN, y, 0xFFFFFF);

            StringBuilder details = new StringBuilder();
            if (plan.slotsOrDefault() > 0) {
                details.append(tr(LangKeys.ORDER_SLOTS, plan.slotsOrDefault())).append("   ");
            }
            for (OrderSession.Line line : session.priceLines(plan, uiLang())) {
                details.append(tr(line.key, line.args)).append("   ");
            }
            this.drawString(MinecraftClient.getInstance().textRenderer, details.toString(),
                    LIST_LEFT_MARGIN, y + 12, 0xAAAAAA);
            y += ROW_HEIGHT;
        }

        this.drawCenteredString(MinecraftClient.getInstance().textRenderer,
                tr(LangKeys.ORDER_CHECKOUT_HINT),
                this.width / 2, this.height - 44, 0x777777);
    }

    @Override
    public void onClose() {
        MinecraftClient.getInstance().openScreen(parent);
    }
}
