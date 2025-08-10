package com.instituto.bancodealimentos;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class telaregistro extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // Corrigido: agora carrega o layout certo
        setContentView(R.layout.activity_telaregistro);

        // Ajuste do insets para evitar sobreposição em barras do sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Botões e textos
        TextView btnLogin = findViewById(R.id.tv_login_here);
        Button btnRegistro = findViewById(R.id.btn_login);

        // Clique no botão de registrar
        btnRegistro.setOnClickListener(v -> {
            Intent intent = new Intent(telaregistro.this, menu.class);
            startActivity(intent);
        });

        // Clique para voltar para a tela de login
        btnLogin.setOnClickListener(v -> {
            Intent intent = new Intent(telaregistro.this, MainActivity.class);
            startActivity(intent);
        });
    }
}
