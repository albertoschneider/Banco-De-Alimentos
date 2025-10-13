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
    private View emptyState;
    private EditText etBusca;

    private AdminDoacaoAdapter adapter;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private ListenerRegistration sub;

    private final Map<String, String> uidNameCache = new HashMap<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_doacoes);

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

        swipe.setOnRefreshListener(this::reload);

        // Filtro em memória por nome / nº do pedido / descrição
        etBusca.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.applyFilter(s == null ? null : s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        reload();
    }

    private void reload() {
        if (auth.getCurrentUser() == null) { finish(); return; }

        if (sub != null) sub.remove();
        swipe.setRefreshing(true);

        db.collection("doacoes")
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

                    // Cache de nomes por uid (faz lookup só do que faltar)
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sub != null) sub.remove();
    }
}
