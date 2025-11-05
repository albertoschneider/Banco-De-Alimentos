package com.instituto.bancodealimentos;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class doealimentos extends AppCompatActivity {

    private androidx.recyclerview.widget.RecyclerView recyclerView;
    private ProdutoUsuarioAdapter adapter;
    private final List<Produto> lista = new ArrayList<>();
    private TextView tvValorTotal;

    private ListenerRegistration listener;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doealimentos);

        // Aplicar insets SOMENTE no conteúdo e rodapé (não no header)
        WindowInsetsHelper.applyScrollInsets(findViewById(R.id.recyclerView));
        WindowInsetsHelper.applyBottomInsets(findViewById(R.id.footer));

        tvValorTotal = findViewById(R.id.tvValorTotal);
        recyclerView = findViewById(R.id.recyclerView);

        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new ProdutoUsuarioAdapter(lista, this::atualizarTotal);
        recyclerView.setAdapter(adapter);

        ImageButton back = findViewById(R.id.btn_voltar);
        if (back != null) back.setOnClickListener(v -> {
            startActivity(new Intent(doealimentos.this, menu.class));
            finish();
        });

        // CORREÇÃO: Usar o FrameLayout btn_cart ao invés do ImageButton interno
        View btnCart = findViewById(R.id.btn_cart);
        if (btnCart != null) btnCart.setOnClickListener(v -> {
            startActivity(new Intent(doealimentos.this, carrinho.class));
        });

        findViewById(R.id.btnContinuar).setOnClickListener(v -> finalizarDoacao());

        escutarProdutos();
    }

    private void escutarProdutos() {
        listener = db.collection("produtos")
                .orderBy("nome", Query.Direction.ASCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
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
            total += (p.getPreco() == null ? 0.0 : p.getPreco()) * Math.max(0, qtd);
        }
        tvValorTotal.setText(Money.fmt(total));
    }

    private void finalizarDoacao() {
        ArrayList<Produto> selecionados = new ArrayList<>();
        double total = 0.0;

        for (int i = 0; i < lista.size(); i++) {
            Produto p = lista.get(i);
            int qtd = Math.max(0, adapter.getQuantidadeByPosition(i));
            if (qtd > 0) {
                Produto copy = new Produto(p.getId(), p.getNome(), p.getPreco(), p.getImagemUrl());
                try {
                    copy.getClass().getMethod("setQuantidade", int.class).invoke(copy, qtd);
                } catch (Exception ignored) {}
                selecionados.add(copy);
                total += (p.getPreco() == null ? 0.0 : p.getPreco()) * qtd;
            }
        }

        if (selecionados.isEmpty()) {
            Toast.makeText(this, "Selecione ao menos 1 item para doar.", Toast.LENGTH_SHORT).show();
            return;
        }

        CartStore.save(this, selecionados);

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) {
            Toast.makeText(this, "Faça login para finalizar a doação.", Toast.LENGTH_LONG).show();
            return;
        }

        String orderId   = db.collection("tmp").document().getId();
        String displayId = orderId.substring(0, Math.min(7, orderId.length())).toUpperCase(Locale.ROOT);

        Timestamp createdAt = Timestamp.now();
        Date expDate        = new Date(System.currentTimeMillis() + 10 * 60 * 1000L);
        Timestamp expiresAt = new Timestamp(expDate);

        Map<String, Object> pedido = new HashMap<>();
        pedido.put("displayId", displayId);
        pedido.put("status", "PENDENTE");
        pedido.put("total", total);
        pedido.put("createdAt", createdAt);
        pedido.put("expiresAt", expiresAt);

        List<Map<String, Object>> items = new ArrayList<>();
        for (Produto p : selecionados) {
            Map<String, Object> it = new HashMap<>();
            it.put("productId", p.getId());
            it.put("name", p.getNome());
            it.put("qty", safeQtd(p));
            it.put("unitPrice", p.getPreco() == null ? 0.0 : p.getPreco());
            it.put("imageUrl", p.getImagemUrl());
            items.add(it);
        }
        pedido.put("items", items);

        db.collection("users").document(uid)
                .collection("orders").document(orderId)
                .set(pedido)
                .addOnSuccessListener(a -> startActivity(new Intent(doealimentos.this, carrinho.class)))
                .addOnFailureListener(err -> Toast.makeText(this,
                        "Não foi possível criar o pedido: " + err.getMessage(), Toast.LENGTH_LONG).show());
    }

    private int safeQtd(Produto p) {
        try {
            return (int) p.getClass().getMethod("getQuantidade").invoke(p);
        } catch (Exception ignored) {
            return 0;
        }
    }

    @Override
    protected void onDestroy() {
        if (listener != null) listener.remove();
        super.onDestroy();
    }
}