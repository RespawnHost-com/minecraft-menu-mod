package com.respawnhost.core.recommend;

import com.respawnhost.core.model.ServerPlan;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class PlanRecommender {
    private PlanRecommender() {
    }

    public static ServerPlan recommend(List<ServerPlan> plans, Integer recommendedRamMb, int loadedModCount) {
        if (plans == null || plans.isEmpty()) {
            return null;
        }
        List<ServerPlan> sorted = new ArrayList<>(plans);
        sorted.sort(Comparator.comparingInt(ServerPlan::getMemory));
        if (recommendedRamMb != null) {
            for (ServerPlan plan : sorted) {
                if (plan.getMemory() >= recommendedRamMb) {
                    return plan;
                }
            }
            return sorted.get(sorted.size() - 1);
        }
        if (loadedModCount < 10) {
            return sorted.get(0);
        }
        if (loadedModCount < 100) {
            return sorted.get(sorted.size() / 2);
        }
        return sorted.get(sorted.size() - 1);
    }
}
