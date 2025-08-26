package com.instituto.bancodealimentos;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class pedido_pago extends AppCompatActivity {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private TextView tvValor, tvDataHora;

    private final NumberFormat br = NumberFormat.getCurrencyInstance(new Locale("pt","BR"));
    private final SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", new Locale("pt","BR"));

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pedido_pago);

        tvValor = findViewById(R.id.tvValor);
        tvDataHora = findViewById(R.id.tvDataHora);

        // Limpa o carrinho local (se você usa outro store, troque aqui)
        try { CartStore.clear(this); } catch (Exception ignored) {}

        String pagamentoId = getIntent().getStringExtra("pagamentoId");
        double valorExtra = getIntent().getDoubleExtra("valor", 0.0);
        long paidAtMillis = getIntent().getLongExtra("paidAtMillis", 0L);

        if (pagamentoId != null && !pagamentoId.isEmpty()) {
            // Busca do Firestore (preferível)
            db.collection("pagamentos").document(pagamentoId).get()
                    .addOnSuccessListener(this::preencherComSnap)
                    .addOnFailureListener(err -> preencherFallback(valorExtra, paidAtMillis));
        } else {
            // Fallback com extras diretos
            preencherFallback(valorExtra, paidAtMillis);
        }

        findViewById(R.id.btnVoltarInicio).setOnClickListener(v -> {
            Intent i = new Intent(this, menu.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
        });
    }

    private void preencherComSnap(DocumentSnapshot d) {
        Double valor = d.getDouble("valor");
        Timestamp paidAt = d.getTimestamp("paidAt"); // definido no webhook
        tvValor.setText(br.format(valor == null ? 0.0 : valor));
        if (paidAt != null) {
            tvDataHora.setText(df.format(paidAt.toDate()));
        }
    }

    private void preencherFallback(double valor, long paidAtMillis) {
        tvValor.setText(br.format(valor));
        if (paidAtMillis > 0) {
            tvDataHora.setText(df.format(new java.util.Date(paidAtMillis)));
        }
    }
}
