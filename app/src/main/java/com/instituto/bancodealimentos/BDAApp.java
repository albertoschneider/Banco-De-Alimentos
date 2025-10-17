package com.instituto.bancodealimentos;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.PersistentCacheSettings;

import java.io.File;
import java.io.FileWriter;

public class BDAApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Modo claro sempre
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

        // 2) Gate central: se qualquer Activity abrir sem usuário, manda para Splash
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override public void onActivityCreated(@NonNull Activity a, Bundle b) {
                // Telas que podem abrir sem sessão
                boolean isAllowed =
                        a instanceof SplashActivity ||
                                a instanceof MainActivity ||
                                a instanceof telalogin ||
                                a instanceof telaregistro ||
                                a instanceof EsqueciSenhaActivity ||
                                a instanceof EmailActionRouterActivity ||
                                a instanceof DeepLinkSuccessActivity;
                if (isAllowed) return;

                // Para as demais: se sessão está nula → Splash decide rota
                if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                    Intent it = new Intent(a, SplashActivity.class);
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    a.startActivity(it);
                    a.finish();
                }
            }
            @Override public void onActivityStarted(@NonNull Activity a) {}
            @Override public void onActivityResumed(@NonNull Activity a) {}
            @Override public void onActivityPaused(@NonNull Activity a) {}
            @Override public void onActivityStopped(@NonNull Activity a) {}
            @Override public void onActivitySaveInstanceState(@NonNull Activity a, @NonNull Bundle b) {}
            @Override public void onActivityDestroyed(@NonNull Activity a) {}
        });
    }
}
