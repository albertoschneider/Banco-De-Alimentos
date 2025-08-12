package com.instituto.bancodealimentos;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class doealimentos extends AppCompatActivity {

    RecyclerView recyclerView;
    ProdutoAdapter adapter;
    List<Produto> listaProdutos;
    TextView tvValorTotal;

    private final NumberFormat currencyBr = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doealimentos);

        tvValorTotal = findViewById(R.id.tvValorTotal);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2)); // 2 colunas

        listaProdutos = new ArrayList<>();
        listaProdutos.add(new Produto("Arroz Branco", 5.00, R.drawable.arroz_branco));
        listaProdutos.add(new Produto("Feijão Preto", 7.00, R.drawable.feijao_preto));
        listaProdutos.add(new Produto("Farinha de Trigo", 8.00, R.drawable.farinha_de_trigo));


        adapter = new ProdutoAdapter(
                this,
                listaProdutos,
                this::atualizarTotal
        );
        recyclerView.setAdapter(adapter);

        atualizarTotal();

        ImageButton imgBtnBack = findViewById(R.id.btn_voltar);

        imgBtnBack.setOnClickListener(v -> {
            Intent intent = new Intent(doealimentos.this, menu.class);
            startActivity(intent);
        });
    }

    private void atualizarTotal() {
        double total = 0.0;
        for (Produto p : listaProdutos) {
            total += p.getPreco() * p.getQuantidade();
        }
        tvValorTotal.setText(currencyBr.format(total));
    }
}
