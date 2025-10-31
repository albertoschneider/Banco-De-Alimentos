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

import com.google.firebase.auth.UserInfo;

public class menu_admin extends AppCompatActivity {

    private TextView tvWelcome, tvName;
    private MaterialCardView cardAdmins, cardDoe, cardPontos, cardHistorico, cardChaves;
    private ImageButton btnSettings;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private ListenerRegistration userListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowInsetsHelper.setupEdgeToEdge(this);
        setContentView(R.layout.activity_menu_admin);

        // Aplicar insets no scroll view
        WindowInsetsHelper.applyTopAndBottomInsets(findViewById(R.id.scroll));

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
        cardChaves = findViewById(R.id.cardChaves);

        // Ações dos cards (ajuste as Activities conforme seu app)
        if (cardAdmins != null) {
            cardAdmins.setOnClickListener(v ->
                    startActivity(new Intent(menu_admin.this, gerenciar_admins.class)));
        }
        
        // Botão de configurações - detecta o tipo de autenticação e redireciona
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> abrirConfiguracoes());
        }
        
        if (cardDoe != null) {
            // Tela para gerenciar produtos (admin)
            cardDoe.setOnClickListener(v ->
                    startActivity(new Intent(menu_admin.this, admin_produtos.class)));
        }
        if (cardPontos != null) {
            // Coloque aqui a Activity da sua gestão de pontos de coleta
            cardPontos.setOnClickListener(v ->
                    startActivity(new Intent(menu_admin.this, gerenciar_pontosdecoleta.class)));
        }
        if (cardHistorico != null) {
            // Coloque aqui a Activity de histórico/admin
            cardHistorico.setOnClickListener(v ->
                    startActivity(new Intent(menu_admin.this, AdminDoacoesActivity.class)));
        }
        if (cardChaves != null) {

            // Chaves PIX e WhatsApp
            cardChaves.setOnClickListener(v ->
                    startActivity(new Intent(menu_admin.this, AlterarPixWhatsappActivity.class)));
        }

        // Carrega o nome do admin
        loadAdminName();
    }

    /**
     * Abre a tela de configurações apropriada baseada no método de autenticação
     */
    private void abrirConfiguracoes() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Usuário não autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        // Verifica se o usuário usa autenticação Google
        boolean isGoogleAuth = false;
        for (UserInfo profile : user.getProviderData()) {
            if ("google.com".equals(profile.getProviderId())) {
                isGoogleAuth = true;
                break;
            }
        }

        // Redireciona para a tela apropriada
        Intent intent;
        if (isGoogleAuth) {
            intent = new Intent(menu_admin.this, configuracoes_google.class);
        } else {
            intent = new Intent(menu_admin.this, configuracoes_email_senha.class);
        }
        startActivity(intent);
    }

    private void loadAdminName() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            tvName.setText("Admin");
            return;
        }

        String uid = user.getUid();

        // Escuta o documento do admin em tempo real
        userListener = db.collection("usuarios").document(uid)
                .addSnapshotListener((doc, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Erro ao carregar dados: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (doc != null && doc.exists()) {
                        String nome = doc.getString("nome");
                        if (!TextUtils.isEmpty(nome)) {
                            tvName.setText(nome);
                        } else {
                            tryDisplayName(user);
                        }
                    } else {
                        tryDisplayName(user);
                    }
                });
    }

    private void tryDisplayName(FirebaseUser user) {
        String displayName = user.getDisplayName();
        if (!TextUtils.isEmpty(displayName)) {
            tvName.setText(displayName);
        } else {
            // Tenta pegar de qualquer provider
            for (UserInfo profile : user.getProviderData()) {
                String name = profile.getDisplayName();
                if (!TextUtils.isEmpty(name)) {
                    tvName.setText(name);
                    return;
                }
            }
            tvName.setText("Admin");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Recarrega o nome quando a Activity volta ao foco
        FirebaseUser user = auth.getCurrentUser();
        if (user != null && userListener == null) {
            loadAdminName();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Remove o listener ao sair da Activity
        if (userListener != null) {
            userListener.remove();
            userListener = null;
        }
    }

    /**
     * Verifica se o usuário atual é admin.
     * Se não for, volta pra tela de login (opcional)
     */
    private void checkAdminPermission() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            finish();
            return;
        }

        String uid = user.getUid();
        db.collection("usuarios").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Boolean isAdmin = doc.getBoolean("isAdmin");
                        if (isAdmin == null || !isAdmin) {
                            Toast.makeText(this, "Você não tem permissão de admin!", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } else {
                        Toast.makeText(this, "Usuário não encontrado!", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erro ao verificar permissões: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    finish();
                });
    }
}
