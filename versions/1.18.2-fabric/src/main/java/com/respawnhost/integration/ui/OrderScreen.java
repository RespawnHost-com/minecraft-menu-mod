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
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Util;

import java.awt.Desktop;
import java.net.URI;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

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
        this.session = new OrderSession(config.apiBaseUrl(), config.panelBaseUrl(), config.gameShort(),
                config.region(), config.currency(), config.creatorCode(), modpackSlug);
        session.load().thenRun(() -> MinecraftClient.getInstance().execute(() -> {
            if (MinecraftClient.getInstance().currentScreen == this) {
                rebuildContent();
            }
        }));
    }

    private static String uiLang() {
        return MinecraftClient.getInstance().getLanguageManager().getLanguage().getCode().startsWith("de") ? "de" : "en";
    }

    private void rebuildContent() {
        clearChildren();
        init();
    }

    private <T> ButtonWidget addCycleButton(int x, int y, int width, List<T> values, T current,
                                            Function<T, Text> label, Consumer<T> onChange) {
        ButtonWidget[] self = new ButtonWidget[1];
        int[] index = {Math.max(0, values.indexOf(current))};
        ButtonWidget button = new ButtonWidget(x, y, width, 20, label.apply(values.get(index[0])), pressed -> {
            index[0] = (index[0] + 1) % values.size();
            T value = values.get(index[0]);
            self[0].setMessage(label.apply(value));
            onChange.accept(value);
        });
        self[0] = button;
        return addDrawableChild(button);
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
                model -> new TranslatableText(OrderSession.modelKey(model)),
                value -> {
                    session.model(value);
                    rebuildContent();
                });

        ButtonWidget termButton = addCycleButton(controlX + controlWidth + controlGap, controlY, controlWidth,
                OrderSession.TERMS, session.termDays(),
                days -> new TranslatableText(LangKeys.ORDER_TERM_DAYS, days),
                value -> session.termDays(value));
        termButton.active = session.termSelectable();

        addCycleButton(controlX + (controlWidth + controlGap) * 2, controlY, controlWidth,
                OrderSession.REGIONS, session.region(),
                region -> new TranslatableText(LangKeys.ORDER_REGION, OrderSession.regionLabel(region)),
                value -> session.region(value));

        addCycleButton(controlX + (controlWidth + controlGap) * 3, controlY, controlWidth,
                session.currencyCodes(), session.currencyCode(),
                code -> new LiteralText(code),
                value -> session.currency(value));

        addDrawableChild(new ButtonWidget(this.width / 2 - 100, this.height - 28, 200, 20,
                new TranslatableText(LangKeys.ORDER_BACK), button -> close()));

        addDrawableChild(new ButtonWidget(4, this.height - 28, ORDER_BUTTON_WIDTH, 20,
                new TranslatableText(LangKeys.CONFIG_TITLE),
                button -> {
                    if (this.client != null) {
                        this.client.setScreen(new ConfigScreen(this));
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
                    new TranslatableText(LangKeys.ORDER_ORDER_NOW),
                    button -> {
                        session.trackOrderClick();
                        openUri(session.orderUrl(plan, uiLang()));
                    });
            orderButton.active = session.orderable(plan);
            addDrawableChild(orderButton);
            y += ROW_HEIGHT;
        }
    }

    private static void openUri(String uri) {
        try {
            Util.getOperatingSystem().open(uri);
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
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.render(matrices, mouseX, mouseY, delta);
        drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 10, 0xFFFFFF);

        ModpackInfo modpackInfo = session.modpackInfo();
        if (modpackSlug != null) {
            String displayName = modpackInfo != null && modpackInfo.getRecommendedRamMb() != null
                    ? modpackInfo.getName()
                    : modpackSlug;
            drawCenteredText(matrices, this.textRenderer,
                    new TranslatableText(LangKeys.ORDER_MODPACK_DETECTED, displayName),
                    this.width / 2, 52, 0x55FF55);
        }

        if (session.isOffline()) {
            drawCenteredText(matrices, this.textRenderer,
                    new TranslatableText(LangKeys.ORDER_OFFLINE),
                    this.width / 2, modpackSlug != null ? 64 : 52, 0xFFAA00);
        }

        List<ServerPlan> current = session.plans();
        if (current == null) {
            drawCenteredText(matrices, this.textRenderer,
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
                    ? new LiteralText(plan.displayName()).append("  ")
                    .append(new TranslatableText(LangKeys.ORDER_RECOMMENDED))
                    : new LiteralText(plan.displayName());
            drawTextWithShadow(matrices, this.textRenderer, nameLine, LIST_LEFT_MARGIN, y, 0xFFFFFF);

            MutableText details = new LiteralText("");
            if (plan.slotsOrDefault() > 0) {
                details.append(new TranslatableText(LangKeys.ORDER_SLOTS, plan.slotsOrDefault())).append("   ");
            }
            for (OrderSession.Line line : session.priceLines(plan, uiLang())) {
                details.append(new TranslatableText(line.key, line.args)).append("   ");
            }
            drawTextWithShadow(matrices, this.textRenderer, details, LIST_LEFT_MARGIN, y + 12, 0xAAAAAA);
            y += ROW_HEIGHT;
        }

        drawCenteredText(matrices, this.textRenderer,
                new TranslatableText(LangKeys.ORDER_CHECKOUT_HINT),
                this.width / 2, this.height - 44, 0x777777);
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }
}
