package com.instituto.bancodealimentos;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class carrinho extends AppCompatActivity {

    private ArrayList<Produto> itens = new ArrayList<>();
    private CarrinhoAdapter adapter;
    private TextView tvTotal;
    private final NumberFormat br = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ---- Edge-to-edge estável (desenha sob status bar) ----
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        setContentView(R.layout.activity_carrinho);

        // Header recebe exatamente a altura da status bar (SEM somar de novo)
        View header = findViewById(R.id.header);
        if (header != null) {
            ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
                Insets sb = insets.getInsets(WindowInsetsCompat.Type.statusBars());
                v.setPadding(v.getPaddingLeft(), sb.top, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
            ViewCompat.requestApplyInsets(header);

            // Ícones escuros na status bar (ajuste para false se o header for escuro)
            WindowInsetsControllerCompat c =
                    ViewCompat.getWindowInsetsController(getWindow().getDecorView());
            if (c != null) c.setAppearanceLightStatusBars(true);
        }

        // (Opcional) Empurra o root pelo tamanho da barra de navegação inferior
        View root = findViewById(R.id.main); // use o id do root do seu layout
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets nb = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), nb.bottom);
                return insets;
            });
            ViewCompat.requestApplyInsets(root);
        }

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

        // Voltar para o "menu"
        View btnVoltarTopo = findViewById(R.id.btn_voltar);
        if (btnVoltarTopo != null) {
            btnVoltarTopo.setOnClickListener(v -> {
                Intent i = new Intent(carrinho.this, doealimentos.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(i);
                finish();
            });
        }

        View btnVoltar = findViewById(R.id.btnVoltar);
        if (btnVoltar != null) {
            btnVoltar.setOnClickListener(v -> {
                Intent i = new Intent(carrinho.this, menu.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(i);
                finish();
            });
        }

        // Continuar/pagamento
        View btnContinuar = findViewById(R.id.btnContinuar);
        if (btnContinuar != null) {
            btnContinuar.setOnClickListener(v -> {
                double total = 0;
                for (Produto p : itens) total += safePreco(p) * safeQtd(p);
                Intent intent = new Intent(carrinho.this, pagamento.class);
                intent.putExtra("total", total);
                startActivity(intent);
            });
        }
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