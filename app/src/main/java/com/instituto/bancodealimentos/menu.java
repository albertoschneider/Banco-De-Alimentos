package com.instituto.bancodealimentos;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;

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
        ImageButton btnCart = findViewById(R.id.btnCart);

        cardPontos.setOnClickListener(v -> {
            Intent i = new Intent(menu.this, pontosdecoleta.class);
            startActivity(i);
        });

        cardDoe.setOnClickListener(v -> {
            Intent i = new Intent(menu.this, doealimentos.class);
            startActivity(i);
        });

        cardVoluntario.setOnClickListener(v -> {
            Intent i = new Intent(menu.this, voluntariar.class);
            startActivity(i);
        });

        cardHistorico.setOnClickListener(v -> {
            Intent i = new Intent(menu.this, pedidos.class);
            startActivity(i);
        });

        btnCart.setOnClickListener(v -> {
            Intent i = new Intent(menu.this, carrinho.class);
            startActivity(i);
        });
    }
}