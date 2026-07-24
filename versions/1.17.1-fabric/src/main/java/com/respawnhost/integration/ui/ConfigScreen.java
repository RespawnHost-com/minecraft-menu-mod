package com.respawnhost.integration.ui;

import com.respawnhost.core.LangKeys;
import com.respawnhost.integration.config.RespawnConfig;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

public class ConfigScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget partnerIdBox;
    private TextFieldWidget packIdBox;
    private boolean showOrderButton;

    public ConfigScreen(Screen parent) {
        super(new TranslatableText(LangKeys.CONFIG_TITLE));
        this.parent = parent;
    }

    private Text toggleLabel() {
        return new TranslatableText(LangKeys.CONFIG_SHOW_BUTTON).append(": ")
                .append(new TranslatableText(showOrderButton ? "options.on" : "options.off"));
    }

    @Override
    protected void init() {
        RespawnConfig config = RespawnConfig.get();
        this.showOrderButton = config.showOrderButton();
        int centerX = this.width / 2;

        partnerIdBox = new TextFieldWidget(this.textRenderer, centerX - 100, 60, 200, 20,
                new TranslatableText(LangKeys.CONFIG_PARTNER_ID));
        partnerIdBox.setMaxLength(128);
        partnerIdBox.setText(config.partnerId());
        addDrawableChild(partnerIdBox);

        packIdBox = new TextFieldWidget(this.textRenderer, centerX - 100, 110, 200, 20,
                new TranslatableText(LangKeys.CONFIG_PACK_ID));
        packIdBox.setMaxLength(128);
        packIdBox.setText(config.packId());
        addDrawableChild(packIdBox);

        addDrawableChild(new ButtonWidget(centerX - 100, 150, 200, 20, toggleLabel(), button -> {
            this.showOrderButton = !this.showOrderButton;
            button.setMessage(toggleLabel());
        }));

        addDrawableChild(new ButtonWidget(centerX - 100, this.height - 28, 200, 20,
                new TranslatableText(LangKeys.CONFIG_SAVE), button -> {
            RespawnConfig cfg = RespawnConfig.get();
            cfg.partnerId(partnerIdBox.getText().trim());
            cfg.packId(packIdBox.getText().trim());
            cfg.showOrderButton(this.showOrderButton);
            cfg.save();
            onClose();
        }));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.render(matrices, mouseX, mouseY, delta);
        drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 10, 0xFFFFFF);
        drawTextWithShadow(matrices, this.textRenderer,
                new TranslatableText(LangKeys.CONFIG_PARTNER_ID),
                this.width / 2 - 100, 48, 0xAAAAAA);
        drawTextWithShadow(matrices, this.textRenderer,
                new TranslatableText(LangKeys.CONFIG_PACK_ID),
                this.width / 2 - 100, 98, 0xAAAAAA);
    }

    @Override
    public void onClose() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }
}
