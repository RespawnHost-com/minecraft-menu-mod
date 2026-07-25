package com.respawnhost.integration.ui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.respawnhost.core.LangKeys;
import com.respawnhost.integration.config.RespawnConfig;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

public class ConfigScreen extends Screen {
    private final Screen parent;
    private EditBox creatorCodeBox;
    private EditBox packIdBox;
    private boolean showOrderButton;

    public ConfigScreen(Screen parent) {
        super(new TranslatableComponent(LangKeys.CONFIG_TITLE));
        this.parent = parent;
    }

    private Component toggleLabel() {
        return new TranslatableComponent(LangKeys.CONFIG_SHOW_BUTTON).append(": ")
                .append(new TranslatableComponent(showOrderButton ? "options.on" : "options.off"));
    }

    @Override
    protected void init() {
        RespawnConfig config = RespawnConfig.get();
        this.showOrderButton = config.showOrderButton();
        int centerX = this.width / 2;

        creatorCodeBox = new EditBox(this.font, centerX - 100, 60, 200, 20,
                new TranslatableComponent(LangKeys.CONFIG_CREATOR_CODE));
        creatorCodeBox.setMaxLength(128);
        creatorCodeBox.setValue(config.creatorCode());
        addRenderableWidget(creatorCodeBox);

        packIdBox = new EditBox(this.font, centerX - 100, 110, 200, 20,
                new TranslatableComponent(LangKeys.CONFIG_PACK_ID));
        packIdBox.setMaxLength(128);
        packIdBox.setValue(config.packId());
        addRenderableWidget(packIdBox);

        addRenderableWidget(new Button(centerX - 100, 150, 200, 20, toggleLabel(), button -> {
            this.showOrderButton = !this.showOrderButton;
            button.setMessage(toggleLabel());
        }));

        addRenderableWidget(new Button(centerX - 100, this.height - 28, 200, 20,
                new TranslatableComponent(LangKeys.CONFIG_SAVE), button -> {
            RespawnConfig cfg = RespawnConfig.get();
            cfg.creatorCode(creatorCodeBox.getValue().trim());
            cfg.packId(packIdBox.getValue().trim());
            cfg.showOrderButton(this.showOrderButton);
            cfg.save();
            onClose();
        }));
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        super.render(poseStack, mouseX, mouseY, partialTick);
        drawCenteredString(poseStack, this.font, this.title, this.width / 2, 10, 0xFFFFFF);
        drawString(poseStack, this.font,
                new TranslatableComponent(LangKeys.CONFIG_CREATOR_CODE),
                this.width / 2 - 100, 48, 0xAAAAAA);
        drawString(poseStack, this.font,
                new TranslatableComponent(LangKeys.CONFIG_PACK_ID),
                this.width / 2 - 100, 98, 0xAAAAAA);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }
}
