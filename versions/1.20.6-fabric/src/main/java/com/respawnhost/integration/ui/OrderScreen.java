package com.respawnhost.integration.ui;

import com.respawnhost.core.LangKeys;
import com.respawnhost.core.model.ModpackInfo;
import com.respawnhost.core.model.ServerPlan;
import com.respawnhost.core.order.OrderSession;
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

public class OrderScreen extends Screen {
    private static final int ROW_HEIGHT = 30;
    private static final int LIST_LEFT_MARGIN = 40;
    private static final int ORDER_BUTTON_WIDTH = 110;

    private final Screen parent;
    private final OrderSession session;
    private final @Nullable String modpackSlug;
    private @Nullable ServerPlan recommended;
    private int listTop;

    public OrderScreen(Screen parent) {
        super(Text.translatable(LangKeys.ORDER_TITLE));
        this.parent = parent;
        RespawnConfig config = RespawnConfig.get();
        this.modpackSlug = ModpackDetector.detectModpackName();
        this.session = new OrderSession(config.apiBaseUrl(), config.panelBaseUrl(), config.gameShort(),
                config.region(), config.currency(), config.creatorCode(), modpackSlug);
        session.load().thenRun(() -> MinecraftClient.getInstance().execute(() -> {
            if (MinecraftClient.getInstance().currentScreen == this) {
                clearAndInit();
            }
        }));
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
        int controlWidth = 80;
        int controlGap = 6;
        int controlsTotal = controlWidth * 4 + controlGap * 3;
        int controlX = this.width / 2 - controlsTotal / 2;

        addDrawableChild(CyclingButtonWidget.<String>builder(model ->
                        Text.translatable(OrderSession.modelKey(model)))
                .values(OrderSession.MODELS)
                .initially(session.model())
                .omitKeyText()
                .build(controlX, controlY, controlWidth, 20, Text.empty(), (button, value) -> {
                    session.model(value);
                    clearAndInit();
                }));

        CyclingButtonWidget<Integer> termButton = CyclingButtonWidget.<Integer>builder(days ->
                        Text.translatable(LangKeys.ORDER_TERM_DAYS, days))
                .values(OrderSession.TERMS)
                .initially(session.termDays())
                .omitKeyText()
                .build(controlX + controlWidth + controlGap, controlY, controlWidth, 20, Text.empty(),
                        (button, value) -> session.termDays(value));
        termButton.active = session.termSelectable();
        addDrawableChild(termButton);

        addDrawableChild(CyclingButtonWidget.<String>builder(region ->
                        Text.translatable(LangKeys.ORDER_REGION, OrderSession.regionLabel(region)))
                .values(OrderSession.REGIONS)
                .initially(session.region())
                .omitKeyText()
                .build(controlX + (controlWidth + controlGap) * 2, controlY, controlWidth, 20, Text.empty(),
                        (button, value) -> session.region(value)));

        addDrawableChild(CyclingButtonWidget.<String>builder(Text::literal)
                .values(session.currencyCodes())
                .initially(session.currencyCode())
                .omitKeyText()
                .build(controlX + (controlWidth + controlGap) * 3, controlY, controlWidth, 20, Text.empty(),
                        (button, value) -> session.currency(value)));

        addDrawableChild(ButtonWidget.builder(Text.translatable(LangKeys.CONFIG_TITLE),
                        button -> MinecraftClient.getInstance().setScreen(new ConfigScreen(this)))
                .dimensions(this.width - 116, 6, 110, 20)
                .build());

        addDrawableChild(ButtonWidget.builder(Text.translatable(LangKeys.ORDER_BACK), button -> close())
                .dimensions(this.width / 2 - 100, this.height - 28, 200, 20)
                .build());

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
            ButtonWidget orderButton = ButtonWidget.builder(Text.translatable(LangKeys.ORDER_ORDER_NOW),
                            button -> {
                                session.trackOrderClick();
                                Util.getOperatingSystem().open(session.orderUrl(plan, uiLang()));
                            })
                    .dimensions(buttonX, y, ORDER_BUTTON_WIDTH, 20)
                    .build();
            orderButton.active = session.orderable(plan);
            addDrawableChild(orderButton);
            y += ROW_HEIGHT;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, 0xFFFFFF);

        ModpackInfo modpackInfo = session.modpackInfo();
        if (modpackSlug != null) {
            String displayName = modpackInfo != null && modpackInfo.getRecommendedRamMb() != null
                    ? modpackInfo.getName()
                    : modpackSlug;
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.translatable(LangKeys.ORDER_MODPACK_DETECTED, displayName),
                    this.width / 2, 52, 0x55FF55);
        }

        if (session.isOffline()) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.translatable(LangKeys.ORDER_OFFLINE),
                    this.width / 2, modpackSlug != null ? 64 : 52, 0xFFAA00);
        }

        List<ServerPlan> current = session.plans();
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
                    ? Text.literal(plan.displayName()).append("  ")
                            .append(Text.translatable(LangKeys.ORDER_RECOMMENDED))
                    : Text.literal(plan.displayName());
            context.drawTextWithShadow(this.textRenderer, nameLine, LIST_LEFT_MARGIN, y, 0xFFFFFF);

            MutableText details = Text.empty();
            if (plan.slotsOrDefault() > 0) {
                details.append(Text.translatable(LangKeys.ORDER_SLOTS, plan.slotsOrDefault())).append("   ");
            }
            for (OrderSession.Line line : session.priceLines(plan, uiLang())) {
                details.append(Text.translatable(line.key, line.args)).append("   ");
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
