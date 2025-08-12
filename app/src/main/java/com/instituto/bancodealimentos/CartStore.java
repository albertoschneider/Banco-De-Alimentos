package com.instituto.bancodealimentos;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class CartStore {
    private static final String PREF = "cart_prefs";
    private static final String KEY  = "cart_items";

    public static void save(Context ctx, List<Produto> itens) {
        SharedPreferences sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        String json = new Gson().toJson(itens);
        sp.edit().putString(KEY, json).apply();
    }

    public static ArrayList<Produto> load(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        String json = sp.getString(KEY, "[]");
        Type type = new TypeToken<ArrayList<Produto>>(){}.getType();
        ArrayList<Produto> lista = new Gson().fromJson(json, type);
        return (lista != null) ? lista : new ArrayList<>();
    }

    public static void clear(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        sp.edit().remove(KEY).apply();
    }
}
