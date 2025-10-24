package com.instituto.bancodealimentos;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SPLASH";
    private final Handler h = new Handler(Looper.getMainLooper());
    private boolean routed = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate()");

        // Se foi trazida à frente, não fazer nada
        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            Log.d(TAG, "BROUGHT_TO_FRONT detectado - finalizando");
            finish();
            return;
        }

        // Pequeno delay para estabilizar
        h.postDelayed(this::routeOnce, 350);
    }

    private void routeOnce() {
        if (routed || isFinishing() || isDestroyed()) {
            return;
        }
        routed = true;

        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        Log.d(TAG, "Usuário: " + (u != null ? u.getUid() : "null"));

        if (u == null) {
            // Sem usuário → MainActivity
            Log.d(TAG, "Sem usuário - navegando para MainActivity");
            Intent it = new Intent(this, MainActivity.class);
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(it);
            finish();
            return;
        }

        // Com usuário → checar se é admin
        Log.d(TAG, "Usuário logado - checando se é admin");
        FirebaseFirestore.getInstance()
                .collection("admins").document(u.getUid())
                .get()
                .addOnSuccessListener(snap -> {
                    Class<?> target = (snap != null && snap.exists()) ? menu_admin.class : menu.class;
                    Log.d(TAG, "Navegando para " + target.getSimpleName());
                    Intent it = new Intent(this, target);
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(it);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao checar admin: " + e.getMessage());
                    Intent it = new Intent(this, menu.class);
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(it);
                    finish();
                });
    }
}