package com.instituto.bancodealimentos;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;

import java.text.Normalizer;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Pattern;

public class pagamento extends AppCompatActivity {

    // ===== Persistência do PIX ativo =====
    private static final String PREFS = "MeuApp";
    private static final String KEY_ACTIVE_TXID = "active_txid";
    private static final String KEY_ACTIVE_STARTED_AT_MS = "active_started_at_ms";
    private static final String KEY_ACTIVE_EXPIRES_AT_MS = "active_expires_at_ms";
    private static final String KEY_ACTIVE_TOTAL_LONG = "active_total_long";
    private static final String KEY_ACTIVE_PAYLOAD = "active_payload";

    // Duração do pagamento: 10 minutos (em ms)
    private static final long DURATION_MS = 10L * 60L * 1000L;

    // ===== UI =====
    private TextView tvTotalValue;
    private EditText etPixKey;
    private ImageView ivQrCode;
    private ImageButton btnCopyPix;
    private TextView tvCountdown, tvPixExpiraLabel;
    private ProgressBar timeProgress;
    private View btnGerarNovoPix;

    // ===== Estado do pagamento =====
    private String activeTxid;
    private long startedAtMs;
    private long expiresAtMs;
    private double total;
    private String payload;

    // ===== Ticker (1s) =====
    private final Handler handler = new Handler();
    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            updateCountdown();
            handler.postDelayed(this, 1000L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Edge-to-edge (status bar transparente) + ícones escuros
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        setContentView(R.layout.activity_pagamento);

        View header = findViewById(R.id.header);
        if (header != null) {
            ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
                Insets sb = insets.getInsets(WindowInsetsCompat.Type.statusBars());
                v.setPadding(v.getPaddingLeft(), sb.top, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
            ViewCompat.requestApplyInsets(header);
            WindowInsetsControllerCompat c = ViewCompat.getWindowInsetsController(getWindow().getDecorView());
            if (c != null) c.setAppearanceLightStatusBars(true);
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom);
            return insets;
        });

        // Bind UI
        tvTotalValue = findViewById(R.id.tvTotalValue);
        etPixKey     = findViewById(R.id.etPixKey);
        ivQrCode     = findViewById(R.id.ivQrCode);
        btnCopyPix   = findViewById(R.id.btnCopyPix);
        tvCountdown  = findViewById(R.id.tvCountdown);
        tvPixExpiraLabel = findViewById(R.id.tvPixExpiraLabel);
        timeProgress = findViewById(R.id.timeProgress);
        btnGerarNovoPix = findViewById(R.id.btnGerarNovoPix);

        // Navegação
        View back = findViewById(R.id.btn_voltar);
        if (back != null) back.setOnClickListener(v -> finish());
        View btnVoltarMenu = findViewById(R.id.btnVoltarMenu);
        if (btnVoltarMenu != null) btnVoltarMenu.setOnClickListener(v ->
                startActivity(new Intent(pagamento.this, menu.class)));

        // Total (intent → fallback carrinho)
        total = getTotal();
        NumberFormat br = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        tvTotalValue.setText(br.format(total));

        // Garante pagamento ativo em tempo real:
        // - Se existir e NÃO expirou → reutiliza e continua a contagem (com base no relógio).
        // - Se não existir ou JÁ expirou → gera NOVO imediatamente (sem mostrar "expirado").
        ensureActivePaymentOrCreateNew();

        // UI inicial
        etPixKey.setText(payload);
        gerarQr(payload, dp(220));

