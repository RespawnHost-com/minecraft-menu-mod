package com.respawnhost.integration.ui;

import com.respawnhost.core.LangKeys;
import com.respawnhost.integration.config.RespawnConfig;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class ConfigScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget creatorCodeBox;
    private TextFieldWidget packIdBox;
    private CyclingButtonWidget<Boolean> showButtonToggle;

    public ConfigScreen(Screen parent) {
        super(Text.translatable(LangKeys.CONFIG_TITLE));
        this.parent = parent;
    }

    @Override
    protected void init() {
        RespawnConfig config = RespawnConfig.get();
        int centerX = this.width / 2;

        creatorCodeBox = new TextFieldWidget(this.textRenderer, centerX - 100, 60, 200, 20,
                Text.translatable(LangKeys.CONFIG_CREATOR_CODE));
        creatorCodeBox.setMaxLength(128);
        creatorCodeBox.setText(config.creatorCode());
        addDrawableChild(creatorCodeBox);

        packIdBox = new TextFieldWidget(this.textRenderer, centerX - 100, 110, 200, 20,
                Text.translatable(LangKeys.CONFIG_PACK_ID));
        packIdBox.setMaxLength(128);
        packIdBox.setText(config.packId());
        addDrawableChild(packIdBox);

        showButtonToggle = CyclingButtonWidget.onOffBuilder(config.showOrderButton())
                .build(centerX - 100, 150, 200, 20,
                        Text.translatable(LangKeys.CONFIG_SHOW_BUTTON),
                        (button, value) -> {
                        });
        addDrawableChild(showButtonToggle);

        addDrawableChild(ButtonWidget.builder(Text.translatable(LangKeys.CONFIG_SAVE), button -> {
            RespawnConfig cfg = RespawnConfig.get();
            cfg.creatorCode(creatorCodeBox.getText().trim());
            cfg.packId(packIdBox.getText().trim());
            cfg.showOrderButton(showButtonToggle.getValue());
            cfg.save();
            close();
        }).dimensions(centerX - 100, this.height - 28, 200, 20).build());
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.render(matrices, mouseX, mouseY, delta);
        drawCenteredTextWithShadow(matrices, this.textRenderer, this.title, this.width / 2, 10, 0xFFFFFF);
        drawTextWithShadow(matrices, this.textRenderer,
                Text.translatable(LangKeys.CONFIG_CREATOR_CODE),
                this.width / 2 - 100, 48, 0xAAAAAA);
        drawTextWithShadow(matrices, this.textRenderer,
                Text.translatable(LangKeys.CONFIG_PACK_ID),
                this.width / 2 - 100, 98, 0xAAAAAA);
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }
}
