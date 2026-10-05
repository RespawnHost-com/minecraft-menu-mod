package com.respawnhost.integration.ui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.respawnhost.core.LangKeys;
import com.respawnhost.core.model.ModpackInfo;
import com.respawnhost.core.model.ServerPlan;
import com.respawnhost.core.order.OrderSession;
import com.respawnhost.core.recommend.PlanRecommender;
import com.respawnhost.integration.config.RespawnConfig;
import com.respawnhost.integration.modpack.ModpackDetector;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.fml.ModList;

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
        super(new TranslatableComponent(LangKeys.ORDER_TITLE));
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

    private void rebuildWidgets() {
        if (this.minecraft != null) {
            this.init(this.minecraft, this.width, this.height);
        }
    }

    private Component billingLabel() {
        return new TranslatableComponent(OrderSession.modelKey(session.model()));
    }

    private static <T> T next(List<T> values, T current) {
        return values.get((values.indexOf(current) + 1) % values.size());
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

        addRenderableWidget(new Button(controlX, controlY, controlWidth, 20, billingLabel(), button -> {
            session.model(next(OrderSession.MODELS, session.model()));
            rebuildWidgets();
        }));

        Button termButton = new Button(controlX + controlWidth + controlGap, controlY, controlWidth, 20,
                new TranslatableComponent(LangKeys.ORDER_TERM_DAYS, session.termDays()), button -> {
            session.termDays(next(OrderSession.TERMS, session.termDays()));
            rebuildWidgets();
        });
        termButton.active = session.termSelectable();
        addRenderableWidget(termButton);

        addRenderableWidget(new Button(controlX + (controlWidth + controlGap) * 2, controlY, controlWidth, 20,
                new TranslatableComponent(LangKeys.ORDER_REGION, OrderSession.regionLabel(session.region())), button -> {
            session.region(next(OrderSession.REGIONS, session.region()));
            rebuildWidgets();
        }));

        addRenderableWidget(new Button(controlX + (controlWidth + controlGap) * 3, controlY, controlWidth, 20,
                new TextComponent(session.currencyCode()), button -> {
            session.currency(next(session.currencyCodes(), session.currencyCode()));
            button.setMessage(new TextComponent(session.currencyCode()));
        }));

        addRenderableWidget(new Button(this.width / 2 - 152, this.height - 28, 150, 20,
                new TranslatableComponent(LangKeys.ORDER_BACK), button -> onClose()));

        addRenderableWidget(new Button(this.width / 2 + 2, this.height - 28, 150, 20,
                new TranslatableComponent(LangKeys.CONFIG_TITLE),
                button -> this.minecraft.setScreen(new ConfigScreen(this))));

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
                    new TranslatableComponent(LangKeys.ORDER_ORDER_NOW),
                    button -> {
                        session.trackOrderClick();
                        Util.getPlatform().openUri(session.orderUrl(plan, uiLang()));
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
                    new TranslatableComponent(LangKeys.ORDER_MODPACK_DETECTED, displayName),
                    this.width / 2, 52, 0x55FF55);
        }

        if (session.isOffline()) {
            drawCenteredString(poseStack, this.font,
                    new TranslatableComponent(LangKeys.ORDER_OFFLINE),
                    this.width / 2, modpackSlug != null ? 64 : 52, 0xFFAA00);
        }

        List<ServerPlan> current = session.plans();
        if (current == null) {
            drawCenteredString(poseStack, this.font,
                    new TranslatableComponent(LangKeys.ORDER_LOADING),
                    this.width / 2, this.height / 2 - 4, 0xAAAAAA);
            return;
        }

        int y = listTop;
        for (ServerPlan plan : current) {
            if (y + ROW_HEIGHT > this.height - 52) {
                break;
            }
            Component nameLine = plan == recommended
                    ? new TextComponent(plan.displayName()).append("  ")
                            .append(new TranslatableComponent(LangKeys.ORDER_RECOMMENDED))
                    : new TextComponent(plan.displayName());
            drawString(poseStack, this.font, nameLine, LIST_LEFT_MARGIN, y, 0xFFFFFF);

            MutableComponent details = new TextComponent("");
            if (plan.slotsOrDefault() > 0) {
                details.append(new TranslatableComponent(LangKeys.ORDER_SLOTS, plan.slotsOrDefault())).append("   ");
            }
            for (OrderSession.Line line : session.priceLines(plan, uiLang())) {
                details.append(new TranslatableComponent(line.key, line.args)).append("   ");
            }
            drawString(poseStack, this.font, details, LIST_LEFT_MARGIN, y + 12, 0xAAAAAA);
            y += ROW_HEIGHT;
        }

        drawCenteredString(poseStack, this.font,
                new TranslatableComponent(LangKeys.ORDER_CHECKOUT_HINT),
                this.width / 2, this.height - 44, 0x777777);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }
}
