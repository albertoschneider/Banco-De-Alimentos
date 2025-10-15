package com.instituto.bancodealimentos;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.TextUtils;

public final class SettingsRepository {

    private static final String PREFS = "settings_pix_whatsapp";
    private static final String K_PIX_NOME   = "pix_nome";
    private static final String K_PIX_CIDADE = "pix_cidade";
    private static final String K_PIX_CHAVE  = "pix_chave";
    private static final String K_WHATS      = "whats_number"; // só dígitos

    private SettingsRepository() {}

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static void savePix(Context c, String nome, String cidade, String chave) {
        prefs(c).edit()
                .putString(K_PIX_NOME, nome)
                .putString(K_PIX_CIDADE, cidade)
                .putString(K_PIX_CHAVE, chave)
                .apply();
    }

    /** aceita colagem com +55, espaços, (), -; salva apenas dígitos */
    public static void saveWhats(Context c, String raw) {
        prefs(c).edit().putString(K_WHATS, digits(raw)).apply();
    }

    public static String getPixNome(Context c)   { return prefs(c).getString(K_PIX_NOME,   "Banco de Alimentos"); }
    public static String getPixCidade(Context c) { return prefs(c).getString(K_PIX_CIDADE, "Porto Alegre - RS"); }
    public static String getPixChave(Context c)  { return prefs(c).getString(K_PIX_CHAVE,  ""); }
    public static String getWhats(Context c)     { return prefs(c).getString(K_WHATS,      ""); }

    /**
     * Monta o link do WhatsApp com mensagem opcional abrindo diretamente no número salvo.
     * Usa endpoint api.whatsapp.com/send (mais compatível).
     */
    public static Uri buildWhatsUrl(Context c, String msgPtBr) {
        String phone = getWhats(c); // só dígitos
        String encoded = Uri.encode(msgPtBr == null ? "" : msgPtBr);

        // Sem número salvo: abre o Whats e deixa a mensagem pronta para escolher o contato
        if (TextUtils.isEmpty(phone)) {
            return Uri.parse("https://api.whatsapp.com/send?text=" + encoded);
        }

        // Se for BR e vier só DDD+numero (11 dígitos) sem DDI, prefixa 55
        if (phone.length() == 11 && !phone.startsWith("55")) {
            phone = "55" + phone;
        }

        // Para outros comprimentos, envia como está (já que pode ser DDI de outro país)
        return Uri.parse("https://api.whatsapp.com/send?phone=" + phone + "&text=" + encoded);
    }

    /** mantém apenas dígitos */
    public static String digits(String s) {
        if (s == null) return "";
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch >= '0' && ch <= '9') b.append(ch);
        }
        return b.toString();
    }
}
