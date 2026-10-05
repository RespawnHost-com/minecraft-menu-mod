package com.respawnhost.integration.ui;

import com.respawnhost.core.LangKeys;
import com.respawnhost.core.model.ModpackInfo;
import com.respawnhost.core.model.ServerPlan;
import com.respawnhost.core.order.OrderSession;
import com.respawnhost.core.recommend.PlanRecommender;
import com.respawnhost.integration.RespawnHostIntegrationFabric;
import com.respawnhost.integration.config.RespawnConfig;
import com.respawnhost.integration.modpack.ModpackDetector;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.SystemUtil;

import java.awt.Desktop;
import java.net.URI;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

public class OrderScreen extends Screen {
    private static final int ROW_HEIGHT = 30;
    private static final int LIST_LEFT_MARGIN = 40;
    private static final int ORDER_BUTTON_WIDTH = 110;

    private final Screen parent;
    private final OrderSession session;
    private final String modpackSlug;
    private final AtomicBoolean pendingRefresh = new AtomicBoolean();
    private ServerPlan recommended;
    private int listTop;

    public OrderScreen(Screen parent) {
        super(new TranslatableText(LangKeys.ORDER_TITLE));
        this.parent = parent;
        RespawnConfig config = RespawnConfig.get();
        this.modpackSlug = ModpackDetector.detectModpackName();
        this.session = new OrderSession(config.apiBaseUrl(), config.panelBaseUrl(), config.gameShort(),
                config.region(), config.currency(), config.creatorCode(), modpackSlug);
        session.load().thenRun(() -> pendingRefresh.set(true));
    }

    private static String uiLang() {
        try {
            return MinecraftClient.getInstance().getLanguageManager().getLanguage().getCode().startsWith("de") ? "de" : "en";
        } catch (RuntimeException e) {
            return "en";
        }
    }

    private void rebuildContent() {
        this.buttons.clear();
        this.children.clear();
        init();
    }

    private <T> ButtonWidget addCycleButton(int x, int y, int width, List<T> values, T current,
                                            Function<T, String> label, Consumer<T> onChange) {
        ButtonWidget[] self = new ButtonWidget[1];
        int[] index = {Math.max(0, values.indexOf(current))};
        ButtonWidget button = new ButtonWidget(x, y, width, 20, label.apply(values.get(index[0])), pressed -> {
            index[0] = (index[0] + 1) % values.size();
            T value = values.get(index[0]);
            self[0].setMessage(label.apply(value));
            onChange.accept(value);
        });
        self[0] = button;
        return addButton(button);
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

        addCycleButton(controlX, controlY, controlWidth, OrderSession.MODELS, session.model(),
                model -> I18n.translate(OrderSession.modelKey(model)),
                value -> {
                    session.model(value);
                    rebuildContent();
                });

        ButtonWidget termButton = addCycleButton(controlX + controlWidth + controlGap, controlY, controlWidth,
                OrderSession.TERMS, session.termDays(),
                days -> I18n.translate(LangKeys.ORDER_TERM_DAYS, days),
                value -> session.termDays(value));
        termButton.active = session.termSelectable();

        addCycleButton(controlX + (controlWidth + controlGap) * 2, controlY, controlWidth,
                OrderSession.REGIONS, session.region(),
                region -> I18n.translate(LangKeys.ORDER_REGION, OrderSession.regionLabel(region)),
                value -> session.region(value));

        addCycleButton(controlX + (controlWidth + controlGap) * 3, controlY, controlWidth,
                session.currencyCodes(), session.currencyCode(),
                code -> code,
                value -> session.currency(value));

        addButton(new ButtonWidget(this.width / 2 - 100, this.height - 28, 200, 20,
                I18n.translate(LangKeys.ORDER_BACK), button -> onClose()));

        addButton(new ButtonWidget(4, this.height - 28, ORDER_BUTTON_WIDTH, 20,
                I18n.translate(LangKeys.CONFIG_TITLE),
                button -> {
                    if (this.minecraft != null) {
                        this.minecraft.openScreen(new ConfigScreen(this));
                    }
                }));

        List<ServerPlan> current = session.plans();
        if (current == null || current.isEmpty()) {
            return;
        }
        ModpackInfo modpackInfo = session.modpackInfo();
        recommended = PlanRecommender.recommend(current,
                modpackInfo != null ? modpackInfo.getRecommendedRamMb() : null,
                RespawnHostIntegrationFabric.loadedModCount());
        int y = listTop;
        int buttonX = Math.max(this.width - ORDER_BUTTON_WIDTH - LIST_LEFT_MARGIN, this.width / 2 + 40);
        for (ServerPlan plan : current) {
            if (y + ROW_HEIGHT > this.height - 52) {
                break;
            }
            ButtonWidget orderButton = new ButtonWidget(buttonX, y, ORDER_BUTTON_WIDTH, 20,
                    I18n.translate(LangKeys.ORDER_ORDER_NOW),
                    button -> {
                        session.trackOrderClick();
                        openUri(session.orderUrl(plan, uiLang()));
                    });
            orderButton.active = session.orderable(plan);
            addButton(orderButton);
            y += ROW_HEIGHT;
        }
    }

