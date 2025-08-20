package com.instituto.bancodealimentos;

import java.text.NumberFormat;
import java.util.Locale;

public class Money {
    private static final NumberFormat CURRENCY_BR = NumberFormat.getCurrencyInstance(new Locale("pt","BR"));

    public static double parse(String s) {
        if (s == null) return 0.0;
        s = s.replace("R$", "").replace(" ", "").replace(".", "").replace(",", ".");
        try { return Double.parseDouble(s); } catch (Exception e) { return 0.0; }
    }

    public static String fmt(double v) {
        return CURRENCY_BR.format(v);
    }
}