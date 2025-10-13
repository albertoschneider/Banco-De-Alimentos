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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;

import org.json.JSONObject;

import java.lang.reflect.Type;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class pagamento extends AppCompatActivity {

    // ================== CONFIG ==================
    public static final String EXTRA_DONATION_ID = "donationId";

    private static final String PREFS = "MeuApp";
    private static final String KEY_CART = "carrinho";
    private static final String KEY_PIX_EXPIRE_AT = "pix_expire_at"; // epoch millis
    private static final long WINDOW_MS = 10 * 60 * 1000L; // 10 minutos

    // Base do seu deploy Vercel (produção), SEM barra no final:
    private static final String VERCEL_BASE =
            "https://barc-webhooks-wrvde24re-albertos-projects-f9774983.vercel.app";

    // ================== UI ==================
    private TextView tvTotalValue;
    private EditText etPixKey;
    private ImageView ivQrCode;

    private TextView tvCountdown;
    private ProgressBar timeProgress;
    private View btnGerarNovoPix;

    // ================== Backend / listener ==================
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ListenerRegistration pagamentoListener;
    private String pagamentoIdAtual;

    // ================== Timer ==================
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            long now = System.currentTimeMillis();
            long exp = getSharedPreferences(PREFS, MODE_PRIVATE).getLong(KEY_PIX_EXPIRE_AT, 0L);
            if (exp <= 0L) exp = now + WINDOW_MS;

            long remainMs = exp - now;
            if (remainMs <= 0) {
                if (tvCountdown != null) tvCountdown.setText("00:00");
                if (timeProgress != null) timeProgress.setProgress(0);
                if (btnGerarNovoPix != null) btnGerarNovoPix.setVisibility(View.VISIBLE);
                handler.removeCallbacks(this);
                return;
            }

            int sec = (int) Math.ceil(remainMs / 1000.0);
            int mm = sec / 60;
            int ss = sec % 60;
            if (tvCountdown != null) {
                tvCountdown.setText(String.format(Locale.getDefault(), "%02d:%02d", mm, ss));
            }

            if (timeProgress != null) {
                if (timeProgress.getMax() != 600) timeProgress.setMax(600);
                timeProgress.setProgress(sec);
            }

            handler.postDelayed(this, 1000);
        }
    };

    // ================== Ciclo de vida ==================
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

        View back = findViewById(R.id.btn_voltar);
        if (back != null) back.setOnClickListener(v -> finish());

        View btnVoltarMenu = findViewById(R.id.btnVoltarMenu);
        if (btnVoltarMenu != null) btnVoltarMenu.setOnClickListener(v -> {
            startActivity(new Intent(pagamento.this, menu.class));
        });

        if (btnCopyPix != null) {
            btnCopyPix.setOnClickListener(v -> {
                CharSequence payload = etPixKey != null ? etPixKey.getText() : null;
                if (payload == null || payload.toString().isEmpty()) return;
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                cm.setPrimaryClip(ClipData.newPlainText("PIX", payload));
                Toast.makeText(this, "Chave PIX copiada!", Toast.LENGTH_SHORT).show();
            });
        }

        // Se vier donationId, reabrimos a MESMA cobrança; se não, criamos nova
        String donationId = getIntent().getStringExtra(EXTRA_DONATION_ID);
        if (donationId != null && !donationId.isEmpty()) {
            carregarCobrancaExistente(donationId);
            // botão "gerar novo" só aparece quando expirar
            if (btnGerarNovoPix != null) btnGerarNovoPix.setVisibility(View.GONE);
        } else {
            double total = getTotal();
            NumberFormat br = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
            if (tvTotalValue != null) tvTotalValue.setText(br.format(total));

            if (btnGerarNovoPix != null) {
                btnGerarNovoPix.setOnClickListener(v -> {
                    double totalAtual = getTotal();
                    criarOuRenovarCobranca(totalAtual, /*orderId*/ null);
                });
            }
            criarOuRenovarCobranca(total, /*orderId*/ null);
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

    // ===================== REABRIR COBRANÇA EXISTENTE =====================
    private void carregarCobrancaExistente(String donationId) {
        db.collection("doacoes").document(donationId)
                .get()
                .addOnSuccessListener(this::bindDoacao)
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Falha ao carregar pagamento: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void bindDoacao(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) {
            Toast.makeText(this, "Pedido não encontrado.", Toast.LENGTH_LONG).show();
            return;
        }
        Doacao d = doc.toObject(Doacao.class);
        if (d == null) {
            Toast.makeText(this, "Erro ao ler dados do pedido.", Toast.LENGTH_LONG).show();
            return;
        }

        if (!"pending".equals(d.getStatus())) {
            Toast.makeText(this, "Este pedido não está mais pendente.", Toast.LENGTH_LONG).show();
            return;
        }

        // Valor
        double total = d.getAmountCents() / 100.0;
        NumberFormat br = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        if (tvTotalValue != null) tvTotalValue.setText(br.format(total));

        // Payload PIX (copia e cola) e QR
        String payload = d.getPixCopiaCola();
        if (etPixKey != null) etPixKey.setText(payload);
        gerarQr(payload, dp(220));

        // Timer baseado no expiresAt (ou createdAt + 10min)
        long expAt = 0L;
        if (d.getExpiresAt() != null) {
            expAt = d.getExpiresAt().toDate().getTime();
        } else if (d.getCreatedAt() != null) {
            expAt = d.getCreatedAt().toDate().getTime() + WINDOW_MS;
        }
        if (expAt <= 0L) expAt = System.currentTimeMillis() + WINDOW_MS;

        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit().putLong(KEY_PIX_EXPIRE_AT, expAt).apply();

        startTimerLoop();

        // (Opcional) ouvir confirmação automática se você gravou um ID de pagamento
        // Tente "pagamentoId" ou "referenceId" no doc de doacoes:
        String payId = doc.getString("pagamentoId");
        if (payId == null || payId.isEmpty()) {
            payId = doc.getString("referenceId");
        }
        if (payId != null && !payId.isEmpty()) {
            observarPagamento(payId);
            pagamentoIdAtual = payId;
        } else {
            // Se não houver ID do doc em /pagamentos, tudo bem; sem listener aqui.
            pagamentoIdAtual = null;
        }
    }

    // ===================== COBRANÇA NOVA | VERCEL + LISTENER =====================
    private void criarOuRenovarCobranca(double total, String orderId) {
        // Limpa listener antigo (se existir)
        if (pagamentoListener != null) {
            pagamentoListener.remove();
            pagamentoListener = null;
        }
        pagamentoIdAtual = null;

        // Exige usuário logado (token para Authorization: Bearer)
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) {
            Toast.makeText(this, "Faça login novamente.", Toast.LENGTH_LONG).show();
            return;
        }

        u.getIdToken(true).addOnSuccessListener(tokenResult -> {
            String idToken = tokenResult.getToken();

            new Thread(() -> {
                try {
                    OkHttpClient client = new OkHttpClient();

                    JSONObject body = new JSONObject();
                    body.put("valor", total);
                    if (orderId != null) body.put("orderId", orderId);

                    Request request = new Request.Builder()
                            .url(VERCEL_BASE + "/api/criarCobranca")
                            .addHeader("Authorization", "Bearer " + idToken)
                            .post(RequestBody.create(
                                    body.toString(),
                                    MediaType.parse("application/json; charset=utf-8")
                            ))
                            .build();

                    try (Response resp = client.newCall(request).execute()) {
                        if (!resp.isSuccessful()) throw new RuntimeException("HTTP " + resp.code());
                        String json = resp.body() != null ? resp.body().string() : "{}";
                        JSONObject r = new JSONObject(json);

                        final String pagamentoId = r.optString("pagamentoId", null);
                        final String payload = r.optString("qrCodePayload", "");
                        final long expMillis = r.optLong("expiresAtMillis",
                                System.currentTimeMillis() + WINDOW_MS);

                        runOnUiThread(() -> {
                            pagamentoIdAtual = pagamentoId;
                            if (tvTotalValue != null) {
                                NumberFormat br = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
                                tvTotalValue.setText(br.format(total));
                            }
                            if (etPixKey != null) etPixKey.setText(payload);
                            gerarQr(payload, dp(220));

                            getSharedPreferences(PREFS, MODE_PRIVATE)
                                    .edit().putLong(KEY_PIX_EXPIRE_AT, expMillis).apply();

                            if (btnGerarNovoPix != null) btnGerarNovoPix.setVisibility(View.GONE);
                            startTimerLoop();

                            if (pagamentoIdAtual != null && !pagamentoIdAtual.isEmpty()) {
                                observarPagamento(pagamentoIdAtual);
                            }
                        });
                    }
                } catch (Exception e) {
                    // Fallback local se der erro no backend
                    runOnUiThread(() -> fallbackLocal(total));
                }
            }).start();

        }).addOnFailureListener(err ->
                Toast.makeText(this, "Erro ao obter token de autenticação.", Toast.LENGTH_LONG).show()
        );
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

        if (tvTotalValue != null) {
            NumberFormat br = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
            tvTotalValue.setText(br.format(total));
        }
        if (etPixKey != null) etPixKey.setText(payload);
        gerarQr(payload, dp(220));

        long exp = System.currentTimeMillis() + WINDOW_MS;
        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit().putLong(KEY_PIX_EXPIRE_AT, exp).apply();

        if (btnGerarNovoPix != null) btnGerarNovoPix.setVisibility(View.GONE);
        startTimerLoop();

        Toast.makeText(this,
                "Aviso: QR local (sem webhook). Configure o backend para confirmação automática.",
                Toast.LENGTH_LONG).show();
    }

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
        if (ivQrCode == null || text == null) return;
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
