package com.instituto.bancodealimentos;

import android.content.Intent;
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
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminDoacoesActivity extends AppCompatActivity {

    private View root;
    private RecyclerView rv;
    private SwipeRefreshLayout swipe;
    private View emptyState;
    private EditText etBusca;

    private AdminDoacaoAdapter adapter;
    private FirebaseFirestore db;

    private final Map<String, String> uidNameCache = new HashMap<>();
    private boolean alive = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_doacoes);
        root = findViewById(android.R.id.content);

        // insets
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

        db = FirebaseFirestore.getInstance();

        swipe.setOnRefreshListener(this::carregarUmaVez);

        etBusca.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.applyFilter(s == null ? null : s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    @Override protected void onStart() {
        super.onStart();
        alive = true;
        if (swipe != null) swipe.setRefreshing(true);
        carregarUmaVez();
    }

    @Override protected void onStop() {
        super.onStop();
        alive = false;
        if (rv != null) rv.setAdapter(null); // solta refs/evita leaks
    }

    private void carregarUmaVez() {
        try {
            if (!alive) { if (swipe != null) swipe.setRefreshing(false); return; }

            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                if (swipe != null) swipe.setRefreshing(false);
                startActivity(new Intent(this, SplashActivity.class));
                finish();
                return;
            }

            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            db.collection("admins").document(uid).get()
                    .addOnSuccessListener(adminDoc -> {
                        if (!alive) return;

                        if (adminDoc == null || !adminDoc.exists()) {
                            if (swipe != null) swipe.setRefreshing(false);
                            mostrarVazio(true);
                            Snackbar.make(root, "Sem permissão de administrador.", Snackbar.LENGTH_LONG).show();
                            return;
                        }

                        db.collection("doacoes")
                                .orderBy("createdAt", Query.Direction.DESCENDING)
                                .limit(500)
                                .get()
                                .addOnSuccessListener(snap -> {
                                    if (!alive) return;
                                    if (swipe != null) swipe.setRefreshing(false);

                                    if (snap == null || snap.isEmpty()) {
                                        adapter.setItems(new ArrayList<>());
                                        mostrarVazio(true);
                                        return;
                                    }
                                    List<Doacao> list = snap.toObjects(Doacao.class);
                                    adapter.setItems(list);
                                    mostrarVazio(list.isEmpty());

                                    try {
                                        for (DocumentSnapshot d : snap.getDocuments()) {
                                            String u = d.getString("uid");
                                            if (u == null || uidNameCache.containsKey(u)) continue;
                                            db.collection("usuarios").document(u).get()
                                                    .addOnSuccessListener(doc -> {
                                                        if (!alive) return;
                                                        String nome = doc != null ? doc.getString("nome") : null;
                                                        if (nome == null || nome.trim().isEmpty()) nome = "(sem nome)";
                                                        uidNameCache.put(u, nome);
                                                        adapter.notifyDataSetChanged();
                                                    });
                                        }
                                    } catch (Throwable ignore) {}
                                })
                                .addOnFailureListener(e -> {
                                    if (!alive) return;
                                    if (swipe != null) swipe.setRefreshing(false);
                                    mostrarVazio(true);
                                    Snackbar.make(root, "Erro ao carregar: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        if (!alive) return;
                        if (swipe != null) swipe.setRefreshing(false);
                        mostrarVazio(true);
                        Snackbar.make(root, "Erro ao verificar admin: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                    });

        } catch (Throwable t) {
            if (swipe != null) swipe.setRefreshing(false);
            mostrarVazio(true);
            Snackbar.make(root, "Falha inesperada: " + t.getClass().getSimpleName(), Snackbar.LENGTH_LONG).show();
        }
    }

    private void mostrarVazio(boolean show) {
        if (emptyState != null) emptyState.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
