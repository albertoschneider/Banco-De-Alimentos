package com.instituto.bancodealimentos;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.PersistentCacheSettings;

public class BDAApp extends Application {

    private static final String TAG = "BDA_APP";

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

        // Gate central com proteção para fluxos de autenticação
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity a, Bundle b) {
                String activityName = a.getClass().getName();
                Log.d(TAG, "Activity criada: " + activityName);

                // CRÍTICO: Ignora Activities do Google Sign-In e outros fluxos de auth
                // Estas Activities são internas do Google Play Services e não devem ser bloqueadas
                if (activityName.contains("google") ||
                        activityName.contains("gms") ||
                        activityName.contains("auth.api") ||
                        activityName.contains("SignInHubActivity") ||
                        activityName.contains("GrantCredentialsPermission")) {
                    Log.d(TAG, "Activity de autenticação detectada - permitindo sem verificação");
                    return;
                }

                // Lista de Activities do SEU app que podem abrir sem sessão
                boolean isAllowed =
                        a instanceof SplashActivity ||
                                a instanceof MainActivity ||
                                a instanceof telalogin ||
                                a instanceof telaregistro ||
                                a instanceof EsqueciSenhaActivity ||
                                a instanceof EmailActionRouterActivity ||
                                a instanceof DeepLinkSuccessActivity ||
                                a instanceof AuthBridgeActivity ||
                                a instanceof ResetarSenhaActivity ||
                                a instanceof EmailAtualizadoSucessoActivity ||
                                a instanceof NovoEmailActivity;

                if (isAllowed) {
                    Log.d(TAG, "Activity permitida sem sessão: " + activityName);
                    return;
                }

                // Para as demais: se sessão está nula → Splash decide rota
                if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                    Log.w(TAG, "Sessão nula detectada em " + activityName + " - redirecionando para Splash");
                    Intent it = new Intent(a, SplashActivity.class);
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    a.startActivity(it);
                    a.finish();
                } else {
                    Log.d(TAG, "Usuário logado detectado em " + activityName);
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