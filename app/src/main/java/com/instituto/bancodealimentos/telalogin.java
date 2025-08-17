package com.instituto.bancodealimentos;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class telalogin extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private EditText edtEmail, edtSenha;

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

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Inputs
        edtEmail = findViewById(R.id.edtEmail);
        edtSenha = findViewById(R.id.edtSenha);

        // Botões
        ImageButton imgBtnBack = findViewById(R.id.btnBack);
        Button btnLogin = findViewById(R.id.btnEntrar);
        TextView tvForgotPassword = findViewById(R.id.txtForgot);
        TextView tvCreateAccount = findViewById(R.id.txtRegister);

        btnLogin.setOnClickListener(v -> loginUsuario());

        imgBtnBack.setOnClickListener(v -> {
            Intent intent = new Intent(telalogin.this, MainActivity.class);
            startActivity(intent);
        });

        tvForgotPassword.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(this, "Digite seu e-mail para recuperar a senha.", Toast.LENGTH_SHORT).show();
            } else {
                mAuth.sendPasswordResetEmail(email)
                        .addOnSuccessListener(aVoid -> Toast.makeText(this, "E-mail de redefinição enviado!", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(this, "Erro: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });

        tvCreateAccount.setOnClickListener(v -> {
            Intent intent = new Intent(telalogin.this, telaregistro.class);
            startActivity(intent);
        });
    }

    private void loginUsuario() {
        String email = edtEmail.getText().toString().trim();
        String senha = edtSenha.getText().toString().trim();

        if (email.isEmpty() || senha.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos!", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, senha)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Login realizado com sucesso!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(telalogin.this, menu.class));
                        finish();
                    } else {
                        Toast.makeText(this, "Erro no login: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
