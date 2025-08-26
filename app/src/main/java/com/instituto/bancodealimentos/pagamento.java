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

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;

import java.lang.reflect.Type;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

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

    // Backend / listener
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ListenerRegistration pagamentoListener;
    private String pagamentoIdAtual;

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
                if (btnGerarNovoPix != null) btnGerarNovoPix.setVisibility(View.VISIBLE);
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

        // Header com insets
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

        View back = findViewById(R.id.btn_voltar);
        if (back != null) back.setOnClickListener(v -> finish());

        View btnVoltarMenu = findViewById(R.id.btnVoltarMenu);
        if (btnVoltarMenu != null) btnVoltarMenu.setOnClickListener(v -> {
            startActivity(new Intent(pagamento.this, menu.class));
        });

        // Cria (ou renova) a cobrança NO BACKEND:
        criarOuRenovarCobranca(total, /*orderId*/ null);

        btnCopyPix.setOnClickListener(v -> {
            CharSequence payload = etPixKey.getText();
            if (payload == null || payload.toString().isEmpty()) return;
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("PIX", payload));
            Toast.makeText(this, "Chave PIX copiada!", Toast.LENGTH_SHORT).show();
        });

        if (btnGerarNovoPix != null) {
            btnGerarNovoPix.setOnClickListener(v -> {
                double totalAtual = getTotal();
                criarOuRenovarCobranca(totalAtual, /*orderId*/ null);
            });
        }
    }

    @Override protected void onResume() {
        super.onResume();
        startTimerLoop();
    }

    @Override protected void onPause() {
        super.onPause();
        handler.removeCallbacks(tick);
    }

    @Override protected void onDestroy() {
        if (pagamentoListener != null) pagamentoListener.remove();
        handler.removeCallbacks(tick);
        super.onDestroy();
    }

    // ===================== COBRANÇA | BACKEND + LISTENER =====================

    private void criarOuRenovarCobranca(double total, String orderId) {
        // Limpa listener antigo (se existir)
        if (pagamentoListener != null) {
            pagamentoListener.remove();
            pagamentoListener = null;
        }
        pagamentoIdAtual = null;

        // Chama a Cloud Function
        Map<String, Object> data = new HashMap<>();
        data.put("valor", total);
        if (orderId != null) data.put("orderId", orderId);

        FirebaseFunctions.getInstance()
                .getHttpsCallable("criarCobranca")
                .call(data)
                .addOnSuccessListener(this::onCobrancaCriada)
                .addOnFailureListener(err -> {
                    // ---- FALLBACK LOCAL ----
                    fallbackLocal(total);
                });
    }

    private void onCobrancaCriada(HttpsCallableResult res) {
        @SuppressWarnings("unchecked")
        Map<String, Object> r = (Map<String, Object>) res.getData();
        if (r == null) {
            Toast.makeText(this, "Erro ao criar cobrança", Toast.LENGTH_LONG).show();
            return;
        }

        // pagamentoId para observar status no Firestore
        pagamentoIdAtual = asString(r.get("pagamentoId"));
        String payload = asString(r.get("qrCodePayload"));

        // Calcula expiração (preferindo millis numérico; senão, ISO 8601)
        long expMillis = System.currentTimeMillis() + WINDOW_MS;

        Object expNum = r.get("expiresAtMillis");
        if (expNum instanceof Number) {
            expMillis = ((Number) expNum).longValue();
        } else {
            String expiresAtIso = asString(r.get("expiresAt"));
            if (expiresAtIso != null && !expiresAtIso.isEmpty()) {
                try {
                    if (android.os.Build.VERSION.SDK_INT >= 26) {
                        expMillis = java.time.Instant.parse(expiresAtIso).toEpochMilli();
                    } else {
                        // Suporta "Z" e offsets com dois pontos no final
                        String s = expiresAtIso;
                        if (s.endsWith("Z")) s = s.substring(0, s.length() - 1) + "+0000";
                        s = s.replaceFirst("([\\+\\-]\\d{2}):(\\d{2})$", "$1$2");
                        java.text.SimpleDateFormat f =
                                new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", java.util.Locale.US);
                        expMillis = f.parse(s).getTime();
                    }
                } catch (Exception ignore) {
                    // mantém expMillis como agora + 10min
                }
            }
        }

        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit().putLong(KEY_PIX_EXPIRE_AT, expMillis).apply();

        // Preenche UI
        etPixKey.setText(payload);
        gerarQr(payload, dp(220));
        if (btnGerarNovoPix != null) btnGerarNovoPix.setVisibility(View.GONE);
        startTimerLoop();

        // Começa a observar o doc até virar PAGO / EXPIRADO
        if (pagamentoIdAtual != null && !pagamentoIdAtual.isEmpty()) {
            observarPagamento(pagamentoIdAtual);
        }
    }

    private void observarPagamento(String pagamentoId) {
        if (pagamentoListener != null) pagamentoListener.remove();

        pagamentoListener = db.collection("pagamentos").document(pagamentoId)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null || !snap.exists()) return;

                    String status = snap.getString("status");
                    if ("PAGO".equalsIgnoreCase(status)) {
                        handler.removeCallbacks(tick);

                        Double v = snap.getDouble("valor");
                        Timestamp paidAt = snap.getTimestamp("paidAt");

                        Intent i = new Intent(this, pedido_pago.class);
                        i.putExtra("pagamentoId", snap.getId());
                        if (v != null) i.putExtra("valor", v);
                        if (paidAt != null) i.putExtra("paidAtMillis", paidAt.toDate().getTime());
                        startActivity(i);
                        finish();
                    } else if ("EXPIRADO".equalsIgnoreCase(status)) {
                        if (btnGerarNovoPix != null) btnGerarNovoPix.setVisibility(View.VISIBLE);
                        Toast.makeText(this, "Pagamento expirado. Gere um novo QR.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ===================== FALLBACK LOCAL (sem backend) =====================

    private void fallbackLocal(double total) {
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

        long exp = System.currentTimeMillis() + WINDOW_MS;
        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit().putLong(KEY_PIX_EXPIRE_AT, exp).apply();

        if (btnGerarNovoPix != null) btnGerarNovoPix.setVisibility(View.GONE);
        startTimerLoop();

        Toast.makeText(this,
                "Aviso: QR local (sem webhook). Configure a Cloud Function para confirmação automática.",
                Toast.LENGTH_LONG).show();
    }

    private static String asString(Object o) { return o == null ? null : String.valueOf(o); }

    // ===================== UTILIDADES EXISTENTES =====================

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
