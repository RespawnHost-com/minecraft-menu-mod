package com.respawnhost.integration.ui;

import com.respawnhost.core.LangKeys;
import com.respawnhost.integration.config.RespawnConfig;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;

public class ConfigScreen extends GuiScreen {
    private static final int ID_SHOW = 1;
    private static final int ID_SAVE = 2;
    private static final int ID_BACK = 3;

    private final GuiScreen parent;
    private GuiTextField partnerIdBox;
    private GuiTextField packIdBox;
    private GuiButton showButtonToggle;
    private boolean showButton;

    public ConfigScreen(GuiScreen parent) {
        this.parent = parent;
        this.showButton = RespawnConfig.get().showOrderButton();
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        int centerX = this.width / 2;

        partnerIdBox = new GuiTextField(0, this.fontRenderer, centerX - 100, 60, 200, 20);
        partnerIdBox.setMaxStringLength(128);
        partnerIdBox.setText(RespawnConfig.get().partnerId());

        packIdBox = new GuiTextField(1, this.fontRenderer, centerX - 100, 110, 200, 20);
        packIdBox.setMaxStringLength(128);
        packIdBox.setText(RespawnConfig.get().packId());

        showButtonToggle = new GuiButton(ID_SHOW, centerX - 100, 150, 200, 20, showButtonLabel());
        this.buttonList.add(showButtonToggle);

        this.buttonList.add(new GuiButton(ID_SAVE, centerX - 100, this.height - 28, 98, 20,
                I18n.format(LangKeys.CONFIG_SAVE)));
        this.buttonList.add(new GuiButton(ID_BACK, centerX + 2, this.height - 28, 98, 20,
                I18n.format(LangKeys.ORDER_BACK)));
    }

    private String showButtonLabel() {
        return I18n.format(LangKeys.CONFIG_SHOW_BUTTON) + ": "
                + I18n.format(showButton ? "gui.yes" : "gui.no");
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (!button.enabled) {
            return;
        }
        switch (button.id) {
            case ID_SHOW:
                showButton = !showButton;
                showButtonToggle.displayString = showButtonLabel();
                break;
            case ID_SAVE:
                RespawnConfig config = RespawnConfig.get();
                config.partnerId(partnerIdBox.getText().trim());
                config.packId(packIdBox.getText().trim());
                config.showOrderButton(showButton);
                config.save();
                this.mc.displayGuiScreen(parent);
                break;
            case ID_BACK:
                this.mc.displayGuiScreen(parent);
                break;
            default:
                break;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (partnerIdBox.textboxKeyTyped(typedChar, keyCode)) {
            return;
        }
        if (packIdBox.textboxKeyTyped(typedChar, keyCode)) {
            return;
        }
        try {
            super.keyTyped(typedChar, keyCode);
        } catch (java.io.IOException ignored) {
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        try {
            super.mouseClicked(mouseX, mouseY, mouseButton);
        } catch (java.io.IOException ignored) {
        }
        partnerIdBox.mouseClicked(mouseX, mouseY, mouseButton);
        packIdBox.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void updateScreen() {
        partnerIdBox.updateCursorCounter();
        packIdBox.updateCursorCounter();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(this.fontRenderer, I18n.format(LangKeys.CONFIG_TITLE), this.width / 2, 10, 0xFFFFFF);
        drawString(this.fontRenderer, I18n.format(LangKeys.CONFIG_PARTNER_ID),
                this.width / 2 - 100, 48, 0xAAAAAA);
        drawString(this.fontRenderer, I18n.format(LangKeys.CONFIG_PACK_ID),
                this.width / 2 - 100, 98, 0xAAAAAA);
        partnerIdBox.drawTextBox();
        packIdBox.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
