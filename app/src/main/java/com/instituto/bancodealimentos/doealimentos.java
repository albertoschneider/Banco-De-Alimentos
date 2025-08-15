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

import com.google.gson.Gson;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class doealimentos extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProdutoAdapter adapter;
    private final List<Produto> listaProdutos = new ArrayList<>();
    private TextView tvValorTotal;

    private ListenerRegistration produtosListener;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private final NumberFormat currencyBr =
            NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // ATENÇÃO: aponte para o layout novo que criamos
        setContentView(R.layout.activity_doealimentos);

        tvValorTotal = findViewById(R.id.tvValorTotal);
        recyclerView = findViewById(R.id.recyclerView);

        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new ProdutoAdapter(this, listaProdutos, this::atualizarTotal);
        recyclerView.setAdapter(adapter);

        // Navegação
        ImageButton btnBack = findViewById(R.id.btn_voltar);
        if (btnBack != null) btnBack.setOnClickListener(v -> onBackPressed());

        findViewById(R.id.btnContinuar).setOnClickListener(v -> irParaCarrinho());

        // Escuta em tempo real dos produtos
        iniciarListenerProdutos();
    }

    private void iniciarListenerProdutos() {
        // produtos ordenados por nome
        produtosListener = db.collection("produtos")
                .orderBy("nome", Query.Direction.ASCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        Toast.makeText(this, "Erro: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (snap == null) return;

                    // mantém quantidades já selecionadas ao atualizar a lista
                    List<Produto> selecionados = new ArrayList<>();
                    for (Produto p : listaProdutos) if (p.getQuantidade() > 0) selecionados.add(p);

                    listaProdutos.clear();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        String docId = d.getId();
                        String nome = d.getString("nome");
                        Double preco = d.getDouble("preco"); // Number no Firestore
                        String imagemUrl = d.getString("imagemUrl"); // pode ser null

                        Produto p = new Produto(
                                docId,
                                nome != null ? nome : "",
                                preco != null ? preco : 0.0,
                                imagemUrl
                        );

                        // restaura quantidade se já estava selecionado
                        for (Produto s : selecionados) {
                            if (s.getDocId().equals(docId)) {
                                p.setQuantidade(s.getQuantidade());
                                break;
                            }
                        }

                        listaProdutos.add(p);
                    }
                    adapter.notifyDataSetChanged();
                    atualizarTotal();
                });
    }

    private void atualizarTotal() {
        double total = 0.0;
        for (Produto p : listaProdutos) {
            total += p.getPreco() * p.getQuantidade();
        }
        tvValorTotal.setText(currencyBr.format(total));
    }

    private void irParaCarrinho() {
        ArrayList<Produto> selecionados = new ArrayList<>();
        for (Produto p : listaProdutos) if (p.getQuantidade() > 0) selecionados.add(p);

        // Salva no SharedPreferences (igual você já fazia)
        Gson gson = new Gson();
        String json = gson.toJson(selecionados);
        getSharedPreferences("MeuApp", MODE_PRIVATE)
                .edit()
                .putString("carrinho", json)
                .apply();

        startActivity(new Intent(this, carrinho.class));
    }

    @Override
    protected void onDestroy() {
        if (produtosListener != null) {
            produtosListener.remove();
            produtosListener = null;
        }
        super.onDestroy();
    }
}