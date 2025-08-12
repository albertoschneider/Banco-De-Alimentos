// carrinho.java
package com.instituto.bancodealimentos;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

public class carrinho extends AppCompatActivity {

    private static final String PREFS = "MeuApp";
    private static final String KEY_CART = "carrinho";

    private ArrayList<Produto> itens;
    private CarrinhoAdapter adapter;
    private TextView tvTotal;
    private final NumberFormat br = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_carrinho);

        tvTotal = findViewById(R.id.tvTotal);

        // 1) Tenta carregar do SharedPreferences
        ArrayList<Produto> salvos = loadCart();

        // 2) Se veio algo pela Intent (da tela de doação), usa isso e salva
        ArrayList<Produto> fromIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            fromIntent = getIntent().getParcelableArrayListExtra("itens", Produto.class);
        } else {
            fromIntent = getIntent().getParcelableArrayListExtra("itens");
        }

        if (fromIntent != null && !fromIntent.isEmpty()) {
            itens = fromIntent;
            saveCart(itens);
        } else if (salvos != null) {
            itens = salvos;
        } else {
            itens = new ArrayList<>();
        }

        RecyclerView rv = findViewById(R.id.rvCarrinho);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CarrinhoAdapter(itens, () -> {
            atualizarTotal();
            saveCart(itens); // persiste a cada mudança de quantidade
        });
        rv.setAdapter(adapter);

        atualizarTotal();

        findViewById(R.id.btnVoltar).setOnClickListener(v -> {
            Intent i = new Intent(carrinho.this, menu.class); // confira o nome da classe!
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
        });

        findViewById(R.id.btn_voltar).setOnClickListener(v -> {
            Intent i = new Intent(carrinho.this, doealimentos.class); // confira o nome da classe!
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
        });

        findViewById(R.id.btnContinuar).setOnClickListener(v -> {
            // próximo passo (pagamento/confirmacao)
            // Ex.: startActivity(new Intent(this, Pagamento.class));
        });
    }

    private void atualizarTotal() {
        double total = 0;
        for (Produto p : itens) {
            total += p.getPreco() * p.getQuantidade();
        }
        tvTotal.setText(br.format(total));
    }

    // --------- Persistência simples com SharedPreferences + Gson ---------

    private void saveCart(ArrayList<Produto> lista) {
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        String json = new Gson().toJson(lista);
        sp.edit().putString(KEY_CART, json).apply();
    }

    private ArrayList<Produto> loadCart() {
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        String json = sp.getString(KEY_CART, null);
        if (json == null) return null;
        Type type = new TypeToken<ArrayList<Produto>>() {}.getType();
        return new Gson().fromJson(json, type);
    }
}