package com.respawnhost.core.order;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.respawnhost.core.model.CurrencyInfo;
import com.respawnhost.core.model.ServerPlan;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OrderSessionTest {
    // Live 8 GB plan from GET /api/games/short/minecraft/packages (2026-10-05).
    private static final String PLAN_JSON = "[{\"id\":253,\"name\":\"minecraft-3\",\"memory\":8192,"
            + "\"priceHourly\":0.05749998,\"priceMonthly\":15.2,\"recommendedPlayers\":30,"
            + "\"priceOverrides\":{\"USD\":{\"monthly\":19.49}},\"availableHourly\":true,\"availableFixed\":true,"
            + "\"priceHourlyEco\":0.04024999,\"priceMonthlyEco\":10.64,\"fixedTerms\":["
            + "{\"termDays\":30,\"discountPercent\":0},{\"termDays\":90,\"discountPercent\":10.0},"
            + "{\"termDays\":180,\"discountPercent\":15.0},{\"termDays\":360,\"discountPercent\":20.0}]}]";
    private static final CurrencyInfo USD = new CurrencyInfo("USD", "$", "before", false, 2, 1.1204);

    private final ServerPlan plan = new Gson().<List<ServerPlan>>fromJson(PLAN_JSON,
            new TypeToken<List<ServerPlan>>() { }.getType()).get(0);

    private OrderSession session(CurrencyInfo currency, boolean eco, double creatorDiscount) {
        OrderSession session = new OrderSession("https://x/api", "https://p", "minecraft", "eu", "", "", null);
        session.context(Collections.singletonList(plan), currency, Collections.singletonMap("eu", eco), creatorDiscount);
        return session;
    }

    private static String text(OrderSession.Line line) {
        return line.args[0].toString();
    }

    @Test
    public void subscriptionUsesEcoWhenFree() {
        assertEquals("Minecraft 8 GB", plan.displayName());
        assertEquals("10,64 €", text(session(CurrencyInfo.EUR, true, 0).priceLines(plan, "de").get(0)));
        assertEquals("15,20 €", text(session(CurrencyInfo.EUR, false, 0).priceLines(plan, "de").get(0)));
    }

    @Test
    public void overrideWinsOverExchangeRate() {
        assertEquals("$19.49", text(session(USD, false, 0).priceLines(plan, "en").get(0)));
        // Panel rounds the Eco price in EUR first: 19.49 / 1.1204 * 0.7 = 12.18 EUR.
        assertEquals("$13.65", text(session(USD, true, 0).priceLines(plan, "en").get(0)));
    }

    @Test
    public void fixedTakesLargestDiscountWithoutStacking() {
        OrderSession s = session(CurrencyInfo.EUR, true, 0.2);
        s.model(OrderSession.FIXED);
        s.termDays(90);
        // 10.64 * 3 = 31.92; creator 20 % (6.38) beats term 10 % (3.19).
        assertEquals("25,54 €", text(s.priceLines(plan, "de").get(0)));
        s.termDays(360);
        // 127.68; term 20 % = creator 20 % -> 25.54 off, once.
        assertEquals("102,14 €", text(s.priceLines(plan, "de").get(0)));
    }

    @Test
    public void subscriptionShowsCreatorDiscountForFirstMonth() {
        List<OrderSession.Line> lines = session(CurrencyInfo.EUR, true, 0.2).priceLines(plan, "de");
        assertEquals(2, lines.size());
        assertEquals("8,51 €", text(lines.get(1)));
    }

    @Test
    public void hourlyAndOrderUrl() {
        OrderSession s = session(CurrencyInfo.EUR, true, 0);
        s.model(OrderSession.HOURLY);
        assertEquals("0,0402 €", text(s.priceLines(plan, "de").get(0)));
        String url = s.orderUrl(plan, "de");
        assertEquals("https://p/de/order/minecraft?plan=253&model=hourly&region=eu&performance_tier=eco", url);
        assertTrue(!s.termSelectable());
    }
}
