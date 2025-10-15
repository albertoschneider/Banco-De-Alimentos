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
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_doacoes);
        root = findViewById(android.R.id.content);

        // Insets no header
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

        // Listener de auth: não finaliza a tela; apenas reage quando loga
        authListener = firebaseAuth -> {
            if (firebaseAuth.getCurrentUser() != null) {
                ensureAndReload();
            } else {
                Snackbar.make(root, "Reconectando...", Snackbar.LENGTH_SHORT).show();
            }
        };
    }

    @Override protected void onStart() {
        super.onStart();
        if (authListener != null) auth.addAuthStateListener(authListener);
        if (auth.getCurrentUser() != null) ensureAndReload();
    }

    @Override protected void onStop() {
        super.onStop();
        if (authListener != null) auth.removeAuthStateListener(authListener);
        if (sub != null) { sub.remove(); sub = null; }
    }

    private void ensureAndReload() {
        if (auth.getCurrentUser() == null) return;

        // Checa se é admin (regras já protegem, mas isso evita PERMISSION_DENIED ficar “misterioso”)
        db.collection("admins").document(auth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        reload();
                    } else {
                        Snackbar.make(root, "Sem permissão de administrador.", Snackbar.LENGTH_LONG).show();
                        // não finaliza; deixa o usuário voltar manualmente
                    }
                })
                .addOnFailureListener(e ->
                        Snackbar.make(root, "Erro ao verificar permissão: " + e.getMessage(), Snackbar.LENGTH_LONG).show()
                );
    }

    private void reload() {
        if (auth.getCurrentUser() == null) return;

        if (sub != null) sub.remove();
        swipe.setRefreshing(true);

        sub = db.collection("doacoes")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(500)
                .addSnapshotListener((snap, err) -> {
                    swipe.setRefreshing(false);
                    if (err != null) {
                        Snackbar.make(rv, "Erro ao carregar: " + err.getMessage(), Snackbar.LENGTH_LONG).show();
                        return;
                    }
                    if (snap == null || snap.isEmpty()) {
                        adapter.setItems(new ArrayList<>());
                        emptyState.setVisibility(View.VISIBLE);
                        return;
                    }

                    List<Doacao> list = snap.toObjects(Doacao.class);
                    adapter.setItems(list);
                    emptyState.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);

                    // Cache de nomes por uid
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        String uid = d.getString("uid");
                        if (uid == null || uidNameCache.containsKey(uid)) continue;
                        db.collection("usuarios").document(uid).get()
                                .addOnSuccessListener(doc -> {
                                    String nome = doc != null ? doc.getString("nome") : null;
                                    if (nome == null || nome.trim().isEmpty()) nome = "(sem nome)";
                                    uidNameCache.put(uid, nome);
                                    adapter.notifyDataSetChanged();
                                });
                    }
                });
    }
}
