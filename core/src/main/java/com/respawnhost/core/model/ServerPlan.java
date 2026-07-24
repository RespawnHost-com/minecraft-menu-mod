package com.respawnhost.core.model;

import java.util.List;
import java.util.Map;

public final class ServerPlan {
    private int id;
    private String name;
    private int memory;
    private int cpu;
    private int disk;
    private double priceHourly;
    private double priceMonthly;
    private Integer recommendedPlayers;
    private boolean isPopular;
    private boolean availableHourly;
    private boolean availableFixed;
    private List<FixedTerm> fixedTerms;
    private Map<String, PriceOverride> priceOverrides;

    public ServerPlan() {
    }

    public ServerPlan(int id, String name, int memory, int cpu, int disk,
            double priceHourly, double priceMonthly, Integer recommendedPlayers,
            boolean isPopular, boolean availableHourly, boolean availableFixed,
            List<FixedTerm> fixedTerms, Map<String, PriceOverride> priceOverrides) {
        this.id = id;
        this.name = name;
        this.memory = memory;
        this.cpu = cpu;
        this.disk = disk;
        this.priceHourly = priceHourly;
        this.priceMonthly = priceMonthly;
        this.recommendedPlayers = recommendedPlayers;
        this.isPopular = isPopular;
        this.availableHourly = availableHourly;
        this.availableFixed = availableFixed;
        this.fixedTerms = fixedTerms;
        this.priceOverrides = priceOverrides;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getMemory() {
        return memory;
    }

    public int getCpu() {
        return cpu;
    }

    public int getDisk() {
        return disk;
    }

    public double getPriceHourly() {
        return priceHourly;
    }

    public double getPriceMonthly() {
        return priceMonthly;
    }

    public Integer getRecommendedPlayers() {
        return recommendedPlayers;
    }

    public boolean isPopular() {
        return isPopular;
    }

    public boolean isAvailableHourly() {
        return availableHourly;
    }

    public boolean isAvailableFixed() {
        return availableFixed;
    }

    public List<FixedTerm> getFixedTerms() {
        return fixedTerms;
    }

    public Map<String, PriceOverride> getPriceOverrides() {
        return priceOverrides;
    }

    public String ramDisplay() {
        if (memory % 1024 == 0) {
            return String.format("%d GB", memory / 1024);
        }
        return String.format("%d MB", memory);
    }

    public int slotsOrDefault() {
        return recommendedPlayers != null ? recommendedPlayers.intValue() : 0;
    }

    public double monthlyOrEstimate() {
        return priceMonthly;
    }
}
