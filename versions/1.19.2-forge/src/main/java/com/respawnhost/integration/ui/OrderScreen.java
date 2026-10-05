package com.respawnhost.integration.ui;

import com.respawnhost.core.LangKeys;
import com.respawnhost.core.model.ModpackInfo;
import com.respawnhost.core.model.ServerPlan;
import com.respawnhost.core.order.OrderSession;
import com.respawnhost.core.recommend.PlanRecommender;
import com.respawnhost.integration.config.RespawnConfig;
import com.respawnhost.integration.modpack.ModpackDetector;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.Nullable;

import java.awt.Desktop;
import java.net.URI;
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
        return Minecraft.getInstance().getLanguageManager().getSelected().getCode().startsWith("de") ? "de" : "en";
    }

    private static int loadedModCount() {
        try {
            return ModList.get().size();
        } catch (RuntimeException e) {
            return 0;
        }
    }

    private static void openUri(String url) {
        try {
            Util.getPlatform().openUri(url);
        } catch (RuntimeException e) {
            try {
                Desktop.getDesktop().browse(new URI(url));
            } catch (Exception ignored) {
            }
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

        addRenderableWidget(CycleButton.<String>builder(model -> Component.translatable(OrderSession.modelKey(model)))
                .withValues(OrderSession.MODELS)
                .withInitialValue(session.model())
                .displayOnlyValue()
                .create(controlX, controlY, controlWidth, 20, Component.empty(), (button, value) -> {
                    session.model(value);
                    rebuildWidgets();
                }));

        CycleButton<Integer> termButton = CycleButton.<Integer>builder(days ->
                        Component.translatable(LangKeys.ORDER_TERM_DAYS, days))
                .withValues(OrderSession.TERMS)
                .withInitialValue(session.termDays())
                .displayOnlyValue()
                .create(controlX + controlWidth + controlGap, controlY, controlWidth, 20, Component.empty(),
                        (button, value) -> session.termDays(value));
        termButton.active = session.termSelectable();
        addRenderableWidget(termButton);

        addRenderableWidget(CycleButton.<String>builder(region ->
                        Component.translatable(LangKeys.ORDER_REGION, OrderSession.regionLabel(region)))
                .withValues(OrderSession.REGIONS)
                .withInitialValue(session.region())
                .displayOnlyValue()
                .create(controlX + (controlWidth + controlGap) * 2, controlY, controlWidth, 20, Component.empty(),
                        (button, value) -> session.region(value)));

        addRenderableWidget(new Button(this.width / 2 - 150, this.height - 28, 140, 20,
                Component.translatable(LangKeys.ORDER_BACK), button -> onClose()));

        addRenderableWidget(new Button(this.width / 2 + 10, this.height - 28, 140, 20,
                Component.translatable(LangKeys.CONFIG_TITLE),
                button -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new ConfigScreen(this));
                    }
                }));

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
            Button orderButton = new Button(buttonX, y, ORDER_BUTTON_WIDTH, 20,
                    Component.translatable(LangKeys.ORDER_ORDER_NOW),
                    button -> {
                        session.trackOrderClick();
                        openUri(session.orderUrl(plan, uiLang()));
                    });
            orderButton.active = session.orderable(plan);
            addRenderableWidget(orderButton);
            y += ROW_HEIGHT;
        }
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        super.render(poseStack, mouseX, mouseY, partialTick);
        drawCenteredString(poseStack, this.font, this.title, this.width / 2, 10, 0xFFFFFF);

        ModpackInfo modpackInfo = session.modpackInfo();
        if (modpackSlug != null) {
            String displayName = modpackInfo != null && modpackInfo.getRecommendedRamMb() != null
                    ? modpackInfo.getName()
                    : modpackSlug;
            drawCenteredString(poseStack, this.font,
                    Component.translatable(LangKeys.ORDER_MODPACK_DETECTED, displayName),
                    this.width / 2, 52, 0x55FF55);
        }

        if (session.isOffline()) {
            drawCenteredString(poseStack, this.font,
                    Component.translatable(LangKeys.ORDER_OFFLINE),
                    this.width / 2, modpackSlug != null ? 64 : 52, 0xFFAA00);
        }

        List<ServerPlan> current = session.plans();
        if (current == null) {
            drawCenteredString(poseStack, this.font,
                    Component.translatable(LangKeys.ORDER_LOADING),
                    this.width / 2, this.height / 2 - 4, 0xAAAAAA);
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
            drawString(poseStack, this.font, nameLine, LIST_LEFT_MARGIN, y, 0xFFFFFF);

            MutableComponent details = Component.empty();
            if (plan.slotsOrDefault() > 0) {
                details.append(Component.translatable(LangKeys.ORDER_SLOTS, plan.slotsOrDefault())).append("   ");
            }
            for (OrderSession.Line line : session.priceLines(plan, uiLang())) {
                details.append(Component.translatable(line.key, line.args)).append("   ");
            }
            drawString(poseStack, this.font, details, LIST_LEFT_MARGIN, y + 12, 0xAAAAAA);
            y += ROW_HEIGHT;
        }

        drawCenteredString(poseStack, this.font,
                Component.translatable(LangKeys.ORDER_CHECKOUT_HINT),
                this.width / 2, this.height - 44, 0x777777);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }
}
