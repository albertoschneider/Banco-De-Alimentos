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

public class SplashActivity extends AppCompatActivity {

    private final Handler h = new Handler(Looper.getMainLooper());
    private boolean routed = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);

        // *** IMPORTANTE ***
        // Se a Splash foi apenas "trazida à frente" (ex.: na volta de outra Activity),
        // NÃO faça nenhum roteamento; apenas encerre para devolver o foco à tela anterior.
        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            finish();
            return;
        }

        // Pequeno atraso para estabilizar sessão
        h.postDelayed(this::routeOnce, 350);
    }

    private void routeOnce() {
        if (routed || isFinishing() || isDestroyed()) return;
        routed = true;

        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) {
            Intent it = new Intent(this, MainActivity.class);
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(it);
            return;
        }

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
                    Intent it = new Intent(this, menu.class);
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(it);
                });
    }
}
