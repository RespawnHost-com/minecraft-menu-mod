package com.respawnhost.integration.forge.ui;

import com.respawnhost.core.LangKeys;
import com.respawnhost.integration.forge.config.RespawnConfig;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TranslationTextComponent;

public class ConfigScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget partnerIdBox;
    private TextFieldWidget packIdBox;
    private boolean showOrderButton;

    public ConfigScreen(Screen parent) {
        super(new TranslationTextComponent(LangKeys.CONFIG_TITLE));
        this.parent = parent;
        this.showOrderButton = RespawnConfig.get().isShowOrderButton();
    }

    private String toggleLabel() {
        return I18n.get(LangKeys.CONFIG_SHOW_BUTTON) + ": "
                + I18n.get(showOrderButton ? "options.on" : "options.off");
    }

    @Override
    protected void init() {
        RespawnConfig config = RespawnConfig.get();
        int centerX = this.width / 2;

        partnerIdBox = new TextFieldWidget(this.font, centerX - 100, 60, 200, 20,
                I18n.get(LangKeys.CONFIG_PARTNER_ID));
        partnerIdBox.setMaxLength(128);
        partnerIdBox.setValue(config.getPartnerId());
        this.addButton(partnerIdBox);

        packIdBox = new TextFieldWidget(this.font, centerX - 100, 110, 200, 20,
                I18n.get(LangKeys.CONFIG_PACK_ID));
        packIdBox.setMaxLength(128);
        packIdBox.setValue(config.getPackId());
        this.addButton(packIdBox);

        this.addButton(new Button(centerX - 100, 150, 200, 20, toggleLabel(), button -> {
            this.showOrderButton = !this.showOrderButton;
            button.setMessage(toggleLabel());
        }));

        this.addButton(new Button(centerX - 100, this.height - 28, 200, 20,
                I18n.get(LangKeys.CONFIG_SAVE), button -> {
            RespawnConfig cfg = RespawnConfig.get();
            cfg.setPartnerId(partnerIdBox.getValue().trim());
            cfg.setPackId(packIdBox.getValue().trim());
            cfg.setShowOrderButton(showOrderButton);
            cfg.save();
            onClose();
        }));
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        this.renderBackground();
        super.render(mouseX, mouseY, partialTicks);
        drawCenteredString(this.font, I18n.get(LangKeys.CONFIG_TITLE), this.width / 2, 10, 0xFFFFFF);
        drawString(this.font,
                I18n.get(LangKeys.CONFIG_PARTNER_ID),
                this.width / 2 - 100, 48, 0xAAAAAA);
        drawString(this.font,
                I18n.get(LangKeys.CONFIG_PACK_ID),
                this.width / 2 - 100, 98, 0xAAAAAA);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }
}
