package com.instituto.bancodealimentos;

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

public class CarrinhoAdapter extends RecyclerView.Adapter<CarrinhoAdapter.VH> {
    public interface OnChangeListener { void onChanged(); }

    private final List<Produto> itens;
    private final OnChangeListener listener;
    private final NumberFormat br = NumberFormat.getCurrencyInstance(new Locale("pt","BR"));

    // Cache local de quantidades (se Produto não tiver get/setQuantidade)
    private final Map<String, Integer> qtdLocal = new HashMap<>();

    public CarrinhoAdapter(List<Produto> itens, OnChangeListener l) {
        this.itens = itens;
        this.listener = l;
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivFoto;
        TextView tvNome, tvPreco, tvQtd;
        ImageButton btnMenos, btnMais, btnRemover;
        VH(@NonNull View v) {
            super(v);
            ivFoto     = v.findViewById(R.id.ivFoto);
            tvNome     = v.findViewById(R.id.tvNome);
            tvPreco    = v.findViewById(R.id.tvPreco);
            tvQtd      = v.findViewById(R.id.tvQtd);
            btnMenos   = v.findViewById(R.id.btnMenos);
            btnMais    = v.findViewById(R.id.btnMais);
            btnRemover = v.findViewById(R.id.btnRemover);
        }
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int vt) {
        // ✅ Usa SEMPRE o layout correto do item do carrinho
        View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_carrinho, p, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int position) {
        Produto p = itens.get(position);

        h.tvNome.setText(safeNome(p));
        h.tvPreco.setText(br.format(safePreco(p)));

        // Thumb do produto
        Glide.with(h.ivFoto.getContext())
                .load(safeImagemUrl(p))
                .placeholder(R.drawable.bg_thumb_round)
                .error(R.drawable.bg_thumb_round)
                .centerCrop()
                .into(h.ivFoto);

        int qtd = getQtd(position, p);
        h.tvQtd.setText(String.valueOf(qtd));

        h.btnMais.setOnClickListener(v -> {
            int pos = h.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            Produto x = itens.get(pos);
            int nova = getQtd(pos, x) + 1;
            setQtd(pos, x, nova);
            notifyItemChanged(pos);
            if (listener != null) listener.onChanged();
        });

        h.btnMenos.setOnClickListener(v -> {
            int pos = h.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            Produto x = itens.get(pos);
            int atual = getQtd(pos, x);
            if (atual > 1) {
                setQtd(pos, x, atual - 1);
                notifyItemChanged(pos);
            } else {
                // Se quantidade cair para 0, remove item
                removeAt(pos, x);
            }
            if (listener != null) listener.onChanged();
        });

        h.btnRemover.setOnClickListener(v -> {
            int pos = h.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            removeAt(pos, itens.get(pos));
            if (listener != null) listener.onChanged();
        });
    }

    @Override public int getItemCount() { return itens.size(); }

    // ------- remoção helper -------
    private void removeAt(int pos, Produto x) {
        itens.remove(pos);
        qtdLocal.remove(key(pos, x));
        notifyItemRemoved(pos);
    }

    // -------- helpers de compatibilidade --------
    private String key(int position, Produto p) {
        try {
            Object v = p.getClass().getMethod("getId").invoke(p);
            if (v != null) return String.valueOf(v);
        } catch (Exception ignored) {}
        return position + "_" + System.identityHashCode(p);
    }

    private int getQtd(int position, Produto p) {
        try {
            Object v = p.getClass().getMethod("getQuantidade").invoke(p);
            if (v instanceof Number) return Math.max(1, ((Number) v).intValue());
        } catch (Exception ignored) {}
        String k = key(position, p);
        Integer q = qtdLocal.get(k);
        if (q == null || q < 1) {
            q = 1;
            qtdLocal.put(k, q);
        }
        return q;
    }

    private void setQtd(int position, Produto p, int nova) {
        try {
            p.getClass().getMethod("setQuantidade", int.class).invoke(p, nova);
            return;
        } catch (Exception ignored) {}
        qtdLocal.put(key(position, p), Math.max(1, nova));
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
