package com.instituto.bancodealimentos;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class menu extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_menu); // Corrigido para o layout correto

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Agora sim, já que o layout foi carregado
        LinearLayout card1 = findViewById(R.id.card1);
        LinearLayout card2 = findViewById(R.id.card2);
        LinearLayout card3 = findViewById(R.id.card3);

        card1.setOnClickListener(v -> {
            Intent intent = new Intent(menu.this, pontosdecoleta.class);
            startActivity(intent);
        });

        card2.setOnClickListener(v -> {
            Intent intent = new Intent(menu.this, doealimentos.class);
            startActivity(intent);
        });

        card3.setOnClickListener(v -> {
            Intent intent = new Intent(menu.this, voluntariar.class);
            startActivity(intent);
        });
    }
}
