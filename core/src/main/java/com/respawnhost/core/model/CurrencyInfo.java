package com.respawnhost.core.model;

import java.util.Locale;

/** Display currency as delivered by GET /api/currencies. */
public final class CurrencyInfo {
    public static final CurrencyInfo EUR = new CurrencyInfo("EUR", "€", "after", true, 2, 1.0);

    private String code;
    private String symbol;
    private String symbolPosition;
    private boolean symbolSpace;
    private int decimalPlaces;
    private double rateFromEur;

    public CurrencyInfo() {
    }

    public CurrencyInfo(String code, String symbol, String symbolPosition, boolean symbolSpace,
            int decimalPlaces, double rateFromEur) {
        this.code = code;
        this.symbol = symbol;
        this.symbolPosition = symbolPosition;
        this.symbolSpace = symbolSpace;
        this.decimalPlaces = decimalPlaces;
        this.rateFromEur = rateFromEur;
    }

    public String getCode() {
        return code;
    }

    public double getRateFromEur() {
        return rateFromEur > 0 ? rateFromEur : 1.0;
    }

    public int getDecimalPlaces() {
        return decimalPlaces;
    }

    /** Formats an amount in this currency, e.g. "10,64 €" (de) or "$10.49" (en). */
    public String format(double amount, int decimals, String lang) {
        Locale locale = "de".equals(lang) ? Locale.GERMANY : Locale.US;
        String number = String.format(locale, "%,." + decimals + "f", amount);
        String space = symbolSpace ? " " : "";
        return "before".equals(symbolPosition) ? symbol + space + number : number + space + symbol;
    }
}
