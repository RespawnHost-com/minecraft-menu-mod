package com.respawnhost.integration.ui;

import com.respawnhost.core.LangKeys;
import com.respawnhost.integration.config.RespawnConfig;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.TranslatableText;

public class ConfigScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget creatorCodeBox;
    private TextFieldWidget packIdBox;
    private boolean showButtonValue;

    public ConfigScreen(Screen parent) {
        super(new TranslatableText(LangKeys.CONFIG_TITLE));
        this.parent = parent;
    }

    @Override
    protected void init() {
        RespawnConfig config = RespawnConfig.get();
        int centerX = this.width / 2;

        creatorCodeBox = new TextFieldWidget(this.font, centerX - 100, 60, 200, 20,
                I18n.translate(LangKeys.CONFIG_CREATOR_CODE));
        creatorCodeBox.setMaxLength(128);
        creatorCodeBox.setText(config.creatorCode());
        this.children.add(creatorCodeBox);

        packIdBox = new TextFieldWidget(this.font, centerX - 100, 110, 200, 20,
                I18n.translate(LangKeys.CONFIG_PACK_ID));
        packIdBox.setMaxLength(128);
        packIdBox.setText(config.packId());
        this.children.add(packIdBox);

        showButtonValue = config.showOrderButton();
        ButtonWidget[] self = new ButtonWidget[1];
        ButtonWidget toggle = new ButtonWidget(centerX - 100, 150, 200, 20, toggleLabel(showButtonValue), button -> {
            showButtonValue = !showButtonValue;
            self[0].setMessage(toggleLabel(showButtonValue));
        });
        self[0] = toggle;
        addButton(toggle);

        addButton(new ButtonWidget(centerX - 100, this.height - 28, 200, 20,
                I18n.translate(LangKeys.CONFIG_SAVE), button -> {
            RespawnConfig cfg = RespawnConfig.get();
            cfg.creatorCode(creatorCodeBox.getText().trim());
            cfg.packId(packIdBox.getText().trim());
            cfg.showOrderButton(showButtonValue);
            cfg.save();
            onClose();
        }));
    }

    private static String toggleLabel(boolean value) {
        return I18n.translate(LangKeys.CONFIG_SHOW_BUTTON)
                + ": "
                + I18n.translate(value ? "options.on" : "options.off");
    }

    @Override
    public void tick() {
        creatorCodeBox.tick();
        packIdBox.tick();
    }

    @Override
    public void render(int mouseX, int mouseY, float delta) {
        super.render(mouseX, mouseY, delta);
        drawCenteredString(this.font, I18n.translate(LangKeys.CONFIG_TITLE), this.width / 2, 10, 0xFFFFFF);
        drawString(this.font,
                I18n.translate(LangKeys.CONFIG_CREATOR_CODE),
                this.width / 2 - 100, 48, 0xAAAAAA);
        drawString(this.font,
                I18n.translate(LangKeys.CONFIG_PACK_ID),
                this.width / 2 - 100, 98, 0xAAAAAA);
        creatorCodeBox.render(mouseX, mouseY, delta);
        packIdBox.render(mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.openScreen(parent);
        }
    }
}
