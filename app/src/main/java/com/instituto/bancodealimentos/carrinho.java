// carrinho.java
package com.instituto.bancodealimentos;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

public class carrinho extends AppCompatActivity {

    private ArrayList<Produto> itens;
    private CarrinhoAdapter adapter;
    private TextView tvTotal;
    private final NumberFormat br = NumberFormat.getCurrencyInstance(new Locale("pt","BR"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Use o nome real do seu layout. Pelo seu XML anterior é "carrinho"
        setContentView(R.layout.activity_carrinho);

        tvTotal = findViewById(R.id.tvTotal);

        // Recebe a lista enviada pela outra Activity (API 33+ tem overload diferente)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            itens = getIntent().getParcelableArrayListExtra("itens", Produto.class);
        } else {
            itens = getIntent().getParcelableArrayListExtra("itens");
        }
        if (itens == null) itens = new ArrayList<>();

        Log.d("CARRINHO", "Recebidos " + itens.size() + " itens no carrinho");

        RecyclerView rv = findViewById(R.id.rvCarrinho);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CarrinhoAdapter(itens, this::atualizarTotal);
        rv.setAdapter(adapter);

        atualizarTotal();

        findViewById(R.id.btnVoltar).setOnClickListener(v -> {
            Intent intent = new Intent(carrinho.this, menu.class);
            startActivity(intent);
        });
        findViewById(R.id.btnContinuar).setOnClickListener(v -> {
            // próximo passo (pagamento/confirmacao)
        });
    }

    private void atualizarTotal() {
        double total = 0;
        for (Produto p : itens) {
            total += p.getPreco() * p.getQuantidade();
        }
        tvTotal.setText(br.format(total));
    }
}