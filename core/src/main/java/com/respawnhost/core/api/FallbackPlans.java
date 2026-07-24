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
        plans.add(new ServerPlan(1, "2 GB", 2048, 100, 10240, 0.0055, 1.99, 10, false, true, true, fixedTerms(1.99), null));
        plans.add(new ServerPlan(2, "4 GB", 4096, 200, 20480, 0.0082, 2.99, 20, true, true, true, fixedTerms(2.99), null));
        plans.add(new ServerPlan(3, "8 GB", 8192, 300, 40960, 0.0164, 5.99, 40, false, true, true, fixedTerms(5.99), null));
        plans.add(new ServerPlan(4, "16 GB", 16384, 400, 81920, 0.0328, 11.99, 80, false, true, true, fixedTerms(11.99), null));
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
