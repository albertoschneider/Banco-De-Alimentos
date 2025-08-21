package com.instituto.bancodealimentos;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;

public class menu_admin extends AppCompatActivity {

    private TextView tvWelcome, tvName;
    private MaterialCardView cardAdmins, cardDoe, cardPontos, cardHistorico;
    private ImageButton btnSettings;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private ListenerRegistration userListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_admin);

        // Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Views
        tvWelcome = findViewById(R.id.tvWelcome);
        tvName = findViewById(R.id.tvName);
        btnSettings = findViewById(R.id.btnSettings);
        cardAdmins = findViewById(R.id.cardAdmins);
        cardDoe = findViewById(R.id.cardDoe);
        cardPontos = findViewById(R.id.cardPontos);
        cardHistorico = findViewById(R.id.cardHistorico);

        // Ações dos cards (ajuste as Activities conforme seu app)
        if (cardAdmins != null) {
            cardAdmins.setOnClickListener(v ->
                    startActivity(new Intent(menu_admin.this, gerenciar_admins.class)));
        }
        if (btnSettings != null) {
            // Pode abrir a mesma tela de gerenciar admins ou outra tela de configurações
            btnSettings.setOnClickListener(v ->
                    startActivity(new Intent(menu_admin.this, gerenciar_admins.class)));
        }
        if (cardDoe != null) {
            // Tela para gerenciar produtos (admin)
            cardDoe.setOnClickListener(v ->
                    startActivity(new Intent(menu_admin.this, admin_produtos.class)));
        }
        if (cardPontos != null) {
            // Coloque aqui a Activity da sua gestão de pontos de coleta
            cardPontos.setOnClickListener(v ->
                    Toast.makeText(this, "Pontos de coleta (em breve)", Toast.LENGTH_SHORT).show());
        }
        if (cardHistorico != null) {
            // Coloque aqui a Activity de histórico/admin
            cardHistorico.setOnClickListener(v ->
                    Toast.makeText(this, "Histórico (em breve)", Toast.LENGTH_SHORT).show());
        }

        // Preencher nome no header
        bindAdminNameToHeader();

        btnSettings = findViewById(R.id.btnSettings);

        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> openSettings(true)); // true = origem: admin
        }
    }

    @Override
    protected void onDestroy() {
        if (userListener != null) {
            userListener.remove();
            userListener = null;
        }
        super.onDestroy();
    }

    private void bindAdminNameToHeader() {
        FirebaseUser fu = auth.getCurrentUser();
        if (fu == null) {
            tvName.setText("");
            return;
        }
        final String uid = fu.getUid();

        // 1) Tenta ouvir em tempo real o doc de usuarios/{uid}
        userListener = db.collection("usuarios")
                .document(uid)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        // Em caso de erro, usa fallback rápido
                        tvName.setText(makeFallbackName(fu));
                        return;
                    }
                    if (snapshot != null && snapshot.exists()) {
                        String nome = snapshot.getString("nome");
                        if (!TextUtils.isEmpty(nome)) {
                            tvName.setText(nome);
                            return;
                        }
                        // Se veio vazio, tenta admins/{uid}
                        fetchNameFromAdminsThenFallback(uid, fu);
                    } else {
                        // Não existe usuarios/{uid}: tenta admins/{uid}
                        fetchNameFromAdminsThenFallback(uid, fu);
                    }
                });
    }

    private void openSettings(boolean fromAdmin) {
        FirebaseUser fu = auth.getCurrentUser();
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
        i.putExtra("from_admin", fromAdmin); // se quiser usar na tela de configs
        startActivity(i);
    }

    private void fetchNameFromAdminsThenFallback(String uid, FirebaseUser fu) {
        db.collection("admins").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        String nome = doc.getString("nome");
                        if (!TextUtils.isEmpty(nome)) {
                            tvName.setText(nome);
                            return;
                        }
                    }
                    // Último recurso: displayName → prefixo do email
                    tvName.setText(makeFallbackName(fu));
                })
                .addOnFailureListener(ignored ->
                        tvName.setText(makeFallbackName(fu)));
    }

    private String makeFallbackName(FirebaseUser fu) {
        if (fu == null) return "";
        if (!TextUtils.isEmpty(fu.getDisplayName())) {
            return fu.getDisplayName();
        }
        String email = fu.getEmail();
        if (!TextUtils.isEmpty(email) && email.contains("@")) {
            String first = email.substring(0, email.indexOf("@"));
            if (!first.isEmpty()) {
                // Capitaliza primeira letra
                return first.substring(0, 1).toUpperCase() + first.substring(1);
            }
        }
        return "Administrador";
    }
}