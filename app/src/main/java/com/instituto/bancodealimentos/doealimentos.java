package com.instituto.bancodealimentos;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class doealimentos extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProdutoUsuarioAdapter adapter;
    private final List<Produto> lista = new ArrayList<>();
    private TextView tvValorTotal;

    private ListenerRegistration listener;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doealimentos);

        tvValorTotal = findViewById(R.id.tvValorTotal);
        recyclerView = findViewById(R.id.recyclerView);

        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new ProdutoUsuarioAdapter(lista, this::atualizarTotal);
        recyclerView.setAdapter(adapter);

        ImageButton back = findViewById(R.id.btn_voltar);
        if (back != null) back.setOnClickListener(v -> {
            Intent intent = new Intent(doealimentos.this, menu.class);
            startActivity(intent);
        });

        findViewById(R.id.btnContinuar).setOnClickListener(v -> {
            // Aqui você pode abrir a tela de checkout/carrinho se quiser
            Toast.makeText(this, "Continuar com total: " + tvValorTotal.getText(), Toast.LENGTH_SHORT).show();
        });

        escutar();
    }

    private void escutar() {
        listener = db.collection("produtos")
                .orderBy("nome", Query.Direction.ASCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) { Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show(); return; }
                    if (snap == null) return;

                    int[] quantidadesAntigas = adapter.dumpQuantidadesByOrder();

                    lista.clear();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        lista.add(new Produto(
                                d.getId(),
                                d.getString("nome"),
                                d.getDouble("preco") == null ? 0.0 : d.getDouble("preco"),
                                d.getString("imagemUrl")
                        ));
                    }
                    adapter.notifyDataSetChanged();
                    adapter.restoreQuantidadesByOrder(quantidadesAntigas);
                    atualizarTotal();
                });
    }

    private void atualizarTotal() {
        double total = 0.0;
        for (int i = 0; i < lista.size(); i++) {
            Produto p = lista.get(i);
            int qtd = adapter.getQuantidadeByPosition(i);
            total += (p.getPreco() == null ? 0.0 : p.getPreco()) * qtd;
        }
        tvValorTotal.setText(Money.fmt(total));
    }

    @Override protected void onDestroy() {
        if (listener != null) listener.remove();
        super.onDestroy();
    }
}