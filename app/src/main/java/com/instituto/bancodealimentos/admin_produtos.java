package com.instituto.bancodealimentos;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

/**
 * Lista de produtos para o ADMIN. Funciona mesmo que o layout não tenha
 * 'recyclerView' nem 'btn_add' (cria o RecyclerView programaticamente se faltar).
 */
public class admin_produtos extends AppCompatActivity {

    // ===== Modelo simples (sem depender de outros packages) =====
    public static class ProdutoItem {
        public String id;
        public String nome;
        public double preco;
        public String imagemUrl; // opcional

        public ProdutoItem(String id, String nome, double preco, String imagemUrl) {
            this.id = id;
            this.nome = nome == null ? "" : nome;
            this.preco = preco;
            this.imagemUrl = imagemUrl;
        }
    }

    private RecyclerView recyclerView;
    private AdminAdapter adapter;
    private final List<ProdutoItem> lista = new ArrayList<>();
    private ListenerRegistration listener;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final NumberFormat currencyBr = NumberFormat.getCurrencyInstance(new Locale("pt","BR"));

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_produtos);

        // Voltar
        ImageButton back = findViewById(R.id.btn_voltar);
        if (back != null) back.setOnClickListener(v -> onBackPressed());

        // Botão adicionar (se existir no layout)
        int idAdd = getResources().getIdentifier("btn_add", "id", getPackageName());
        if (idAdd != 0) {
            View add = findViewById(idAdd);
            if (add != null) add.setOnClickListener(v ->
                    startActivity(new Intent(this, criar_produto.class)));
        }

        // RecyclerView do layout (se existir)
        int idRv = getResources().getIdentifier("recyclerView", "id", getPackageName());
        recyclerView = (idRv != 0) ? findViewById(idRv) : null;

        // Se não existir, cria programaticamente e ancora entre header e footer (se houver)
        if (recyclerView == null) {
            recyclerView = criarRecyclerViewProgramaticamente();
        }

        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new AdminAdapter(this, lista, currencyBr);
        recyclerView.setAdapter(adapter);

        escutarProdutos();

        View header = findViewById(R.id.header); // o ConstraintLayout do topo
        ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop() + sb.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });
        ViewCompat.requestApplyInsets(header);
    }

    private void escutarProdutos() {
        listener = db.collection("produtos")
                .orderBy("nome", Query.Direction.ASCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        toast(e.getMessage());
                        return;
                    }
                    if (snap == null) return;

                    lista.clear();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        String id = d.getId();
                        String nome = d.getString("nome");
                        Double preco = d.getDouble("preco");
                        String imagemUrl = d.getString("imagemUrl");

                        lista.add(new ProdutoItem(
                                id,
                                nome,
                                preco == null ? 0.0 : preco,
                                imagemUrl
                        ));
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    @Override
    protected void onDestroy() {
        if (listener != null) listener.remove();
        super.onDestroy();
    }

    private void toast(String s) { android.widget.Toast.makeText(this, s, android.widget.Toast.LENGTH_SHORT).show(); }

    /**
     * Cria um RecyclerView e adiciona ao ConstraintLayout raiz (@id/main),
     * ancorando abaixo do header (@id/header) e acima do footer (@id/footer) se existir.
     */
    private RecyclerView criarRecyclerViewProgramaticamente() {
        ConstraintLayout root = findViewById(R.id.main);
        if (root == null) throw new IllegalStateException("Seu layout precisa ter ConstraintLayout com id @+id/main");

        RecyclerView rv = new RecyclerView(this);
        rv.setId(View.generateViewId());
        root.addView(rv, new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_CONSTRAINT,
                ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
        ));

        int idHeader = R.id.header; // existe nos seus layouts
        int idFooter = getResources().getIdentifier("footer", "id", getPackageName()); // pode não existir

        ConstraintSet cs = new ConstraintSet();
        cs.clone(root);

        cs.connect(rv.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 0);
        cs.connect(rv.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 0);

        if (findViewById(idHeader) != null) {
            cs.connect(rv.getId(), ConstraintSet.TOP, idHeader, ConstraintSet.BOTTOM, 0);
        } else {
            cs.connect(rv.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0);
        }

        if (idFooter != 0 && findViewById(idFooter) != null) {
            cs.connect(rv.getId(), ConstraintSet.BOTTOM, idFooter, ConstraintSet.TOP, 0);
        } else {
            cs.connect(rv.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 0);
        }

        cs.applyTo(root);
        return rv;
    }

    // ================== Adapter simples (sem XML de item) ==================
    private static class AdminAdapter extends RecyclerView.Adapter<AdminAdapter.VH> {

        private final android.content.Context ctx;
        private final List<ProdutoItem> data;
        private final NumberFormat currencyBr;

        AdminAdapter(android.content.Context ctx, List<ProdutoItem> data, NumberFormat currencyBr) {
            this.ctx = ctx;
            this.data = data;
            this.currencyBr = currencyBr;
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            // Card
            CardView card = new CardView(ctx);
            card.setUseCompatPadding(true);
            card.setRadius(dp(ctx, 12));
            card.setCardElevation(dp(ctx, 2));
            card.setContentPadding(dp(ctx, 12), dp(ctx, 12), dp(ctx, 12), dp(ctx, 12));

            // Layout interno vertical
            LinearLayout box = new LinearLayout(ctx);
            box.setOrientation(LinearLayout.VERTICAL);
            card.addView(box, new CardView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));

            // Nome
            TextView tvNome = new TextView(ctx);
            tvNome.setTextColor(0xFF163B5C);
            tvNome.setTextSize(16);
            tvNome.setTypeface(tvNome.getTypeface(), android.graphics.Typeface.BOLD);
            box.addView(tvNome);

            // Preço
            TextView tvPreco = new TextView(ctx);
            tvPreco.setTextColor(0xFF57748E);
            tvPreco.setTextSize(14);
            LinearLayout.LayoutParams lpPreco = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lpPreco.topMargin = dp(ctx, 4);
            box.addView(tvPreco, lpPreco);

            // "Editar"
            TextView btnEditar = new TextView(ctx);
            btnEditar.setText("Editar");
            btnEditar.setTextColor(0xFF004E7C);
            btnEditar.setTextSize(14);
            LinearLayout.LayoutParams lpEdit = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lpEdit.topMargin = dp(ctx, 8);
            box.addView(btnEditar, lpEdit);

            return new VH(card, tvNome, tvPreco, btnEditar);
        }

        @Override
        public void onBindViewHolder(VH h, int position) {
            ProdutoItem p = data.get(position);
            h.tvNome.setText(p.nome);
            h.tvPreco.setText(currencyBr.format(p.preco));

            View.OnClickListener goEdit = v -> {
                Intent i = new Intent(ctx, editar_produto.class);
                i.putExtra("produtoId", p.id);
                ctx.startActivity(i);
            };
            h.btnEditar.setOnClickListener(goEdit);
            h.itemView.setOnClickListener(goEdit);
        }

        @Override public int getItemCount() { return data.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvNome, tvPreco, btnEditar;
            VH(View itemView, TextView tvNome, TextView tvPreco, TextView btnEditar) {
                super(itemView);
                this.tvNome = tvNome;
                this.tvPreco = tvPreco;
                this.btnEditar = btnEditar;
            }
        }

        private static int dp(android.content.Context c, int v) {
            float d = c.getResources().getDisplayMetrics().density;
            return Math.round(v * d);
        }
    }
}