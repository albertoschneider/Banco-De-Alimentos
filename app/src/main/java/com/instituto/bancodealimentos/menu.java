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
        WindowInsetsHelper.setupEdgeToEdge(this);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_menu);

        // Aplicar insets
        WindowInsetsHelper.applyTopAndBottomInsets(findViewById(R.id.scroll));

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tvName = findViewById(R.id.tvName);
        btnSettings = findViewById(R.id.btnSettings);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> openSettings(false));
        }

        carregarNomeUsuario();

        MaterialCardView cardPontos = findViewById(R.id.cardPontos);
        MaterialCardView cardDoe = findViewById(R.id.cardDoe);
        MaterialCardView cardVoluntario = findViewById(R.id.cardVoluntario);
        MaterialCardView cardHistorico = findViewById(R.id.cardHistorico);
        MaterialCardView cardCarrinho = findViewById(R.id.cardCarrinho);

        cardPontos.setOnClickListener(v -> startActivity(new Intent(menu.this, pontosdecoleta.class)));
        cardDoe.setOnClickListener(v -> startActivity(new Intent(menu.this, doealimentos.class)));
        cardVoluntario.setOnClickListener(v -> startActivity(new Intent(menu.this, voluntariar.class)));
        cardHistorico.setOnClickListener(v -> startActivity(new Intent(menu.this, HistoricoDoacoesActivity.class)));
        cardCarrinho.setOnClickListener(v -> startActivity(new Intent(menu.this, carrinho.class)));
    }

    private void carregarNomeUsuario() {
        FirebaseUser fu = mAuth.getCurrentUser();
        if (fu == null) { tvName.setText("Usuário não logado"); return; }

        String uid = fu.getUid();
        DocumentReference docRef = db.collection("usuarios").document(uid);

        docRef.get().addOnSuccessListener(document -> {
            if (document.exists()) {
                String nome = document.getString("nome");
                if (nome != null && !nome.isEmpty()) {
                    tvName.setText(nome);
                    return;
                }
            }
            // Fallback
            if (fu.getDisplayName() != null && !fu.getDisplayName().isEmpty()) {
                tvName.setText(fu.getDisplayName());
            } else if (fu.getEmail() != null && fu.getEmail().contains("@")) {
                String first = fu.getEmail().substring(0, fu.getEmail().indexOf("@"));
                tvName.setText(first.substring(0,1).toUpperCase() + first.substring(1));
            } else {
                tvName.setText("Nome não definido");
            }
        }).addOnFailureListener(e -> tvName.setText("Erro ao carregar nome"));
    }

    private void openSettings(boolean fromAdmin) {
        FirebaseUser fu = mAuth.getCurrentUser();
        if (fu == null) {
            // Deixa o Splash decidir a rota correta (login x menu)
            startActivity(new Intent(this, SplashActivity.class));
            finish();
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
        i.putExtra("from_admin", fromAdmin);
        startActivity(i);
    }

}