package com.instituto.bancodealimentos;

import android.os.Bundle;
import android.view.View;
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
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class HistoricoDoacoesActivity extends AppCompatActivity {

    private static final long TEN_MIN_MS = 10 * 60 * 1000L;

    private RecyclerView rv;
    private SwipeRefreshLayout swipe;
    private View emptyState;
    private DoacaoAdapter adapter;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private ListenerRegistration sub;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historico_doacoes);

        // Header com insets
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

        rv = findViewById(R.id.rvDoacoes);
        swipe = findViewById(R.id.swipe);
        emptyState = findViewById(R.id.emptyState);

        adapter = new DoacaoAdapter();
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        swipe.setOnRefreshListener(this::reload);

        reload();
    }

    private void reload() {
        if (auth.getCurrentUser() == null) { finish(); return; }
        String uid = auth.getCurrentUser().getUid();

        if (sub != null) sub.remove();
        swipe.setRefreshing(true);

        sub = db.collection("doacoes")
                .whereEqualTo("uid", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(100)
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

                    // Auto-expirar: 10 minutos após createdAt se ainda estiver "pending"
                    autoExpire(list);

                    adapter.setItems(list);
                    emptyState.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    private void autoExpire(List<Doacao> list) {
        if (list == null || list.isEmpty()) return;

        long nowMs = System.currentTimeMillis();

        for (Doacao d : list) {
            if (d == null) continue;

            if ("pending".equals(d.getStatus())) {
                long createdMs = d.getCreatedAt() != null ? d.getCreatedAt().toDate().getTime() : 0L;

                // se não houver expiresAt, considere createdAt + 10min
                long expMs = (d.getExpiresAt() != null)
                        ? d.getExpiresAt().toDate().getTime()
                        : createdMs + TEN_MIN_MS;

                if (expMs > 0 && nowMs >= expMs) {
                    // marca como expirado e grava expiresAt se não existia
                    if (d.getId() != null) {
                        if (d.getExpiresAt() == null) {
                            Date expDate = new Date(createdMs + TEN_MIN_MS);
                            db.collection("doacoes")
                                    .document(d.getId())
                                    .update(
                                            "status", "expired",
                                            "expiresAt", new Timestamp(expDate),
                                            "updatedAt", FieldValue.serverTimestamp()
                                    );
                        } else {
                            db.collection("doacoes")
                                    .document(d.getId())
                                    .update(
                                            "status", "expired",
                                            "updatedAt", FieldValue.serverTimestamp()
                                    );
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sub != null) sub.remove();
    }
}
