package com.instituto.bancodealimentos;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SplashActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1) Checa sessão
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) {
            // não logado -> vai pro login
            startActivity(new Intent(this, telalogin.class));
            finish();
            return;
        }

        // 2) Logado -> checa admins/{uid}
        FirebaseFirestore.getInstance()
                .collection("admins").document(u.getUid())
                .get()
                .addOnSuccessListener(snap -> {
                    boolean isAdmin = snap != null && snap.exists();
                    Intent it = new Intent(this, isAdmin ? menu_admin.class : menu.class);
                    it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(it);
                    finish();
                })
                .addOnFailureListener(e -> {
                    // fallback: usuário comum
                    startActivity(new Intent(this, menu.class));
                    finish();
                });
    }
}