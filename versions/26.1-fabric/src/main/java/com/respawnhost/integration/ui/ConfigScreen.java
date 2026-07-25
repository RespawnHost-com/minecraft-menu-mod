package com.respawnhost.integration.ui;

import com.respawnhost.core.LangKeys;
import com.respawnhost.integration.config.RespawnConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ConfigScreen extends Screen {
    private final Screen parent;
    private EditBox creatorCodeBox;
    private EditBox packIdBox;
    private CycleButton<Boolean> showButtonToggle;

    public ConfigScreen(Screen parent) {
        super(Component.translatable(LangKeys.CONFIG_TITLE));
        this.parent = parent;
    }

    @Override
    protected void init() {
        RespawnConfig config = RespawnConfig.get();
        int centerX = this.width / 2;

        creatorCodeBox = new EditBox(this.font, centerX - 100, 60, 200, 20,
                Component.translatable(LangKeys.CONFIG_CREATOR_CODE));
        creatorCodeBox.setMaxLength(128);
        creatorCodeBox.setValue(config.creatorCode());
        addRenderableWidget(creatorCodeBox);

        packIdBox = new EditBox(this.font, centerX - 100, 110, 200, 20,
                Component.translatable(LangKeys.CONFIG_PACK_ID));
        packIdBox.setMaxLength(128);
        packIdBox.setValue(config.packId());
        addRenderableWidget(packIdBox);

        showButtonToggle = CycleButton.onOffBuilder(config.showOrderButton())
                .create(centerX - 100, 150, 200, 20,
                        Component.translatable(LangKeys.CONFIG_SHOW_BUTTON),
                        (button, value) -> {
                        });
        addRenderableWidget(showButtonToggle);

        addRenderableWidget(Button.builder(Component.translatable(LangKeys.CONFIG_SAVE), button -> {
                    RespawnConfig cfg = RespawnConfig.get();
                    cfg.creatorCode(creatorCodeBox.getValue().trim());
                    cfg.packId(packIdBox.getValue().trim());
                    cfg.showOrderButton(showButtonToggle.getValue());
                    cfg.save();
                    onClose();
                })
                .bounds(centerX - 100, this.height - 28, 200, 20)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(extractor, mouseX, mouseY, partialTick);
        extractor.centeredText(this.font, this.title, this.width / 2, 10, 0xFFFFFF);
        extractor.text(this.font,
                Component.translatable(LangKeys.CONFIG_CREATOR_CODE),
                this.width / 2 - 100, 48, 0xAAAAAA);
        extractor.text(this.font,
                Component.translatable(LangKeys.CONFIG_PACK_ID),
                this.width / 2 - 100, 98, 0xAAAAAA);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }
}
