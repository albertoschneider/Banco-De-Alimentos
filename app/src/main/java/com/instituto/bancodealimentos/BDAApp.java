package com.instituto.bancodealimentos;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.PersistentCacheSettings;

public class BDAApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Força modo claro
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        // Firebase init + cache Firestore
        FirebaseApp.initializeApp(this);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        PersistentCacheSettings cache =
                PersistentCacheSettings.newBuilder()
                        .setSizeBytes(40L * 1024L * 1024L) // 40MB
                        .build();

        FirebaseFirestoreSettings settings =
                new FirebaseFirestoreSettings.Builder()
                        .setLocalCacheSettings(cache)
                        .build();

        db.setFirestoreSettings(settings);

        // >>> MIGRAÇÃO 1x dos valores antigos (strings.xml) para os Settings:
        // Isso garante que o app já use as chaves PIX/Whats antigas enquanto você não trocar na tela.
        InitDefaults.ensureLegacyPixAndWhatsMigrated(this);
    }
}