    private static void openUri(String uri) {
        try {
            SystemUtil.getOperatingSystem().open(uri);
        } catch (RuntimeException e) {
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(new URI(uri));
                }
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void render(int mouseX, int mouseY, float delta) {
        if (pendingRefresh.compareAndSet(true, false)) {
            rebuildContent();
        }
        super.render(mouseX, mouseY, delta);
        drawCenteredString(this.font, I18n.translate(LangKeys.ORDER_TITLE), this.width / 2, 10, 0xFFFFFF);

        ModpackInfo modpackInfo = session.modpackInfo();
        if (modpackSlug != null) {
            String displayName = modpackInfo != null && modpackInfo.getRecommendedRamMb() != null
                    ? modpackInfo.getName()
                    : modpackSlug;
            drawCenteredString(this.font,
                    I18n.translate(LangKeys.ORDER_MODPACK_DETECTED, displayName),
                    this.width / 2, 52, 0x55FF55);
        }

        if (session.isOffline()) {
            drawCenteredString(this.font,
                    I18n.translate(LangKeys.ORDER_OFFLINE),
                    this.width / 2, modpackSlug != null ? 64 : 52, 0xFFAA00);
        }

        List<ServerPlan> current = session.plans();
        if (current == null) {
            drawCenteredString(this.font,
                    I18n.translate(LangKeys.ORDER_LOADING),
                    this.width / 2, this.height / 2 - 4, 0xAAAAAA);
            return;
        }

        int y = listTop;
        for (ServerPlan plan : current) {
            if (y + ROW_HEIGHT > this.height - 52) {
                break;
            }
            String nameLine = plan == recommended
                    ? plan.displayName() + "  " + I18n.translate(LangKeys.ORDER_RECOMMENDED)
                    : plan.displayName();
            drawString(this.font, nameLine, LIST_LEFT_MARGIN, y, 0xFFFFFF);

            StringBuilder details = new StringBuilder();
            if (plan.slotsOrDefault() > 0) {
                details.append(I18n.translate(LangKeys.ORDER_SLOTS, plan.slotsOrDefault())).append("   ");
            }
            for (OrderSession.Line line : session.priceLines(plan, uiLang())) {
                details.append(I18n.translate(line.key, line.args)).append("   ");
            }
            drawString(this.font, details.toString(), LIST_LEFT_MARGIN, y + 12, 0xAAAAAA);
            y += ROW_HEIGHT;
        }

        drawCenteredString(this.font,
                I18n.translate(LangKeys.ORDER_CHECKOUT_HINT),
                this.width / 2, this.height - 44, 0x777777);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.openScreen(parent);
        }
    }
}
