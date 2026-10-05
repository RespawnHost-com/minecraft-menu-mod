package com.respawnhost.integration.ui;

import com.respawnhost.core.LangKeys;
import com.respawnhost.core.model.ModpackInfo;
import com.respawnhost.core.model.ServerPlan;
import com.respawnhost.core.order.OrderSession;
import com.respawnhost.core.recommend.PlanRecommender;
import com.respawnhost.integration.config.RespawnConfig;
import com.respawnhost.integration.modpack.ModpackDetector;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
        super(Component.translatable(LangKeys.ORDER_TITLE));
        this.parent = parent;
        RespawnConfig config = RespawnConfig.get();
        this.modpackSlug = ModpackDetector.detectModpackName();
        this.session = new OrderSession(config.apiBaseUrl(), config.panelBaseUrl(), config.gameShort(),
                config.region(), config.currency(), config.creatorCode(), modpackSlug);
        session.load().thenRun(() -> Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().screen == this) {
                rebuildWidgets();
            }
        }));
    }

    private static String uiLang() {
        return Minecraft.getInstance().getLanguageManager().getSelected().startsWith("de") ? "de" : "en";
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
        int controlWidth = 100;
        int controlGap = 8;
        int controlsTotal = controlWidth * 3 + controlGap * 2;
        int controlX = this.width / 2 - controlsTotal / 2;

        addRenderableWidget(CycleButton.<String>builder(model ->
                        Component.translatable(OrderSession.modelKey(model)), session.model())
                .withValues(OrderSession.MODELS)
                .displayOnlyValue()
                .create(controlX, controlY, controlWidth, 20, Component.empty(), (button, value) -> {
                    session.model(value);
                    rebuildWidgets();
                }));

        CycleButton<Integer> termButton = CycleButton.<Integer>builder(days ->
                        Component.translatable(LangKeys.ORDER_TERM_DAYS, days), session.termDays())
                .withValues(OrderSession.TERMS)
                .displayOnlyValue()
                .create(controlX + controlWidth + controlGap, controlY, controlWidth, 20, Component.empty(),
                        (button, value) -> session.termDays(value));
        termButton.active = session.termSelectable();
        addRenderableWidget(termButton);

        addRenderableWidget(CycleButton.<String>builder(region ->
                        Component.translatable(LangKeys.ORDER_REGION, OrderSession.regionLabel(region)), session.region())
                .withValues(OrderSession.REGIONS)
                .displayOnlyValue()
                .create(controlX + (controlWidth + controlGap) * 2, controlY, controlWidth, 20, Component.empty(),
                        (button, value) -> session.region(value)));

        addRenderableWidget(Button.builder(Component.translatable(LangKeys.ORDER_BACK), button -> onClose())
                .bounds(this.width / 2 - 152, this.height - 28, 148, 20)
                .build());

        addRenderableWidget(Button.builder(Component.translatable(LangKeys.CONFIG_TITLE),
                        button -> {
                            if (this.minecraft != null) {
                                this.minecraft.setScreen(new ConfigScreen(this));
                            }
                        })
                .bounds(this.width / 2 + 4, this.height - 28, 148, 20)
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
            Button orderButton = Button.builder(Component.translatable(LangKeys.ORDER_ORDER_NOW),
                            button -> {
                                session.trackOrderClick();
                                Util.getPlatform().openUri(session.orderUrl(plan, uiLang()));
                            })
                    .bounds(buttonX, y, ORDER_BUTTON_WIDTH, 20)
                    .build();
            orderButton.active = session.orderable(plan);
            addRenderableWidget(orderButton);
            y += ROW_HEIGHT;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFFFF);

        ModpackInfo modpackInfo = session.modpackInfo();
        if (modpackSlug != null) {
            String displayName = modpackInfo != null && modpackInfo.getRecommendedRamMb() != null
                    ? modpackInfo.getName()
                    : modpackSlug;
            guiGraphics.drawCenteredString(this.font,
                    Component.translatable(LangKeys.ORDER_MODPACK_DETECTED, displayName),
                    this.width / 2, 52, 0xFF55FF55);
        }

        if (session.isOffline()) {
            guiGraphics.drawCenteredString(this.font,
                    Component.translatable(LangKeys.ORDER_OFFLINE),
                    this.width / 2, modpackSlug != null ? 64 : 52, 0xFFFFAA00);
        }

        List<ServerPlan> current = session.plans();
        if (current == null) {
            guiGraphics.drawCenteredString(this.font,
                    Component.translatable(LangKeys.ORDER_LOADING),
                    this.width / 2, this.height / 2 - 4, 0xFFAAAAAA);
            return;
        }

        int y = listTop;
        for (ServerPlan plan : current) {
            if (y + ROW_HEIGHT > this.height - 52) {
                break;
            }
            Component nameLine = plan == recommended
                    ? Component.literal(plan.displayName()).append("  ")
                            .append(Component.translatable(LangKeys.ORDER_RECOMMENDED))
                    : Component.literal(plan.displayName());
            guiGraphics.drawString(this.font, nameLine, LIST_LEFT_MARGIN, y, 0xFFFFFFFF);

            MutableComponent details = Component.empty();
            if (plan.slotsOrDefault() > 0) {
                details.append(Component.translatable(LangKeys.ORDER_SLOTS, plan.slotsOrDefault())).append("   ");
            }
            for (OrderSession.Line line : session.priceLines(plan, uiLang())) {
                details.append(Component.translatable(line.key, line.args)).append("   ");
            }
            guiGraphics.drawString(this.font, details, LIST_LEFT_MARGIN, y + 12, 0xFFAAAAAA);
            y += ROW_HEIGHT;
        }

        guiGraphics.drawCenteredString(this.font,
                Component.translatable(LangKeys.ORDER_CHECKOUT_HINT),
                this.width / 2, this.height - 44, 0xFF777777);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }
}

