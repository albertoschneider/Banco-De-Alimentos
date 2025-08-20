package com.instituto.bancodealimentos;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

public class carrinho extends AppCompatActivity {

    private ArrayList<Produto> itens = new ArrayList<>();
    private CarrinhoAdapter adapter;
    private TextView tvTotal;
    private final NumberFormat br = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_carrinho);

        tvTotal = findViewById(R.id.tvTotal);

        // Carrega do storage central
        itens = CartStore.load(this);

        RecyclerView rv = findViewById(R.id.rvCarrinho);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CarrinhoAdapter(itens, () -> {
            atualizarTotal();
            CartStore.save(this, itens);
        });
        rv.setAdapter(adapter);

        atualizarTotal();

        // Voltar para o "menu" (troque se essa Activity não existir)
        findViewById(R.id.btnVoltar).setOnClickListener(v -> {
            Intent i = new Intent(carrinho.this, menu.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
        });

        // Voltar ao catálogo
        findViewById(R.id.btn_voltar).setOnClickListener(v -> {
            Intent i = new Intent(carrinho.this, doealimentos.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
        });

        // Continuar/pagamento
        findViewById(R.id.btnContinuar).setOnClickListener(v -> {
            double total = 0;
            for (Produto p : itens) {
                total += safePreco(p) * safeQtd(p);
            }
            Intent intent = new Intent(carrinho.this, pagamento.class);
            intent.putExtra("total", total);
            startActivity(intent);
        });
    }

    private void atualizarTotal() {
        double total = 0;
        for (Produto p : itens) total += safePreco(p) * safeQtd(p);
        tvTotal.setText(br.format(total));
    }

    // --- helpers robustos (funcionam mesmo sem getters explícitos) ---
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
        return 1; // mude para 0 se preferir
    }
}