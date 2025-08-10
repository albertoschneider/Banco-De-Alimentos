package com.instituto.bancodealimentos;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class telalogin extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_telalogin);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ImageButton imgBtnBack = findViewById(R.id.btn_back);
        Button btnLogin = findViewById(R.id.btn_login);
        TextView tvForgotPassword = findViewById(R.id.tv_forgot_password);
        TextView tvCreateAccount = findViewById(R.id.tv_create_account);

        // Botão login
        btnLogin.setOnClickListener(v -> {
            Intent intent = new Intent(telalogin.this, menu.class);
            startActivity(intent);
        });

        // Voltar para MainActivity
        imgBtnBack.setOnClickListener(v -> {
            Intent intent = new Intent(telalogin.this, MainActivity.class);
            startActivity(intent);
        });

        // Clique no "Esqueceu sua senha?"
        tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(telalogin.this, MainActivity.class);
            startActivity(intent);
        });

        // Clique no "Crie uma aqui"
        tvCreateAccount.setOnClickListener(v -> {
            Intent intent = new Intent(telalogin.this, telaregistro.class);
            startActivity(intent);
        });
    }
}
