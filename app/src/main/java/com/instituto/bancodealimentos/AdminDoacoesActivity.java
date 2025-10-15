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
import com.google.firebase.firestore.QuerySnapshot;

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
    private ListenerRegistration sub;

    private final Map<String, String> uidNameCache = new HashMap<>();
    private boolean alive = false;
    private boolean listenerAttached = false;

    private final Retry.Backoff retry = new Retry.Backoff(5, 300);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_doacoes);
        root = findViewById(android.R.id.content);

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

        swipe.setOnRefreshListener(this::startOrReload);

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
        swipe.setRefreshing(true);
        listenerAttached = false;
        startOrReload();
    }

    @Override protected void onStop() {
        super.onStop();
        alive = false;
        detachListener();
    }

    private void startOrReload() {
        if (!alive) return;
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            // NÃO navega pro login. Espera estabilizar (o Splash já cuidou de roteamento)
            swipe.setRefreshing(false);
            showEmpty(true);
            Snackbar.make(root, "Sessão não disponível. Tente novamente.", Snackbar.LENGTH_LONG)
                    .setAction("Tentar de novo", v -> startOrReload())
                    .show();
            return;
        }

        // Primer: get() do servidor — sem listener, evita corridas de auth no 1º toque
        db.collection("admins").document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (!alive) return;
                    if (doc != null && doc.exists()) {
                        fetchOnceThenAttach();
                    } else {
                        swipe.setRefreshing(false);
                        showEmpty(true);
                        Snackbar.make(root, "Sem permissão de administrador.", Snackbar.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> handlePrimerError(e, this::startOrReload));
    }

    private void fetchOnceThenAttach() {
        db.collection("doacoes")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(500)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!alive) return;
                    applySnapshot(snap);
                    swipe.setRefreshing(false);
                    if (!listenerAttached) attachListener(); // só agora liga o tempo real
                })
                .addOnFailureListener(e -> handlePrimerError(e, this::fetchOnceThenAttach));
    }

    private void attachListener() {
        detachListener();
        listenerAttached = true;

        sub = db.collection("doacoes")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(500)
                .addSnapshotListener((snap, err) -> {
                    if (!alive) return;

                    if (err != null) {
                        // Listener falhou; mostra e tenta reconectar depois
                        handleLiveError(err);
                        return;
                    }
                    applySnapshot(snap);
                });
    }

    private void detachListener() {
        listenerAttached = false;
        if (sub != null) {
            try { sub.remove(); } catch (Exception ignored) {}
            sub = null;
        }
    }

    private void applySnapshot(QuerySnapshot snap) {
        if (snap == null || snap.isEmpty()) {
            adapter.setItems(new ArrayList<>());
            showEmpty(true);
            return;
        }
        List<Doacao> list = snap.toObjects(Doacao.class);
        adapter.setItems(list);
        showEmpty(list.isEmpty());

        // cache nomes
        for (DocumentSnapshot d : snap.getDocuments()) {
            String uid = d.getString("uid");
            if (uid == null || uidNameCache.containsKey(uid)) continue;
            db.collection("usuarios").document(uid).get()
                    .addOnSuccessListener(doc -> {
                        if (!alive) return;
                        String nome = doc != null ? doc.getString("nome") : null;
                        if (nome == null || nome.trim().isEmpty()) nome = "(sem nome)";
                        uidNameCache.put(uid, nome);
                        adapter.notifyDataSetChanged();
                    });
        }
    }

    private void showEmpty(boolean show) {
        if (emptyState != null) emptyState.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void handlePrimerError(Exception e, Runnable retryJob) {
        swipe.setRefreshing(false);
        showEmpty(true);

        if (e instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException fe = (FirebaseFirestoreException) e;
            switch (fe.getCode()) {
                case UNAUTHENTICATED:
                case PERMISSION_DENIED:
                case UNAVAILABLE:
                case DEADLINE_EXCEEDED:
                    if (retry.canRetry()) {
                        Snackbar.make(root, "Reconectando…", Snackbar.LENGTH_SHORT).show();
                        retry.schedule(retryJob::run);
                        return;
                    }
            }
        }
        Snackbar.make(root, "Erro ao carregar: " + e.getMessage(), Snackbar.LENGTH_LONG)
                .setAction("Tentar de novo", v -> retryJob.run())
                .show();
    }

    private void handleLiveError(Exception e) {
        // Não derruba a tela nem remove o adapter.
        if (e instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException.Code c = ((FirebaseFirestoreException) e).getCode();
            if (c == FirebaseFirestoreException.Code.UNAUTHENTICATED ||
                    c == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
                    c == FirebaseFirestoreException.Code.UNAVAILABLE) {
                // solta um retry discreto
                if (retry.canRetry()) retry.schedule(this::attachListener);
            }
        }
        Snackbar.make(root, "Conexão instável. Mantendo dados em tela.", Snackbar.LENGTH_SHORT).show();
    }
}
