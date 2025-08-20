package com.instituto.bancodealimentos;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProdutoAdapter extends RecyclerView.Adapter<ProdutoAdapter.VH> {

    public interface OnQuantityChange { void onChanged(); }

    private final Context ctx;
    private final List<Produto> data;
    private final OnQuantityChange onQuantityChange;
    private final NumberFormat currencyBr = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    // cache local de quantidades quando o modelo não possui get/setQuantidade
    private final Map<String, Integer> qtdLocal = new HashMap<>();

    public ProdutoAdapter(Context ctx, List<Produto> data, OnQuantityChange cb) {
        this.ctx = ctx;
        this.data = data;
        this.onQuantityChange = cb;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_produto_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Produto p = data.get(position);

        h.tvNome.setText(safeNome(p));
        h.tvPreco.setText(currencyBr.format(safePreco(p)));
        int q = getQtd(position, p);
        h.tvQtd.setText(String.valueOf(q));

        String url = safeImagemUrl(p);
        if (url != null && !url.isEmpty()) {
            try {
                Glide.with(ctx).load(url)
                        .centerCrop()
                        .placeholder(R.drawable.bg_image_placeholder)
                        .into(h.ivFoto);
            } catch (Throwable t) {
                // se Glide não estiver no projeto, evita crash
                h.ivFoto.setImageResource(R.drawable.bg_image_placeholder);
            }
        } else {
            h.ivFoto.setImageResource(R.drawable.bg_image_placeholder);
        }

        h.btnMais.setOnClickListener(v -> {
            int nova = getQtd(h.getBindingAdapterPosition(), p) + 1;
            setQtd(h.getBindingAdapterPosition(), p, nova);
            h.tvQtd.setText(String.valueOf(nova));
            if (onQuantityChange != null) onQuantityChange.onChanged();
        });

        h.btnMenos.setOnClickListener(v -> {
            int atual = getQtd(h.getBindingAdapterPosition(), p);
            if (atual > 0) {
                int nova = atual - 1;
                setQtd(h.getBindingAdapterPosition(), p, nova);
                h.tvQtd.setText(String.valueOf(nova));
                if (onQuantityChange != null) onQuantityChange.onChanged();
            }
        });
    }

    @Override public int getItemCount() { return data.size(); }

    // ===================== ViewHolder tolerante a IDs diferentes =====================
    static class VH extends RecyclerView.ViewHolder {
        ImageView ivFoto;
        TextView tvNome, tvQtd, tvPreco;
        ImageButton btnMais, btnMenos;

        VH(@NonNull View itemView) {
            super(itemView);
            Context c = itemView.getContext();

            ivFoto  = findViewByAnyId(itemView, c, new String[]{"ivFoto","img"}, ImageView.class);
            tvNome  = findViewByAnyId(itemView, c, new String[]{"tvNome","txtNome"}, TextView.class);
            tvPreco = findViewByAnyId(itemView, c, new String[]{"tvPreco","txtPreco"}, TextView.class);
            tvQtd   = findViewByAnyId(itemView, c, new String[]{"tvQtd","txtQtd"}, TextView.class);
            btnMais = findViewByAnyId(itemView, c, new String[]{"btnMais","btnPlus"}, ImageButton.class);
            btnMenos= findViewByAnyId(itemView, c, new String[]{"btnMenos","btnMinus"}, ImageButton.class);

            // avisos cedo, se algo essencial não for encontrado
            if (ivFoto == null)  throw new IllegalStateException("item_produto_card precisa de ivFoto ou img");
            if (tvNome == null)  throw new IllegalStateException("item_produto_card precisa de tvNome ou txtNome");
            if (tvPreco == null) throw new IllegalStateException("item_produto_card precisa de tvPreco ou txtPreco");
            if (tvQtd == null)   throw new IllegalStateException("item_produto_card precisa de tvQtd ou txtQtd");
            if (btnMais == null) throw new IllegalStateException("item_produto_card precisa de btnMais ou btnPlus");
            if (btnMenos == null)throw new IllegalStateException("item_produto_card precisa de btnMenos ou btnMinus");
        }

        @SuppressWarnings("unchecked")
        private static <T extends View> T findViewByAnyId(View root, Context c, String[] names, Class<T> clazz) {
            for (String n : names) {
                int id = c.getResources().getIdentifier(n, "id", c.getPackageName());
                if (id != 0) {
                    View v = root.findViewById(id);
                    if (v != null && clazz.isInstance(v)) {
                        return (T) v;
                    }
                }
            }
            return null;
        }
    }

    // ===================== Helpers compatíveis com modelos diferentes =====================
    private String key(int position, Produto p) {
        try {
            Object v = p.getClass().getMethod("getId").invoke(p);
            if (v != null) return String.valueOf(v);
        } catch (Exception ignored) {}
        return position + "_" + System.identityHashCode(p);
    }

    private int getQtd(int position, Produto p) {
        // tenta getQuantidade()
        try {
            Object v = p.getClass().getMethod("getQuantidade").invoke(p);
            if (v instanceof Number) return Math.max(0, ((Number) v).intValue());
        } catch (Exception ignored) {}
        // cache local
        String k = key(position, p);
        Integer q = qtdLocal.get(k);
        if (q == null) { q = 0; qtdLocal.put(k, q); }
        return q;
    }

    private void setQtd(int position, Produto p, int nova) {
        // tenta setQuantidade(int)
        try {
            p.getClass().getMethod("setQuantidade", int.class).invoke(p, Math.max(0, nova));
            return;
        } catch (Exception ignored) {}
        // cache local
        qtdLocal.put(key(position, p), Math.max(0, nova));
    }

    private String safeNome(Produto p) {
        try {
            Object v = p.getClass().getMethod("getNome").invoke(p);
            if (v != null) return String.valueOf(v);
        } catch (Exception ignored) {}
        return "";
    }

    private double safePreco(Produto p) {
        try {
            Object v = p.getClass().getMethod("getPreco").invoke(p);
            if (v instanceof Number) return ((Number) v).doubleValue();
        } catch (Exception ignored) {}
        return 0.0;
    }

    private String safeImagemUrl(Produto p) {
        try {
            Object v = p.getClass().getMethod("getImagemUrl").invoke(p);
            if (v != null) return String.valueOf(v);
        } catch (Exception ignored) {}
        return null;
    }
}