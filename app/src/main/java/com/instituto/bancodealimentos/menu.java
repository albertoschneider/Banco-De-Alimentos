package com.instituto.bancodealimentos;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import android.widget.ImageButton;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;

public class menu extends AppCompatActivity {

    private TextView tvName;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ImageButton btnSettings;

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

        // inicializa Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // pega referência do TextView
        tvName = findViewById(R.id.tvName);

        btnSettings = findViewById(R.id.btnSettings);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> openSettings(false)); // false = origem: usuário comum
        }

        // chama o método para buscar o nome
        carregarNomeUsuario();

        // --- seus cards continuam iguais ---
        MaterialCardView cardPontos = findViewById(R.id.cardPontos);
        MaterialCardView cardDoe = findViewById(R.id.cardDoe);
        MaterialCardView cardVoluntario = findViewById(R.id.cardVoluntario);
        MaterialCardView cardHistorico = findViewById(R.id.cardHistorico);
        MaterialCardView cardCarrinho = findViewById(R.id.cardCarrinho);

        cardPontos.setOnClickListener(v -> startActivity(new Intent(menu.this, pontosdecoleta.class)));
        cardDoe.setOnClickListener(v -> startActivity(new Intent(menu.this, doealimentos.class)));
        cardVoluntario.setOnClickListener(v -> startActivity(new Intent(menu.this, voluntariar.class)));
        cardHistorico.setOnClickListener(v -> startActivity(new Intent(menu.this, pedidos.class)));
        cardCarrinho.setOnClickListener(v -> startActivity(new Intent(menu.this, carrinho.class)));
    }

    private void carregarNomeUsuario() {
        if (mAuth.getCurrentUser() == null) {
            tvName.setText("Usuário não logado");
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();
        DocumentReference docRef = db.collection("usuarios").document(uid);

        docRef.get().addOnSuccessListener(document -> {
            if (document.exists()) {
                String nome = document.getString("nome");
                if (nome != null && !nome.isEmpty()) {
                    tvName.setText(nome);
                } else {
                    tvName.setText("Nome não definido");
                }
            } else {
                tvName.setText("Usuário não encontrado");
            }
        }).addOnFailureListener(e -> tvName.setText("Erro ao carregar nome"));
    }

    private void openSettings(boolean fromAdmin) {
        FirebaseUser fu = mAuth.getCurrentUser();
        if (fu == null) {
            startActivity(new Intent(this, MainActivity.class));
            return;
        }

        boolean hasGoogle = false, hasPassword = false;
        for (UserInfo info : fu.getProviderData()) {
            String p = info.getProviderId();
            if ("google.com".equals(p)) hasGoogle = true;
            if ("password".equals(p))   hasPassword = true;
        }

        Class<?> next = hasPassword ? configuracoes_email_senha.class : configuracoes_google.class;
        Intent i = new Intent(this, next);
        i.putExtra("from_admin", fromAdmin); // apenas informativo, se quiser usar
        startActivity(i);
    }
}
