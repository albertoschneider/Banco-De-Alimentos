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

import java.util.HashMap;
import java.util.Map;

public class telaregistro extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private EditText edtNome, edtEmail, edtSenha, edtConfirmarSenha;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_telaregistro);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Inputs
        edtNome = findViewById(R.id.et_username);
        edtEmail = findViewById(R.id.et_email);
        edtSenha = findViewById(R.id.et_password);
        edtConfirmarSenha = findViewById(R.id.et_confirm_password);

        // Botões
        ImageButton imgBtnBack = findViewById(R.id.btn_back);
        TextView btnLogin = findViewById(R.id.tv_login_here);
        Button btnRegistro = findViewById(R.id.btn_login);

        // Registro
        btnRegistro.setOnClickListener(v -> registrarUsuario());

        // Voltar
        btnLogin.setOnClickListener(v -> {
            Intent intent = new Intent(telaregistro.this, telalogin.class);
            startActivity(intent);
        });

        imgBtnBack.setOnClickListener(v -> {
            Intent intent = new Intent(telaregistro.this, MainActivity.class);
            startActivity(intent);
        });
    }

    private void registrarUsuario() {
        String nome = edtNome.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String senha = edtSenha.getText().toString().trim();
        String confirmar = edtConfirmarSenha.getText().toString().trim();

        if (nome.isEmpty() || email.isEmpty() || senha.isEmpty() || confirmar.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!senha.equals(confirmar)) {
            Toast.makeText(this, "As senhas não coincidem!", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, senha)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String uid = mAuth.getCurrentUser().getUid();

                        Map<String, Object> usuario = new HashMap<>();
                        usuario.put("nome", nome);
                        usuario.put("email", email);

                        db.collection("usuarios").document(uid).set(usuario)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Cadastro realizado com sucesso!", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(telaregistro.this, menu.class));
                                    finish();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Erro ao salvar no banco: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    } else {
                        Toast.makeText(this, "Erro no cadastro: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
