package com.instituto.bancodealimentos;

import android.app.Activity;
import android.view.View;

import androidx.annotation.MainThread;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Portão de sessão:
 * - Espera a autenticação estabilizar (FirebaseAuth).
 * - (Opcional) Checa se é admin (admins/{uid}).
 * - Só então chama onReady().
 * - Nunca navega para outra tela; mantém a Activity viva e dá feedback visual.
 */
public final class AuthGate {

    public interface Callback {
        /** Chamado quando a sessão está ok (e admin conferido, se exigido). */
        @MainThread void onReady();
        /** Chamado quando a sessão não está ok. UI deve permanecer na Activity. */
        @MainThread default void onNotReady(String msg) {}
    }

    private final Activity activity;
    private final View rootView;
    private final boolean requireAdmin;
    private final Callback cb;

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private FirebaseAuth.AuthStateListener authListener;
    private boolean started = false;
    private boolean ready = false;

    public AuthGate(Activity activity, View rootView, boolean requireAdmin, Callback cb) {
        this.activity = activity;
        this.rootView = rootView != null ? rootView : activity.findViewById(android.R.id.content);
        this.requireAdmin = requireAdmin;
        this.cb = cb;
        buildListener();
    }

    private void buildListener() {
        authListener = firebaseAuth -> {
            if (!started) return;
            if (firebaseAuth.getCurrentUser() == null) {
                ready = false;
                cb.onNotReady("Reconectando…");
                showSnack("Reconectando…");
                return;
            }
            // Autenticado. Se precisar, checa admin.
            if (requireAdmin) {
                String uid = firebaseAuth.getCurrentUser().getUid();
                db.collection("admins").document(uid).get()
                        .addOnSuccessListener(this::handleAdminDoc)
                        .addOnFailureListener(e -> {
                            ready = false;
                            cb.onNotReady("Erro ao verificar permissão: " + e.getMessage());
                            showSnack("Erro ao verificar permissão.");
                        });
            } else {
                if (!ready) {
                    ready = true;
                    cb.onReady();
                }
            }
        };
    }

    private void handleAdminDoc(DocumentSnapshot doc) {
        if (!started) return;
        if (doc != null && doc.exists()) {
            if (!ready) {
                ready = true;
                cb.onReady();
            }
        } else {
            ready = false;
            cb.onNotReady("Sem permissão de administrador.");
            showSnack("Sem permissão de administrador.");
        }
    }

    public void start() {
        if (started) return;
        started = true;
        auth.addAuthStateListener(authListener);
        // Dispara uma primeira avaliação
        FirebaseAuth current = FirebaseAuth.getInstance();
        if (current.getCurrentUser() == null) {
            cb.onNotReady("Aguardando sessão…");
        } else {
            if (requireAdmin) {
                String uid = current.getCurrentUser().getUid();
                db.collection("admins").document(uid).get()
                        .addOnSuccessListener(this::handleAdminDoc)
                        .addOnFailureListener(e -> {
                            ready = false;
                            cb.onNotReady("Erro ao verificar permissão: " + e.getMessage());
                            showSnack("Erro ao verificar permissão.");
                        });
            } else {
                ready = true;
                cb.onReady();
            }
        }
    }

    public void stop() {
        started = false;
        ready = false;
        if (authListener != null) {
            try { auth.removeAuthStateListener(authListener); } catch (Exception ignored) {}
        }
    }

    /** True quando a tela está liberada para iniciar listeners de conteúdo (Firestore etc.) */
    public boolean isReady() { return ready; }

    private void showSnack(String msg) {
        try { Snackbar.make(rootView, msg, Snackbar.LENGTH_SHORT).show(); } catch (Exception ignored) {}
    }
}
