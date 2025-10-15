package com.instituto.bancodealimentos;

import android.content.Context;
import android.content.SharedPreferences;

public final class InitDefaults {
    private InitDefaults(){}

    /**
     * Migra UMA VEZ os valores antigos do strings.xml para SharedPreferences,
     * assim o app já começa usando os dados que funcionavam antes.
     */
    public static void ensureLegacyPixAndWhatsMigrated(Context c) {
        SharedPreferences p = c.getSharedPreferences("settings_pix_whatsapp", Context.MODE_PRIVATE);
        // Se já existe a chave PIX salva, consideramos migrado.
        if (p.contains("pix_chave")) return;

        // Lê strings antigas; usa defaults seguros se não existirem.
        String chave  = safeGet(c, R.string.pix_key, "");
        String nome   = safeGet(c, R.string.pix_nome, "Banco de Alimentos");
        String cidade = safeGet(c, R.string.pix_cidade, "Porto Alegre - RS");

        // WhatsApp antigo do link que você usava (ajuste se for outro):
        String whatsLegacy = "555192481830"; // só dígitos; DDI 55 + DDD + número

        // Salva nos Settings para o app inteiro usar
        SettingsRepository.savePix(c, nome, cidade, chave);
        SettingsRepository.saveWhats(c, whatsLegacy);
    }

    private static String safeGet(Context c, int resId, String def) {
        try { return c.getString(resId); } catch (Exception e) { return def; }
    }
}
