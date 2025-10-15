package com.instituto.bancodealimentos;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Splash "fonte única da verdade" de navegação:
 * - Espera ~350ms para a sessão estabilizar (refresh de token).
 * - Se não logado => MainActivity (seu login atual).
 * - Se logado => checa admins/{uid} UMA vez (get()) e decide menu_admin ou menu.
 * - Sempre limpa a pilha para evitar voltar ao splash.
 */
public class SplashActivity extends AppCompatActivity {

    private final Handler h = new Handler(Looper.getMainLooper());
    private boolean routed = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);

        // Pequeno atraso para evitar corrida de autenticação
        h.postDelayed(this::routeOnce, 350);
    }

    private void routeOnce() {
        if (routed || isFinishing() || isDestroyed()) return;
        routed = true;

        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) {
            // NÃO logado -> sua tela de login atual (mantive MainActivity como no seu código)
            Intent it = new Intent(this, MainActivity.class);
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(it);
            return;
        }

        // Logado -> checa se é admin UMA vez (sem listeners)
        FirebaseFirestore.getInstance()
                .collection("admins").document(u.getUid())
                .get()
                .addOnSuccessListener(snap -> {
                    Class<?> target = (snap != null && snap.exists()) ? menu_admin.class : menu.class;
                    Intent it = new Intent(this, target);
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(it);
                })
                .addOnFailureListener(e -> {
                    // Em caso de falha de rede/latência, não trava o usuário: vai para menu comum
                    Intent it = new Intent(this, menu.class);
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(it);
                });
    }
}
