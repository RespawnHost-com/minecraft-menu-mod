package com.respawnhost.integration.ui;

import net.minecraft.client.gui.GuiButton;

public class RunnableButton extends GuiButton {
    private Runnable action;

    public RunnableButton(int id, int x, int y, int width, int height, String text, Runnable action) {
        super(id, x, y, width, height, text);
        this.action = action;
    }

    public void setAction(Runnable action) {
        this.action = action;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        action.run();
    }

    public void setMessage(String text) {
        this.displayString = text;
    }
}
