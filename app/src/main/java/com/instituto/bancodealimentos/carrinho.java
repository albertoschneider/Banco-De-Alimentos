package com.instituto.bancodealimentos;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
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
    private TextView tvErroVazio;
    private final NumberFormat br = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        setContentView(R.layout.activity_carrinho);

        final View header = findViewById(R.id.header);
        final View footer = findViewById(R.id.footer);

        // Inset fix: margem no topo e embaixo
        ViewCompat.setOnApplyWindowInsetsListener(getWindow().getDecorView(), (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            Insets nb = insets.getInsets(WindowInsetsCompat.Type.navigationBars());

            if (header != null) {
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) header.getLayoutParams();
                if (lp.topMargin != sb.top) {
                    lp.topMargin = sb.top;
                    header.setLayoutParams(lp);
                }
            }
            if (footer != null) {
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) footer.getLayoutParams();
                if (lp.bottomMargin != nb.bottom) {
                    lp.bottomMargin = nb.bottom;
                    footer.setLayoutParams(lp);
                }
            }

            WindowInsetsControllerCompat c = ViewCompat.getWindowInsetsController(getWindow().getDecorView());
            if (c != null) c.setAppearanceLightStatusBars(true);
            return insets;
        });
        ViewCompat.requestApplyInsets(getWindow().getDecorView());

        tvTotal = findViewById(R.id.tvTotal);
        tvErroVazio = findViewById(R.id.tvErroVazio);

        itens = CartStore.load(this);

        RecyclerView rv = findViewById(R.id.rvCarrinho);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CarrinhoAdapter(itens, () -> {
            atualizarTotal();
            CartStore.save(this, itens);
        });
        rv.setAdapter(adapter);

        atualizarTotal();

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

        View btnContinuar = findViewById(R.id.btnContinuar);
        if (btnContinuar != null) {
            btnContinuar.setOnClickListener(v -> {
                if (isCartEmpty()) {
                    if (tvErroVazio != null) tvErroVazio.setVisibility(View.VISIBLE);
                    return;
                }
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
        int somaQtd = 0;
        for (Produto p : itens) {
            int q = safeQtd(p);
            somaQtd += q;
            total += safePreco(p) * q;
        }
        tvTotal.setText(br.format(total));
        if (tvErroVazio != null) tvErroVazio.setVisibility(somaQtd <= 0 ? View.VISIBLE : View.GONE);
    }

    private boolean isCartEmpty() {
        for (Produto p : itens) if (safeQtd(p) > 0) return false;
        return true;
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
        return 0;
    }
}
