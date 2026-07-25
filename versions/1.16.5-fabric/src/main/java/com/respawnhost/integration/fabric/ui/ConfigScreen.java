package com.respawnhost.integration.fabric.ui;

import com.respawnhost.core.LangKeys;
import com.respawnhost.integration.fabric.config.RespawnConfig;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.TranslatableText;

public class ConfigScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget creatorCodeBox;
    private TextFieldWidget packIdBox;
    private boolean showOrderButton;

    public ConfigScreen(Screen parent) {
        super(new TranslatableText(LangKeys.CONFIG_TITLE));
        this.parent = parent;
        this.showOrderButton = RespawnConfig.get().isShowOrderButton();
    }

    private MutableText toggleLabel() {
        return new TranslatableText(LangKeys.CONFIG_SHOW_BUTTON)
                .append(": ")
                .append(new TranslatableText(showOrderButton ? "options.on" : "options.off"));
    }

    @Override
    protected void init() {
        RespawnConfig config = RespawnConfig.get();
        int centerX = this.width / 2;

        creatorCodeBox = new TextFieldWidget(this.textRenderer, centerX - 100, 60, 200, 20,
                new TranslatableText(LangKeys.CONFIG_CREATOR_CODE));
        creatorCodeBox.setMaxLength(128);
        creatorCodeBox.setText(config.creatorCode());
        this.addButton(creatorCodeBox);

        packIdBox = new TextFieldWidget(this.textRenderer, centerX - 100, 110, 200, 20,
                new TranslatableText(LangKeys.CONFIG_PACK_ID));
        packIdBox.setMaxLength(128);
        packIdBox.setText(config.getPackId());
        this.addButton(packIdBox);

        this.addButton(new ButtonWidget(centerX - 100, 150, 200, 20, toggleLabel(), button -> {
            this.showOrderButton = !this.showOrderButton;
            button.setMessage(toggleLabel());
        }));

        this.addButton(new ButtonWidget(centerX - 100, this.height - 28, 200, 20,
                new TranslatableText(LangKeys.CONFIG_SAVE), button -> {
            RespawnConfig cfg = RespawnConfig.get();
            cfg.creatorCode(creatorCodeBox.getText().trim());
            cfg.setPackId(packIdBox.getText().trim());
            cfg.setShowOrderButton(showOrderButton);
            cfg.save();
            onClose();
        }));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);
        DrawableHelper.drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 10, 0xFFFFFF);
        DrawableHelper.drawTextWithShadow(matrices, this.textRenderer,
                new TranslatableText(LangKeys.CONFIG_CREATOR_CODE),
                this.width / 2 - 100, 48, 0xAAAAAA);
        DrawableHelper.drawTextWithShadow(matrices, this.textRenderer,
                new TranslatableText(LangKeys.CONFIG_PACK_ID),
                this.width / 2 - 100, 98, 0xAAAAAA);
    }

    @Override
    public void onClose() {
        if (this.client != null) {
            this.client.openScreen(parent);
        }
    }
}
