package com.instituto.bancodealimentos;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.card.MaterialCardView;

public class menu extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_menu);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        MaterialCardView cardPontos = findViewById(R.id.cardPontos);
        MaterialCardView cardDoe = findViewById(R.id.cardDoe);
        MaterialCardView cardVoluntario = findViewById(R.id.cardVoluntario);
        MaterialCardView cardHistorico = findViewById(R.id.cardHistorico);
        MaterialCardView cardCarrinho = findViewById(R.id.cardCarrinho);

        cardPontos.setOnClickListener(v -> {
            startActivity(new Intent(menu.this, pontosdecoleta.class));
        });

        cardDoe.setOnClickListener(v -> {
            startActivity(new Intent(menu.this, doealimentos.class));
        });

        cardVoluntario.setOnClickListener(v -> {
            startActivity(new Intent(menu.this, voluntariar.class));
        });

        cardHistorico.setOnClickListener(v -> {
            startActivity(new Intent(menu.this, pedidos.class));
        });

        cardCarrinho.setOnClickListener(v -> {
            startActivity(new Intent(menu.this, carrinho.class));
        });
    }
}