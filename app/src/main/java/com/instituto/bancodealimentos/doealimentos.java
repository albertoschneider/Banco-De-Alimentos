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

        // --- Edge-to-edge estável ---
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        setContentView(R.layout.activity_doealimentos);

        // Header recebe padding = altura da status bar (NÃO somar)
        View header = findViewById(R.id.header);
        if (header != null) {
            ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
                Insets sb = insets.getInsets(WindowInsetsCompat.Type.statusBars());
                v.setPadding(v.getPaddingLeft(), sb.top, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
            ViewCompat.requestApplyInsets(header);

            // Ícones escuros na status bar (porque seu header é claro)
            WindowInsetsControllerCompat c = ViewCompat.getWindowInsetsController(getWindow().getDecorView());
            if (c != null) c.setAppearanceLightStatusBars(true);
        }

        // Opcional: empurra conteúdo/rodapé pelo tamanho da barra de navegação
        View root = findViewById(R.id.main); // use o root da sua tela
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets nb = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), nb.bottom);
                return insets;
            });
            ViewCompat.requestApplyInsets(root);
        }

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

        // *** FINALIZAR DOAÇÃO -> SALVA O CARRINHO E ABRE A TELA DE CARRINHO ***
        findViewById(R.id.btnContinuar).setOnClickListener(v -> {
            salvarCarrinhoLocal();
            Intent it = new Intent(doealimentos.this, carrinho.class);
            // se quiser limpar o histórico entre as telas, pode usar flags:
            // it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(it);
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

    /**
     * Varre a lista + quantidades do adapter, monta a lista de itens selecionados
     * e salva no storage central (CartStore) para o carrinho abrir populado.
     */
    private void salvarCarrinhoLocal() {
        ArrayList<Produto> selecionados = new ArrayList<>();
        for (int i = 0; i < lista.size(); i++) {
            Produto p = lista.get(i);
            int qtd = adapter.getQuantidadeByPosition(i);
            if (qtd > 0) {
                // cria uma cópia “leve” do produto e tenta setar a quantidade (se existir o setter)
                Produto copy = new Produto(p.getId(), p.getNome(), p.getPreco(), p.getImagemUrl());
                try {
                    copy.getClass().getMethod("setQuantidade", int.class).invoke(copy, qtd);
                } catch (Exception ignored) {
                    // se sua classe Produto não tiver setQuantidade, pode ajustar o CartStore
                    // para salvar a quantidade em paralelo (mas normalmente essa classe tem)
                }
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