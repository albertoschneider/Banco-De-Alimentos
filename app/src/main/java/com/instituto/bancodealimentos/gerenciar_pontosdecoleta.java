package com.instituto.bancodealimentos;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class gerenciar_pontosdecoleta extends AppCompatActivity {

    private FirebaseFirestore db;
    private RecyclerView rv;
    private EditText etBusca;
    private PontosAdapter adapter;
    private final List<Ponto> base = new ArrayList<>();
    private final List<Ponto> filtrada = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gerenciar_pontosdecoleta);

        View header = findViewById(R.id.header);
        if (header != null) {
            ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
                Insets sb = insets.getInsets(WindowInsetsCompat.Type.statusBars());
                v.setPadding(v.getPaddingLeft(), sb.top, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
            ViewCompat.requestApplyInsets(header);
        }

        FirebaseApp.initializeApp(this);
        db = FirebaseFirestore.getInstance();

        ImageButton btnVoltar = findViewById(R.id.btn_voltar);
        btnVoltar.setOnClickListener(v -> finish());

        etBusca = findViewById(R.id.etBusca);
        rv = findViewById(R.id.rvPontos);
        adapter = new PontosAdapter(filtrada, new PontosAdapter.Listener() {
            @Override public void onEditar(Ponto p) {
                Toast.makeText(gerenciar_pontosdecoleta.this, "Editar " + p.nome, Toast.LENGTH_SHORT).show();
                // (depois você decide o fluxo)
            }
            @Override public void onExcluir(Ponto p) {
                Toast.makeText(gerenciar_pontosdecoleta.this, "Excluir " + p.nome, Toast.LENGTH_SHORT).show();
                // (depois você decide o fluxo)
            }
        });
        rv.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fabAdd);
        fab.setOnClickListener(v ->
                startActivity(new Intent(this, pontosdecoleta_admin.class)));

        carregarPontos();
        configurarBusca();
    }

    private void configurarBusca() {
        etBusca.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filtrar(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void filtrar(String q) {
        filtrada.clear();
        if (q == null || q.trim().isEmpty()) {
            filtrada.addAll(base);
        } else {
            String qq = q.toLowerCase();
            for (Ponto p : base) {
                if ((p.nome != null && p.nome.toLowerCase().contains(qq)) ||
                        (p.endereco != null && p.endereco.toLowerCase().contains(qq))) {
                    filtrada.add(p);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void carregarPontos() {
        db.collection("pontos")
                .orderBy("nome", Query.Direction.ASCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        Toast.makeText(this, "Erro ao ler pontos: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (snap == null) return;

                    // Atualização incremental
                    for (DocumentChange dc : snap.getDocumentChanges()) {
                        Ponto p = Ponto.from(dc.getDocument().getId(), dc.getDocument().getData());
                        switch (dc.getType()) {
                            case ADDED:
                                base.add(p);
                                break;
                            case MODIFIED:
                                for (int i = 0; i < base.size(); i++) {
                                    if (base.get(i).id.equals(p.id)) { base.set(i, p); break; }
                                }
                                break;
                            case REMOVED:
                                for (int i = 0; i < base.size(); i++) {
                                    if (base.get(i).id.equals(p.id)) { base.remove(i); break; }
                                }
                                break;
                        }
                    }
                    filtrar(etBusca.getText().toString());
                });
    }
}
