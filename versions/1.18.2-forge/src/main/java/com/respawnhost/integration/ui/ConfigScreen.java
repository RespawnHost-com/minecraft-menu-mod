package com.respawnhost.integration.ui;

import com.respawnhost.core.LangKeys;
import com.respawnhost.integration.config.RespawnConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TranslatableComponent;

public class ConfigScreen extends Screen {
    private final Screen parent;
    private EditBox creatorCodeBox;
    private EditBox packIdBox;
    private boolean showButtonValue;

    public ConfigScreen(Screen parent) {
        super(new TranslatableComponent(LangKeys.CONFIG_TITLE));
        this.parent = parent;
    }

    @Override
    protected void init() {
        RespawnConfig config = RespawnConfig.get();
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

        showButtonValue = config.showOrderButton();
        Button[] self = new Button[1];
        Button toggle = new Button(centerX - 100, 150, 200, 20, toggleLabel(showButtonValue), button -> {
            showButtonValue = !showButtonValue;
            self[0].setMessage(toggleLabel(showButtonValue));
        });
        self[0] = toggle;
        addRenderableWidget(toggle);

        addRenderableWidget(new Button(centerX - 100, this.height - 28, 200, 20,
                new TranslatableComponent(LangKeys.CONFIG_SAVE), button -> {
            RespawnConfig cfg = RespawnConfig.get();
            cfg.creatorCode(creatorCodeBox.getValue().trim());
            cfg.packId(packIdBox.getValue().trim());
            cfg.showOrderButton(showButtonValue);
            cfg.save();
            onClose();
        }));
    }

    private static MutableComponent toggleLabel(boolean value) {
        return new TranslatableComponent(LangKeys.CONFIG_SHOW_BUTTON)
                .append(": ")
                .append(value ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF);
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
