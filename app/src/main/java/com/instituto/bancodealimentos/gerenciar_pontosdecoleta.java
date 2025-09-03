package com.instituto.bancodealimentos;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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

        // Ajuste de status bar no header amarelo
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
        if (btnVoltar != null) btnVoltar.setOnClickListener(v -> finish());

        etBusca = findViewById(R.id.etBusca);
        rv = findViewById(R.id.rvPontos);

        adapter = new PontosAdapter(filtrada, new PontosAdapter.Listener() {
            @Override
            public void onEditar(Ponto p) {
                // Abre a tela de edição com os dados do ponto
                Intent it = new Intent(gerenciar_pontosdecoleta.this, editar_pontodecoleta.class);
                it.putExtra("docId", p.id);
                it.putExtra("nome", p.nome);
                it.putExtra("endereco", p.endereco);
                it.putExtra("lat", p.lat == null ? "" : String.valueOf(p.lat));
                it.putExtra("lng", p.lng == null ? "" : String.valueOf(p.lng));
                it.putExtra("disponibilidade", p.disponibilidade);
                startActivity(it);
            }

            @Override
            public void onExcluir(Ponto p) {
                mostrarDialogExcluir(p);
            }
        });
        rv.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fabAdd);
        if (fab != null) {
            fab.setOnClickListener(v ->
                    startActivity(new Intent(this, pontosdecoleta_admin.class)));
        }

        carregarPontos();
        configurarBusca();
    }

    private void configurarBusca() {
        etBusca.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filtrar(s.toString()); }
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

    /** Mostra popup custom igual ao design */
    private void mostrarDialogExcluir(Ponto p) {
        View view = getLayoutInflater().inflate(R.layout.dialog_excluir_ponto, null);

        TextView btnCancelar = view.findViewById(R.id.btnCancelar);
        TextView btnExcluir  = view.findViewById(R.id.btnExcluir);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        btnCancelar.setOnClickListener(v -> dialog.dismiss());

        btnExcluir.setOnClickListener(v -> {
            db.collection("pontos").document(p.id)
                    .delete()
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Ponto excluído.", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(err ->
                            Toast.makeText(this, "Erro ao excluir: " + err.getMessage(), Toast.LENGTH_LONG).show());
        });

        dialog.show();
    }
}
