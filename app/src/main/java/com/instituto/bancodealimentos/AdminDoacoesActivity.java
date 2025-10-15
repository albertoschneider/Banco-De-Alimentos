package com.instituto.bancodealimentos;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("FieldCanBeLocal")
public class AdminDoacoesActivity extends AppCompatActivity {

    private RecyclerView rv;
    private SwipeRefreshLayout swipe;
    private View emptyState, root;
    private EditText etBusca;

    private AdminDoacaoAdapter adapter;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private ListenerRegistration sub;
    private FirebaseAuth.AuthStateListener authListener;

    private final Map<String, String> uidNameCache = new HashMap<>();
    private boolean isStarted = false; // evita operar UI/listener após onStop

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_doacoes);
        root = findViewById(android.R.id.content);

        // Header insets
        View header = findViewById(R.id.header);
        if (header != null) {
            ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
                Insets sb = insets.getInsets(WindowInsetsCompat.Type.statusBars());
                v.setPadding(v.getPaddingLeft(), sb.top, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
            ViewCompat.requestApplyInsets(header);
        }

        ImageButton back = findViewById(R.id.btn_voltar);
        if (back != null) back.setOnClickListener(v -> finish());

        etBusca = findViewById(R.id.etBusca);
        rv = findViewById(R.id.rvDoacoesAdmin);
        swipe = findViewById(R.id.swipe);
        emptyState = findViewById(R.id.emptyState);

        adapter = new AdminDoacaoAdapter();
        adapter.setUidNameMap(uidNameCache);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        swipe.setOnRefreshListener(this::ensureAndReload);

        etBusca.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.applyFilter(s == null ? null : s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Gate de auth robusto (não finaliza a tela; espera estabilizar)
        authListener = firebaseAuth -> {
            if (!isStarted) return;
            if (firebaseAuth.getCurrentUser() != null) {
                ensureAndReload();
            } else {
                // Sem sessão ainda (latência): mostra aviso e mantém a tela
                Snackbar.make(root, "Reconectando…", Snackbar.LENGTH_SHORT).show();
                stopQueryIfAny();
                swipe.setRefreshing(false);
                showEmpty(true);
            }
        };
    }

    @Override protected void onStart() {
        super.onStart();
        isStarted = true;
        if (authListener != null) auth.addAuthStateListener(authListener);
        if (auth.getCurrentUser() != null) ensureAndReload();
        else {
            swipe.setRefreshing(true); // mostra loading enquanto auth estabiliza
        }
    }

    @Override protected void onStop() {
        super.onStop();
        isStarted = false;
        if (authListener != null) auth.removeAuthStateListener(authListener);
        stopQueryIfAny();
    }

    private void stopQueryIfAny() {
        if (sub != null) {
            try { sub.remove(); } catch (Exception ignored) {}
            sub = null;
        }
    }

    private void showEmpty(boolean show) {
        if (emptyState != null) emptyState.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void ensureAndReload() {
        if (!isStarted) return;
        if (auth.getCurrentUser() == null) return;

        // Confirma admin explicitamente (além das rules)
        db.collection("admins").document(auth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (!isStarted) return;
                    if (doc != null && doc.exists()) {
                        reload();
                    } else {
                        stopQueryIfAny();
                        swipe.setRefreshing(false);
                        showEmpty(true);
                        Snackbar.make(root, "Sem permissão de administrador.", Snackbar.LENGTH_LONG)
                                .setAction("Tentar de novo", v -> ensureAndReload())
                                .show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isStarted) return;
                    stopQueryIfAny();
                    swipe.setRefreshing(false);
                    showEmpty(true);
                    Snackbar.make(root, "Erro ao verificar permissão: " + e.getMessage(), Snackbar.LENGTH_LONG)
                            .setAction("Tentar de novo", v -> ensureAndReload())
                            .show();
                });
    }

    private void reload() {
        if (!isStarted) return;
        if (auth.getCurrentUser() == null) return;

        stopQueryIfAny();
        swipe.setRefreshing(true);

        sub = db.collection("doacoes")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(500)
                .addSnapshotListener((snap, err) -> {
                    if (!isStarted) return;

                    swipe.setRefreshing(false);

                    if (err != null) {
                        handleFirestoreError(err);
                        return;
                    }
                    if (snap == null || snap.isEmpty()) {
                        adapter.setItems(new ArrayList<>());
                        showEmpty(true);
                        return;
                    }

                    List<Doacao> list = snap.toObjects(Doacao.class);
                    adapter.setItems(list);
                    showEmpty(list.isEmpty());

                    // Cache dos nomes
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        String uid = d.getString("uid");
                        if (uid == null || uidNameCache.containsKey(uid)) continue;
                        db.collection("usuarios").document(uid).get()
                                .addOnSuccessListener(doc -> {
                                    if (!isStarted) return;
                                    String nome = doc != null ? doc.getString("nome") : null;
                                    if (nome == null || nome.trim().isEmpty()) nome = "(sem nome)";
                                    uidNameCache.put(uid, nome);
                                    adapter.notifyDataSetChanged();
                                });
                    }
                });
    }

    private void handleFirestoreError(Exception e) {
        stopQueryIfAny();
        showEmpty(true);

        if (e instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException fe = (FirebaseFirestoreException) e;
            switch (fe.getCode()) {
                case UNAUTHENTICATED:
                    Snackbar.make(root, "Sessão expirada. Tente novamente.", Snackbar.LENGTH_LONG)
                            .setAction("Tentar de novo", v -> ensureAndReload())
                            .show();
                    return;
                case PERMISSION_DENIED:
                    Snackbar.make(root, "Sem permissão de administrador.", Snackbar.LENGTH_LONG)
                            .setAction("Tentar de novo", v -> ensureAndReload())
                            .show();
                    return;
                default:
                    // Continua para o genérico
            }
        }

        Snackbar.make(root, "Erro ao carregar: " + e.getMessage(), Snackbar.LENGTH_LONG)
                .setAction("Tentar de novo", v -> ensureAndReload())
                .show();
    }
}
