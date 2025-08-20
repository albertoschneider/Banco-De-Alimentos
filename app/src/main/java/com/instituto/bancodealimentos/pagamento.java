package com.instituto.bancodealimentos;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;

import java.lang.reflect.Type;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Pattern;

// SE Produto está no pacote raiz:
import com.instituto.bancodealimentos.Produto;

public class pagamento extends AppCompatActivity {

    private static final String PREFS = "MeuApp";
    private static final String KEY_CART = "carrinho";

    private TextView tvTotalValue;
    private EditText etPixKey;
    private ImageView ivQrCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pagamento);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom);
            return insets;
        });

        tvTotalValue = findViewById(R.id.tvTotalValue);
        etPixKey     = findViewById(R.id.etPixKey);
        ivQrCode     = findViewById(R.id.ivQrCode);
        ImageButton btnCopyPix = findViewById(R.id.btnCopyPix);

        // TOTAL
        double total = getTotal();
        NumberFormat br = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        tvTotalValue.setText(br.format(total));

        // DADOS RECEBEDOR (strings.xml)
        String chave  = getString(R.string.pix_key);
        String nome   = normalizeNameOrCity(getString(R.string.pix_nome), 25);
        String cidade = normalizeNameOrCity(getString(R.string.pix_cidade), 15);

        // PAYLOAD PIX (valor com ponto)
        String valorStr = String.format(Locale.US, "%.2f", total);
        String txid = makeTxid();

        String payload = new PixPayloadBuilder()
                .setChavePix(chave)
                .setDescricao("Doacao BARC")
                .setNomeRecebedor(nome)
                .setCidadeRecebedor(cidade)
                .setTxid(txid)
                .setValor(valorStr) // null p/ QR sem valor fixo
                .build();

        etPixKey.setText(payload);
        gerarQr(payload, dp(220));

        // Copiar
        btnCopyPix.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("PIX", payload));
            Toast.makeText(this, "Chave PIX copiada!", Toast.LENGTH_SHORT).show();
        });

        // Voltar
        if (findViewById(R.id.btn_voltar) != null) {
            findViewById(R.id.btn_voltar).setOnClickListener(v -> finish());
        }
        if (findViewById(R.id.btnVoltarMenu) != null) {
            findViewById(R.id.btnVoltarMenu).setOnClickListener(v -> {
                Intent intent = new Intent(pagamento.this, menu.class);
                startActivity(intent);
            });
        }
    }

    private double getTotal() {
        double viaIntent = getIntent().getDoubleExtra("total", -1);
        if (viaIntent >= 0) return viaIntent;

        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        String json = sp.getString(KEY_CART, null);
        if (json == null) return 0.0;

        Type type = new TypeToken<ArrayList<Produto>>() {}.getType();
        ArrayList<Produto> itens = new Gson().fromJson(json, type);

        double total = 0.0;
        if (itens != null) {
            for (Produto p : itens) {
                total += safePreco(p) * safeQtd(p);
            }
        }
        return total;
    }

    private String makeTxid() {
        String base = "DOACAO" + System.currentTimeMillis();
        if (base.length() > 25) base = base.substring(0, 25);
        return base;
    }

    private static String normalizeNameOrCity(String input, int maxLen) {
        if (input == null) return "";
        String upper = input.toUpperCase(Locale.ROOT);
        String noAccents = removeAccents(upper);
        String cleaned = noAccents.replaceAll("[^A-Z0-9 \\-\\.]", " ");
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        if (cleaned.length() > maxLen) cleaned = cleaned.substring(0, maxLen);
        return cleaned;
    }

    private static String removeAccents(String s) {
        String norm = Normalizer.normalize(s, Normalizer.Form.NFD);
        return Pattern.compile("\\p{InCombiningDiacriticalMarks}+").matcher(norm).replaceAll("");
    }

    private void gerarQr(String text, int sizePx) {
        QRCodeWriter writer = new QRCodeWriter();
        try {
            com.google.zxing.common.BitMatrix m =
                    writer.encode(text, com.google.zxing.BarcodeFormat.QR_CODE, sizePx, sizePx);
            Bitmap bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
            for (int x = 0; x < sizePx; x++) {
                for (int y = 0; y < sizePx; y++) {
                    bmp.setPixel(x, y, m.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }
            ivQrCode.setImageBitmap(bmp);
        } catch (WriterException e) {
            Toast.makeText(this, "Erro ao gerar QR", Toast.LENGTH_SHORT).show();
        }
    }

    private int dp(int v) {
        float d = getResources().getDisplayMetrics().density;
        return Math.round(v * d);
    }

    // ===== helpers seguros para Produto =====
    private double safePreco(Produto p) {
        try {
            Object v = p.getClass().getMethod("getPreco").invoke(p);
            if (v instanceof Number) return ((Number) v).doubleValue();
        } catch (Exception ignored) {}
        return 0.0;
    }

    private int safeQtd(Produto p) {
        try {
            Object v = p.getClass().getMethod("getQuantidade").invoke(p);
            if (v instanceof Number) return ((Number) v).intValue();
        } catch (Exception ignored) {}
        return 1; // mude p/ 0 se quiser ignorar itens sem quantidade
    }
}