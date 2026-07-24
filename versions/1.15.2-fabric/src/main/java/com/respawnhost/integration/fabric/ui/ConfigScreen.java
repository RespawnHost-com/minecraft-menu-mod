package com.respawnhost.integration.fabric.ui;

import com.respawnhost.core.LangKeys;
import com.respawnhost.integration.fabric.config.RespawnConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.TranslatableText;

public class ConfigScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget partnerIdBox;
    private TextFieldWidget packIdBox;
    private boolean showOrderButton;

    public ConfigScreen(Screen parent) {
        super(new TranslatableText(LangKeys.CONFIG_TITLE));
        this.parent = parent;
        this.showOrderButton = RespawnConfig.get().isShowOrderButton();
    }

    private static String tr(String key) {
        return new TranslatableText(key).asFormattedString();
    }

    private String toggleLabel() {
        return tr(LangKeys.CONFIG_SHOW_BUTTON) + ": "
                + tr(showOrderButton ? "options.on" : "options.off");
    }

    @Override
    protected void init() {
        RespawnConfig config = RespawnConfig.get();
        int centerX = this.width / 2;

        partnerIdBox = new TextFieldWidget(MinecraftClient.getInstance().textRenderer, centerX - 100, 60, 200, 20,
                tr(LangKeys.CONFIG_PARTNER_ID));
        partnerIdBox.setMaxLength(128);
        partnerIdBox.setText(config.getPartnerId());
        this.addButton(partnerIdBox);

        packIdBox = new TextFieldWidget(MinecraftClient.getInstance().textRenderer, centerX - 100, 110, 200, 20,
                tr(LangKeys.CONFIG_PACK_ID));
        packIdBox.setMaxLength(128);
        packIdBox.setText(config.getPackId());
        this.addButton(packIdBox);

        this.addButton(new ButtonWidget(centerX - 100, 150, 200, 20, toggleLabel(), button -> {
            this.showOrderButton = !this.showOrderButton;
            button.setMessage(toggleLabel());
        }));

        this.addButton(new ButtonWidget(centerX - 100, this.height - 28, 200, 20,
                tr(LangKeys.CONFIG_SAVE), button -> {
            RespawnConfig cfg = RespawnConfig.get();
            cfg.setPartnerId(partnerIdBox.getText().trim());
            cfg.setPackId(packIdBox.getText().trim());
            cfg.setShowOrderButton(showOrderButton);
            cfg.save();
            onClose();
        }));
    }

    @Override
    public void render(int mouseX, int mouseY, float delta) {
        this.renderBackground();
        super.render(mouseX, mouseY, delta);
        this.drawCenteredString(MinecraftClient.getInstance().textRenderer, tr(LangKeys.CONFIG_TITLE),
                this.width / 2, 10, 0xFFFFFF);
        this.drawString(MinecraftClient.getInstance().textRenderer,
                tr(LangKeys.CONFIG_PARTNER_ID),
                this.width / 2 - 100, 48, 0xAAAAAA);
        this.drawString(MinecraftClient.getInstance().textRenderer,
                tr(LangKeys.CONFIG_PACK_ID),
                this.width / 2 - 100, 98, 0xAAAAAA);
    }

    @Override
    public void onClose() {
        MinecraftClient.getInstance().openScreen(parent);
    }
}
