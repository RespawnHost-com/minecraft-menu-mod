package com.respawnhost.integration.ui;

import com.respawnhost.core.LangKeys;
import com.respawnhost.integration.config.RespawnConfig;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TranslationTextComponent;

public class ConfigScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget creatorCodeBox;
    private TextFieldWidget packIdBox;
    private boolean showButtonValue;

    public ConfigScreen(Screen parent) {
        super(new TranslationTextComponent(LangKeys.CONFIG_TITLE));
        this.parent = parent;
    }

    @Override
    protected void init() {
        RespawnConfig config = RespawnConfig.get();
        int centerX = this.width / 2;

        creatorCodeBox = new TextFieldWidget(this.font, centerX - 100, 60, 200, 20,
                I18n.get(LangKeys.CONFIG_CREATOR_CODE));
        creatorCodeBox.setMaxLength(128);
        creatorCodeBox.setValue(config.creatorCode());
        this.children.add(creatorCodeBox);

        packIdBox = new TextFieldWidget(this.font, centerX - 100, 110, 200, 20,
                I18n.get(LangKeys.CONFIG_PACK_ID));
        packIdBox.setMaxLength(128);
        packIdBox.setValue(config.packId());
        this.children.add(packIdBox);

        showButtonValue = config.showOrderButton();
        Button[] self = new Button[1];
        Button toggle = new Button(centerX - 100, 150, 200, 20, toggleLabel(showButtonValue), button -> {
            showButtonValue = !showButtonValue;
            self[0].setMessage(toggleLabel(showButtonValue));
        });
        self[0] = toggle;
        addButton(toggle);

        addButton(new Button(centerX - 100, this.height - 28, 200, 20,
                I18n.get(LangKeys.CONFIG_SAVE), button -> {
            RespawnConfig cfg = RespawnConfig.get();
            cfg.creatorCode(creatorCodeBox.getValue().trim());
            cfg.packId(packIdBox.getValue().trim());
            cfg.showOrderButton(showButtonValue);
            cfg.save();
            onClose();
        }));
    }

    private static String toggleLabel(boolean value) {
        return I18n.get(LangKeys.CONFIG_SHOW_BUTTON)
                + ": "
                + I18n.get(value ? "options.on" : "options.off");
    }

    @Override
    public void tick() {
        creatorCodeBox.tick();
        packIdBox.tick();
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTick) {
        super.render(mouseX, mouseY, partialTick);
        drawCenteredString(this.font, I18n.get(LangKeys.CONFIG_TITLE), this.width / 2, 10, 0xFFFFFF);
        drawString(this.font,
                I18n.get(LangKeys.CONFIG_CREATOR_CODE),
                this.width / 2 - 100, 48, 0xAAAAAA);
        drawString(this.font,
                I18n.get(LangKeys.CONFIG_PACK_ID),
                this.width / 2 - 100, 98, 0xAAAAAA);
        creatorCodeBox.render(mouseX, mouseY, partialTick);
        packIdBox.render(mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }
}
