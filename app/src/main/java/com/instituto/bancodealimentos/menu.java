package com.instituto.bancodealimentos;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

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
        setContentView(R.layout.activity_menu);

        // Aplicar insets no scroll view
        WindowInsetsHelper.applyTopAndBottomInsets(findViewById(R.id.scroll));

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
                } else {
                    tryDisplayName(fu);
                }
            } else {
                tryDisplayName(fu);
            }
        }).addOnFailureListener(e -> {
            tryDisplayName(fu);
        });
    }

    private void tryDisplayName(FirebaseUser fu) {
        String dn = fu.getDisplayName();
        if (dn != null && !dn.isEmpty()) {
            tvName.setText(dn);
        } else {
            for (UserInfo ui : fu.getProviderData()) {
                String displayName = ui.getDisplayName();
                if (displayName != null && !displayName.isEmpty()) {
                    tvName.setText(displayName);
                    return;
                }
            }
            tvName.setText("Usuário");
        }
    }

    private void openSettings(boolean isAdmin) {
        Intent intent;
        FirebaseUser fu = mAuth.getCurrentUser();
        if (fu == null) return;

        boolean isGoogleUser = false;
        for (UserInfo profile : fu.getProviderData()) {
            if ("google.com".equals(profile.getProviderId())) {
                isGoogleUser = true;
                break;
            }
        }

        if (isGoogleUser) {
            intent = new Intent(this, configuracoes_google.class);
        } else {
            intent = new Intent(this, configuracoes_email_senha.class);
        }
        intent.putExtra("IS_ADMIN", isAdmin);
        startActivity(intent);
    }
}