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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
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

        // (opcional) manter a status bar amarela e ícones escuros
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        getWindow().setStatusBarColor(Color.parseColor("#FFF1B100"));
        WindowInsetsControllerCompat c = ViewCompat.getWindowInsetsController(getWindow().getDecorView());
        if (c != null) c.setAppearanceLightStatusBars(true);

        setContentView(R.layout.activity_doealimentos);

        // >>> EXATAMENTE como no Pontos de Coleta: aplica o paddingTop da status bar no header
        View header = findViewById(R.id.header);
        if (header != null) {
            ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
                Insets sb = insets.getInsets(WindowInsetsCompat.Type.statusBars());
                v.setPadding(v.getPaddingLeft(), sb.top, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
            ViewCompat.requestApplyInsets(header);
        }
        // <<<

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

        // Finalizar Doação -> salva carrinho e abre carrinho
        findViewById(R.id.btnContinuar).setOnClickListener(v -> {
            salvarCarrinhoLocal();
            startActivity(new Intent(doealimentos.this, carrinho.class));
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

    private void salvarCarrinhoLocal() {
        ArrayList<Produto> selecionados = new ArrayList<>();
        for (int i = 0; i < lista.size(); i++) {
            Produto p = lista.get(i);
            int qtd = adapter.getQuantidadeByPosition(i);
            if (qtd > 0) {
                Produto copy = new Produto(p.getId(), p.getNome(), p.getPreco(), p.getImagemUrl());
                try { copy.getClass().getMethod("setQuantidade", int.class).invoke(copy, qtd); } catch (Exception ignored) {}
                selecionados.add(copy);
            }
        }
        CartStore.save(this, selecionados);
    }

    @Override protected void onDestroy() {
        if (listener != null) listener.remove();
        super.onDestroy();
    }
}