        btnCopyPix.setOnClickListener(v -> {
            if (isExpired()) {
                Toast.makeText(this, "O código expirou. Gere um novo.", Toast.LENGTH_SHORT).show();
                return;
            }
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("PIX", payload));
            Toast.makeText(this, "Chave PIX copiada!", Toast.LENGTH_SHORT).show();
        });

        btnGerarNovoPix.setOnClickListener(v -> {
            // Usuário estava na tela e expirou → gera um novo na hora
            forceCreateNewPayment();
        });
    }

    @Override protected void onResume() {
        super.onResume();
        // Se o usuário saiu e voltou: se expirou durante a ausência, criamos NOVO automaticamente
        if (isExpired()) {
            forceCreateNewPayment();
        }
        startTicker();
    }

    @Override protected void onPause() {
        super.onPause();
        stopTicker();
    }

    // ====================== Pagamento ativo ======================

    private void ensureActivePaymentOrCreateNew() {
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        long now = System.currentTimeMillis();

        String savedTxid = sp.getString(KEY_ACTIVE_TXID, null);
        long savedExpires = sp.getLong(KEY_ACTIVE_EXPIRES_AT_MS, 0L);

        if (savedTxid != null && now < savedExpires) {
            // Reutiliza (o tempo correu "de verdade" enquanto app estava fechado)
            activeTxid   = savedTxid;
            startedAtMs  = sp.getLong(KEY_ACTIVE_STARTED_AT_MS, now);
            expiresAtMs  = savedExpires;
            long totalBits = sp.getLong(KEY_ACTIVE_TOTAL_LONG, Double.doubleToRawLongBits(total));
            total = Double.longBitsToDouble(totalBits);
            payload = sp.getString(KEY_ACTIVE_PAYLOAD, null);
            if (payload == null) payload = buildPixPayload(activeTxid, total);
        } else {
            // Já estava expirado (ou inexistente) → cria novo imediatamente
            createAndPersistNew(now);
        }

        // Progresso inicial
        timeProgress.setMax(600);
        updateCountdown();
        refreshUiEnabled();
    }

    private void forceCreateNewPayment() {
        long now = System.currentTimeMillis();
        createAndPersistNew(now);
        etPixKey.setText(payload);
        gerarQr(payload, dp(220));
        btnGerarNovoPix.setVisibility(View.GONE);
        updateCountdown();
        refreshUiEnabled();
        startTicker();
    }

    private void createAndPersistNew(long now) {
        startedAtMs = now;
        expiresAtMs = now + DURATION_MS;
        activeTxid  = makeTxid();
        payload     = buildPixPayload(activeTxid, total);

        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        sp.edit()
                .putString(KEY_ACTIVE_TXID, activeTxid)
                .putLong(KEY_ACTIVE_STARTED_AT_MS, startedAtMs)
                .putLong(KEY_ACTIVE_EXPIRES_AT_MS, expiresAtMs)
                .putLong(KEY_ACTIVE_TOTAL_LONG, Double.doubleToRawLongBits(total))
                .putString(KEY_ACTIVE_PAYLOAD, payload)
                .apply();
    }

    private boolean isExpired() {
        return System.currentTimeMillis() >= expiresAtMs;
    }

    private void startTicker() {
        handler.removeCallbacks(ticker);
        handler.post(ticker);
    }

    private void stopTicker() {
        handler.removeCallbacks(ticker);
    }

    private void updateCountdown() {
        long now = System.currentTimeMillis();
        long remaining = Math.max(0L, expiresAtMs - now);
        int remainingSec = (int) Math.ceil(remaining / 1000.0);

        // texto mm:ss
        tvCountdown.setText(formatMMSS(remainingSec));

        // progresso (contagem regressiva 600 -> 0)
        timeProgress.setProgress(remainingSec);

        if (remaining <= 0L) {
            // Expirou ENQUANTO a tela estava aberta → mostra estado e dá opção de gerar novo
            tvPixExpiraLabel.setText("Expirou");
            tvCountdown.setText("00:00");
            stopTicker();
            btnGerarNovoPix.setVisibility(View.VISIBLE);
            refreshUiEnabled();
        } else {
            tvPixExpiraLabel.setText("Expira em");
            btnGerarNovoPix.setVisibility(View.GONE);
            refreshUiEnabled();
        }
    }

    private void refreshUiEnabled() {
        boolean expired = isExpired();
        btnCopyPix.setEnabled(!expired);
        btnCopyPix.setAlpha(expired ? 0.4f : 1f);
    }

    private static String formatMMSS(int totalSec) {
        int mm = totalSec / 60;
        int ss = totalSec % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", mm, ss);
    }

    // ====================== Payload & QR ======================

    private String buildPixPayload(String txid, double totalValue) {
        // DADOS RECEBEDOR (strings.xml)
        String chave  = getString(R.string.pix_key);
        String nome   = normalizeNameOrCity(getString(R.string.pix_nome), 25);
        String cidade = normalizeNameOrCity(getString(R.string.pix_cidade), 15);

        String valorStr = String.format(Locale.US, "%.2f", totalValue);

        return new PixPayloadBuilder()
                .setChavePix(chave)
                .setDescricao("Doacao BARC")
                .setNomeRecebedor(nome)
                .setCidadeRecebedor(cidade)
                .setTxid(txid)
                .setValor(valorStr)
                .build();
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

    private int dp(int v) {
        float d = getResources().getDisplayMetrics().density;
        return Math.round(v * d);
    }

    // ====================== Total & helpers ======================

    private double getTotal() {
        // 1) via Intent
        double viaIntent = getIntent().getDoubleExtra("total", -1);
        if (viaIntent >= 0) return viaIntent;

        // 2) fallback: carrinho salvo
        ArrayList<Produto> itens = CartStore.load(this);
        double t = 0.0;
        if (itens != null) {
            for (Produto p : itens) t += safePreco(p) * safeQtd(p);
        }
        return t;
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

    private String makeTxid() {
        String base = "DOACAO" + System.currentTimeMillis();
        if (base.length() > 25) base = base.substring(0, 25);
        return base;
    }
}