package com.respawnhost.core.api;

import com.respawnhost.core.model.FixedTerm;
import com.respawnhost.core.model.ServerPlan;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FallbackPlans {
    private static final int[] TERM_DAYS = {30, 90, 180, 360};
    private static final int[] TERM_DISCOUNTS = {0, 10, 15, 20};

    private static final List<ServerPlan> PLANS;

    static {
        List<ServerPlan> plans = new ArrayList<>();
        // ponytail: snapshot of the live Minecraft plans (performance tier, EUR) from 2026-10-05.
        plans.add(new ServerPlan(252, "minecraft-2", 4096, 0, 0, 0.02961249, 8.00, 10, false, true, true, fixedTerms(8.00), null));
        plans.add(new ServerPlan(1116, "minecraft-6gb", 6144, 0, 0, 0.0427, 11.39, 20, false, true, true, fixedTerms(11.39), null));
        plans.add(new ServerPlan(253, "minecraft-3", 8192, 0, 0, 0.05749998, 15.20, 30, true, true, true, fixedTerms(15.20), null));
        plans.add(new ServerPlan(254, "minecraft-4", 12288, 0, 0, 0.084525, 21.60, 50, false, true, true, fixedTerms(21.60), null));
        plans.add(new ServerPlan(255, "minecraft-5", 16384, 0, 0, 0.10924998, 27.20, 80, false, true, true, fixedTerms(27.20), null));
        plans.add(new ServerPlan(256, "minecraft-6", 32768, 0, 0, 0.207, 48.00, 150, false, true, true, fixedTerms(48.00), null));
        PLANS = Collections.unmodifiableList(plans);
    }

    private FallbackPlans() {
    }

    public static List<ServerPlan> get() {
        return PLANS;
    }

    private static List<FixedTerm> fixedTerms(double priceMonthly) {
        List<FixedTerm> terms = new ArrayList<>();
        for (int i = 0; i < TERM_DAYS.length; i++) {
            int months = TERM_DAYS[i] / 30;
            double discount = TERM_DISCOUNTS[i] / 100.0;
            double price = round(priceMonthly * months * (1.0 - discount));
            double effectiveMonthly = round(price / months);
            terms.add(new FixedTerm(TERM_DAYS[i], price, effectiveMonthly, TERM_DISCOUNTS[i]));
        }
        return Collections.unmodifiableList(terms);
    }

    private static double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
