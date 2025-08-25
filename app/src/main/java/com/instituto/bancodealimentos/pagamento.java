package com.instituto.bancodealimentos;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;

import java.lang.reflect.Type;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Pattern;

public class pagamento extends AppCompatActivity {

    private static final String PREFS = "MeuApp";
    private static final String KEY_CART = "carrinho";
    private static final String KEY_PIX_EXPIRE_AT = "pix_expire_at"; // epoch millis
    private static final long WINDOW_MS = 10 * 60 * 1000L; // 10 minutos

    private TextView tvTotalValue;
    private EditText etPixKey;
    private ImageView ivQrCode;

    private TextView tvCountdown;
    private ProgressBar timeProgress;
    private View btnGerarNovoPix;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            long now = System.currentTimeMillis();
            long exp = getSharedPreferences(PREFS, MODE_PRIVATE).getLong(KEY_PIX_EXPIRE_AT, 0L);
            if (exp <= 0L) exp = now + WINDOW_MS;

            long remainMs = exp - now;
            if (remainMs <= 0) {
                tvCountdown.setText("00:00");
                timeProgress.setProgress(0);
                btnGerarNovoPix.setVisibility(View.VISIBLE);
                handler.removeCallbacks(this);
                return;
            }

            int sec = (int) Math.ceil(remainMs / 1000.0);
            int mm = sec / 60;
            int ss = sec % 60;
            tvCountdown.setText(String.format(Locale.getDefault(), "%02d:%02d", mm, ss));

            if (timeProgress.getMax() != 600) timeProgress.setMax(600);
            timeProgress.setProgress(sec);

            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pagamento);

        // Header com o MESMO tratamento de insets da tela de Pontos de Coleta
        View header = findViewById(R.id.header);
        if (header != null) {
            ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
                Insets sb = insets.getInsets(WindowInsetsCompat.Type.statusBars());
                v.setPadding(v.getPaddingLeft(), sb.top, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
            ViewCompat.requestApplyInsets(header);
        }

        tvTotalValue   = findViewById(R.id.tvTotalValue);
        etPixKey       = findViewById(R.id.etPixKey);
        ivQrCode       = findViewById(R.id.ivQrCode);
        ImageButton btnCopyPix = findViewById(R.id.btnCopyPix);

        tvCountdown    = findViewById(R.id.tvCountdown);
        timeProgress   = findViewById(R.id.timeProgress);
        btnGerarNovoPix= findViewById(R.id.btnGerarNovoPix);

        double total = getTotal();
        NumberFormat br = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        tvTotalValue.setText(br.format(total));

        String chave  = getString(R.string.pix_key);
        String nome   = normalizeNameOrCity(getString(R.string.pix_nome), 25);
        String cidade = normalizeNameOrCity(getString(R.string.pix_cidade), 15);

        String valorStr = String.format(Locale.US, "%.2f", total);
        String txid = makeTxid();
        String payload = new PixPayloadBuilder()
                .setChavePix(chave)
                .setDescricao("Doacao BARC")
                .setNomeRecebedor(nome)
                .setCidadeRecebedor(cidade)
                .setTxid(txid)
                .setValor(valorStr)
                .build();

        etPixKey.setText(payload);
        gerarQr(payload, dp(220));

        btnCopyPix.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("PIX", payload));
            Toast.makeText(this, "Chave PIX copiada!", Toast.LENGTH_SHORT).show();
        });

        View back = findViewById(R.id.btn_voltar);
        if (back != null) back.setOnClickListener(v -> finish());

        View btnVoltarMenu = findViewById(R.id.btnVoltarMenu);
        if (btnVoltarMenu != null) btnVoltarMenu.setOnClickListener(v -> {
            startActivity(new Intent(pagamento.this, menu.class));
        });

        ensureExpireAt();

        btnGerarNovoPix.setOnClickListener(v -> {
            setNewExpireAt();
            btnGerarNovoPix.setVisibility(View.GONE);
            startTimerLoop();
        });
    }

    @Override protected void onResume() {
        super.onResume();
        startTimerLoop();
    }

    @Override protected void onPause() {
        super.onPause();
        handler.removeCallbacks(tick);
    }

    private void ensureExpireAt() {
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        long exp = sp.getLong(KEY_PIX_EXPIRE_AT, 0L);
        long now = System.currentTimeMillis();
        if (exp <= now) {
            setNewExpireAt();
        }
    }

    private void setNewExpireAt() {
        long exp = System.currentTimeMillis() + WINDOW_MS;
        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putLong(KEY_PIX_EXPIRE_AT, exp)
                .apply();
    }

    private void startTimerLoop() {
        handler.removeCallbacks(tick);
        handler.post(tick);
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
            for (Produto p : itens) total += safePreco(p) * safeQtd(p);
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
        return java.util.regex.Pattern.compile("\\p{InCombiningDiacriticalMarks}+").matcher(norm).replaceAll("");
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
        return 1;
    }
}
