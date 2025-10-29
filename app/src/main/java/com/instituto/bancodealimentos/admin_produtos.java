package com.instituto.bancodealimentos;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class admin_produtos extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProdutoAdminAdapter adapter;
    private final List<Produto> lista = new ArrayList<>();
    private ListenerRegistration listener;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowInsetsHelper.setupEdgeToEdge(this);
        setContentView(R.layout.activity_admin_produtos);

        // Aplicar insets
        WindowInsetsHelper.applyTopInsets(findViewById(R.id.header));
        WindowInsetsHelper.applyScrollInsets(findViewById(R.id.recyclerView));


        ImageButton back = findViewById(R.id.btn_voltar);
        if (back != null) back.setOnClickListener(v -> onBackPressed());

        // >>> ADIÇÃO: botão "+" para criar produto
        ImageButton add = findViewById(R.id.btn_add);
        if (add != null) {
            add.setOnClickListener(v -> {
                Intent i = new Intent(admin_produtos.this, criar_produto.class);
                startActivity(i);
            });
        }
        // <<<

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);

        // spanCount dinâmico (2–4) conforme a largura em dp
        int span = calcularSpanPorLarguraDp(/* base= */ 160);
        recyclerView.setLayoutManager(new GridLayoutManager(this, span));

        adapter = new ProdutoAdminAdapter(lista, p -> {
            Intent i = new Intent(admin_produtos.this, editar_produto.class);
            i.putExtra("produtoId", p.getId()); // garanta getId() no seu model Produto
            startActivity(i);
        });
        recyclerView.setAdapter(adapter);

        escutarProdutos();
    }

    private void escutarProdutos() {
        listener = db.collection("produtos")
                .orderBy("nome", Query.Direction.ASCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;

                    lista.clear();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        Produto p = new Produto();
                        // mapeia campos (ajuste nomes conforme seu model)
                        p.setId(d.getId());
                        p.setNome(d.getString("nome"));
                        Double preco = d.getDouble("preco");
                        p.setPreco(preco == null ? 0.0 : preco);
                        p.setImagemUrl(d.getString("imagemUrl"));
                        lista.add(p);
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    @Override protected void onDestroy() {
        if (listener != null) listener.remove();
        super.onDestroy();
    }

    private int calcularSpanPorLarguraDp(int baseItemDp) {
        float density = getResources().getDisplayMetrics().density;
        int widthPx = getResources().getDisplayMetrics().widthPixels;
        int widthDp = Math.max(1, Math.round(widthPx / density));
        int span = Math.max(2, widthDp / baseItemDp);
        return Math.min(span, 4); // limita até 4 colunas
    }
}