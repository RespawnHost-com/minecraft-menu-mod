package com.respawnhost.integration.ui;

import com.respawnhost.core.LangKeys;
import com.respawnhost.core.api.FallbackPlans;
import com.respawnhost.core.api.RespawnApiClient;
import com.respawnhost.core.model.FixedTerm;
import com.respawnhost.core.model.ModpackInfo;
import com.respawnhost.core.model.ServerPlan;
import com.respawnhost.core.recommend.PlanRecommender;
import com.respawnhost.integration.RespawnHostIntegrationFabric;
import com.respawnhost.integration.config.RespawnConfig;
import com.respawnhost.integration.modpack.ModpackDetector;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.SystemUtil;

import java.awt.Desktop;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

public class OrderScreen extends Screen {
    private static final int ROW_HEIGHT = 30;
    private static final int LIST_LEFT_MARGIN = 40;
    private static final int ORDER_BUTTON_WIDTH = 110;
    private static final List<Integer> TERM_OPTIONS = Arrays.asList(30, 90, 180, 360);
    private static final List<String> REGION_OPTIONS = Arrays.asList("eu", "us");

    private final Screen parent;
    private final RespawnApiClient apiClient;
    private final String modpackSlug;
    private final AtomicBoolean pendingRefresh = new AtomicBoolean();
    private volatile List<ServerPlan> plans;
    private volatile ModpackInfo modpackInfo;
    private volatile boolean offline;
    private boolean hourlySelected;
    private int termDaysSelected = 30;
    private String regionSelected;
    private ServerPlan recommended;
    private int listTop;

    public OrderScreen(Screen parent) {
        super(new TranslatableText(LangKeys.ORDER_TITLE));
        this.parent = parent;
        RespawnConfig config = RespawnConfig.get();
        this.apiClient = new RespawnApiClient(config.apiBaseUrl(), config.gameShort(), config.panelBaseUrl());
        this.modpackSlug = ModpackDetector.detectModpackName();
        String configRegion = config.region();
        this.regionSelected = configRegion != null && REGION_OPTIONS.contains(configRegion.toLowerCase(Locale.ROOT))
                ? configRegion.toLowerCase(Locale.ROOT)
                : "eu";

        CompletableFuture<List<ServerPlan>> plansFuture = apiClient.fetchPlans();
        CompletableFuture<ModpackInfo> infoFuture = modpackSlug != null ? apiClient.fetchModpackInfo(modpackSlug) : null;
        CompletableFuture<Void> done = infoFuture != null
                ? CompletableFuture.allOf(plansFuture, infoFuture)
                : plansFuture.thenAccept(ignored -> {
                });
        done.thenRun(() -> {
            this.plans = plansFuture.join();
            this.modpackInfo = infoFuture != null ? infoFuture.join() : null;
            this.offline = isFallbackContent(this.plans);
            pendingRefresh.set(true);
        });
    }

