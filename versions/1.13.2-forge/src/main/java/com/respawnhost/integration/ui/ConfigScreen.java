package com.respawnhost.integration.ui;

import com.respawnhost.core.LangKeys;
import com.respawnhost.integration.config.RespawnConfig;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;

public class ConfigScreen extends GuiScreen {
    private static final int ID_SHOW = 1;
    private static final int ID_SAVE = 2;
    private static final int ID_BACK = 3;

    private final GuiScreen parent;
    private GuiTextField creatorCodeBox;
    private GuiTextField packIdBox;
    private boolean showButton;

    public ConfigScreen(GuiScreen parent) {
        this.parent = parent;
        this.showButton = RespawnConfig.get().showOrderButton();
    }

    @Override
    protected void initGui() {
        int centerX = this.width / 2;

        creatorCodeBox = new GuiTextField(0, this.fontRenderer, centerX - 100, 60, 200, 20);
        creatorCodeBox.setMaxStringLength(128);
        creatorCodeBox.setText(RespawnConfig.get().creatorCode());
        this.children.add(creatorCodeBox);

        packIdBox = new GuiTextField(1, this.fontRenderer, centerX - 100, 110, 200, 20);
        packIdBox.setMaxStringLength(128);
        packIdBox.setText(RespawnConfig.get().packId());
        this.children.add(packIdBox);

        final RunnableButton showButtonToggle = new RunnableButton(ID_SHOW, centerX - 100, 150, 200, 20,
                showButtonLabel(), () -> {
        });
        showButtonToggle.setAction(() -> {
            showButton = !showButton;
            showButtonToggle.setMessage(showButtonLabel());
        });
        addButton(showButtonToggle);

        addButton(new RunnableButton(ID_SAVE, centerX - 100, this.height - 28, 98, 20,
                I18n.format(LangKeys.CONFIG_SAVE), () -> {
            RespawnConfig config = RespawnConfig.get();
            config.creatorCode(creatorCodeBox.getText().trim());
            config.packId(packIdBox.getText().trim());
            config.showOrderButton(showButton);
            config.save();
            this.mc.displayGuiScreen(parent);
        }));

        addButton(new RunnableButton(ID_BACK, centerX + 2, this.height - 28, 98, 20,
                I18n.format(LangKeys.ORDER_BACK),
                () -> this.mc.displayGuiScreen(parent)));
    }

    private String showButtonLabel() {
        return I18n.format(LangKeys.CONFIG_SHOW_BUTTON) + ": "
                + I18n.format(showButton ? "gui.yes" : "gui.no");
    }

    @Override
    public void tick() {
        creatorCodeBox.tick();
        packIdBox.tick();
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.render(mouseX, mouseY, partialTicks);
        drawCenteredString(this.fontRenderer, I18n.format(LangKeys.CONFIG_TITLE), this.width / 2, 10, 0xFFFFFF);
        drawString(this.fontRenderer, I18n.format(LangKeys.CONFIG_CREATOR_CODE),
                this.width / 2 - 100, 48, 0xAAAAAA);
        drawString(this.fontRenderer, I18n.format(LangKeys.CONFIG_PACK_ID),
                this.width / 2 - 100, 98, 0xAAAAAA);
        creatorCodeBox.drawTextField(mouseX, mouseY, partialTicks);
        packIdBox.drawTextField(mouseX, mouseY, partialTicks);
    }

    @Override
    public void close() {
        this.mc.displayGuiScreen(parent);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
