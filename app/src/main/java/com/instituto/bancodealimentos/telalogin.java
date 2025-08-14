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

        ImageButton imgBtnBack = findViewById(R.id.btnBack);
        Button btnLogin = findViewById(R.id.btnEntrar);
        TextView tvForgotPassword = findViewById(R.id.txtForgot);
        TextView tvCreateAccount = findViewById(R.id.txtRegister);

        btnLogin.setOnClickListener(v -> {
            Intent intent = new Intent(telalogin.this, menu.class);
            startActivity(intent);
        });

        imgBtnBack.setOnClickListener(v -> {
            Intent intent = new Intent(telalogin.this, MainActivity.class);
            startActivity(intent);
        });

        tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(telalogin.this, MainActivity.class);
            startActivity(intent);
        });

        tvCreateAccount.setOnClickListener(v -> {
            Intent intent = new Intent(telalogin.this, telaregistro.class);
            startActivity(intent);
        });
    }
}