    private static boolean isFallbackContent(List<ServerPlan> list) {
        List<ServerPlan> fallback = FallbackPlans.get();
        if (list.size() != fallback.size()) {
            return false;
        }
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId() != fallback.get(i).getId()) {
                return false;
            }
        }
        return true;
    }

    private static String uiLang() {
        try {
            return MinecraftClient.getInstance().getLanguageManager().getLanguage().getCode().startsWith("de") ? "de" : "en";
        } catch (RuntimeException e) {
            return "en";
        }
    }

    private void rebuildContent() {
        this.buttons.clear();
        this.children.clear();
        init();
    }

    private <T> ButtonWidget addCycleButton(int x, int y, int width, List<T> values, T current,
                                            Function<T, String> label, Consumer<T> onChange) {
        ButtonWidget[] self = new ButtonWidget[1];
        int[] index = {Math.max(0, values.indexOf(current))};
        ButtonWidget button = new ButtonWidget(x, y, width, 20, label.apply(values.get(index[0])), pressed -> {
            index[0] = (index[0] + 1) % values.size();
            T value = values.get(index[0]);
            self[0].setMessage(label.apply(value));
            onChange.accept(value);
        });
        self[0] = button;
        return addButton(button);
    }

    @Override
    protected void init() {
        boolean showModpackLine = modpackSlug != null;
        listTop = showModpackLine ? 78 : 66;

        int controlY = 26;
        int controlWidth = 100;
        int controlGap = 8;
        int controlsTotal = controlWidth * 3 + controlGap * 2;
        int controlX = this.width / 2 - controlsTotal / 2;

        addCycleButton(controlX, controlY, controlWidth, Arrays.asList(Boolean.FALSE, Boolean.TRUE), hourlySelected,
                hourly -> I18n.translate(hourly ? LangKeys.ORDER_MODEL_HOURLY : LangKeys.ORDER_MODEL_FIXED),
                value -> {
                    this.hourlySelected = value;
                    rebuildContent();
                });

        ButtonWidget termButton = addCycleButton(controlX + controlWidth + controlGap, controlY, controlWidth,
                TERM_OPTIONS, termDaysSelected,
                days -> I18n.translate(LangKeys.ORDER_TERM_DAYS, days),
                value -> this.termDaysSelected = value);
        termButton.active = !hourlySelected;

        addCycleButton(controlX + (controlWidth + controlGap) * 2, controlY, controlWidth,
                REGION_OPTIONS, regionSelected,
                region -> I18n.translate(LangKeys.ORDER_REGION, region.toUpperCase(Locale.ROOT)),
                value -> this.regionSelected = value);

        addButton(new ButtonWidget(this.width / 2 - 100, this.height - 28, 200, 20,
                I18n.translate(LangKeys.ORDER_BACK), button -> onClose()));

        addButton(new ButtonWidget(4, this.height - 28, ORDER_BUTTON_WIDTH, 20,
                I18n.translate(LangKeys.CONFIG_TITLE),
                button -> {
                    if (this.minecraft != null) {
                        this.minecraft.openScreen(new ConfigScreen(this));
                    }
                }));

        List<ServerPlan> current = plans;
        if (current == null || current.isEmpty()) {
            return;
        }
        recommended = PlanRecommender.recommend(current,
                modpackInfo != null ? modpackInfo.getRecommendedRamMb() : null,
                RespawnHostIntegrationFabric.loadedModCount());
        int y = listTop;
        int buttonX = Math.max(this.width - ORDER_BUTTON_WIDTH - LIST_LEFT_MARGIN, this.width / 2 + 40);
        for (ServerPlan plan : current) {
            if (y + ROW_HEIGHT > this.height - 52) {
                break;
            }
            ButtonWidget orderButton = new ButtonWidget(buttonX, y, ORDER_BUTTON_WIDTH, 20,
                    I18n.translate(LangKeys.ORDER_ORDER_NOW),
                    button -> {
                        apiClient.trackCreatorCode(RespawnConfig.get().creatorCode());
                        openUri(apiClient.buildOrderUrl(plan, hourlySelected, termDaysSelected, regionSelected, uiLang()));
                    });
            orderButton.active = hourlySelected ? plan.isAvailableHourly() : plan.isAvailableFixed();
            addButton(orderButton);
            y += ROW_HEIGHT;
        }
    }

    private static void openUri(String uri) {
        try {
            SystemUtil.getOperatingSystem().open(uri);
        } catch (RuntimeException e) {
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(new URI(uri));
                }
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void render(int mouseX, int mouseY, float delta) {
        if (pendingRefresh.compareAndSet(true, false)) {
            rebuildContent();
        }
        super.render(mouseX, mouseY, delta);
        drawCenteredString(this.font, I18n.translate(LangKeys.ORDER_TITLE), this.width / 2, 10, 0xFFFFFF);

        if (modpackSlug != null) {
            String displayName = modpackInfo != null && modpackInfo.getRecommendedRamMb() != null
                    ? modpackInfo.getName()
                    : modpackSlug;
            drawCenteredString(this.font,
                    I18n.translate(LangKeys.ORDER_MODPACK_DETECTED, displayName),
                    this.width / 2, 52, 0x55FF55);
        }

        if (offline) {
            drawCenteredString(this.font,
                    I18n.translate(LangKeys.ORDER_OFFLINE),
                    this.width / 2, modpackSlug != null ? 64 : 52, 0xFFAA00);
        }

        List<ServerPlan> current = plans;
        if (current == null) {
            drawCenteredString(this.font,
                    I18n.translate(LangKeys.ORDER_LOADING),
                    this.width / 2, this.height / 2 - 4, 0xAAAAAA);
            return;
        }

        int y = listTop;
        for (ServerPlan plan : current) {
            if (y + ROW_HEIGHT > this.height - 52) {
                break;
            }
            String nameLine = plan == recommended
                    ? plan.getName() + "  " + I18n.translate(LangKeys.ORDER_RECOMMENDED)
                    : plan.getName();
            drawString(this.font, nameLine, LIST_LEFT_MARGIN, y, 0xFFFFFF);

            StringBuilder details = new StringBuilder();
            details.append(I18n.translate(LangKeys.ORDER_RAM, plan.ramDisplay()));
            if (plan.slotsOrDefault() > 0) {
                details.append("   ").append(I18n.translate(LangKeys.ORDER_SLOTS, plan.slotsOrDefault()));
            }
            if (hourlySelected) {
                details.append("   ").append(I18n.translate(LangKeys.ORDER_PRICE_HOURLY, plan.getPriceHourly(), "EUR"));
            } else {
                details.append("   ").append(I18n.translate(LangKeys.ORDER_PRICE, plan.getPriceMonthly(), "EUR"));
            }
            if (!hourlySelected && plan.getFixedTerms() != null) {
                for (FixedTerm term : plan.getFixedTerms()) {
                    if (term.getTermDays() == termDaysSelected && term.getDiscountPercent() > 0) {
                        details.append("   ")
                                .append(I18n.translate(LangKeys.ORDER_EFFECTIVE_MONTHLY, term.getEffectiveMonthly(), "EUR"));
                        break;
                    }
                }
            }
            drawString(this.font, details.toString(), LIST_LEFT_MARGIN, y + 12, 0xAAAAAA);
            y += ROW_HEIGHT;
        }

        drawCenteredString(this.font,
                I18n.translate(LangKeys.ORDER_CHECKOUT_HINT),
                this.width / 2, this.height - 44, 0x777777);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.openScreen(parent);
        }
    }
}
