package com.instituto.bancodealimentos;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class CarrinhoAdapter extends RecyclerView.Adapter<CarrinhoAdapter.VH> {
    public interface OnChangeListener { void onChanged(); }

    private final List<Produto> itens;
    private final OnChangeListener listener;
    private final NumberFormat br = NumberFormat.getCurrencyInstance(new Locale("pt","BR"));

    public CarrinhoAdapter(List<Produto> itens, OnChangeListener l) {
        this.itens = itens;
        this.listener = l;
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvNome, tvPreco, tvQtd;
        ImageButton btnMenos, btnMais;
        VH(@NonNull View v) {
            super(v);
            tvNome = v.findViewById(R.id.tvNome);
            tvPreco = v.findViewById(R.id.tvPreco);
            tvQtd = v.findViewById(R.id.tvQtd);
            btnMenos = v.findViewById(R.id.btnMenos);
            btnMais = v.findViewById(R.id.btnMais);
        }
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int vt) {
        View v = LayoutInflater.from(p.getContext())
                .inflate(R.layout.row_cart_item, p, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int position) {
        Produto p = itens.get(position);
        h.tvNome.setText(p.getNome());
        h.tvPreco.setText(br.format(p.getPreco()));
        h.tvQtd.setText(String.valueOf(p.getQuantidade()));

        h.btnMais.setOnClickListener(v -> {
            int pos = h.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                Produto x = itens.get(pos);
                x.setQuantidade(x.getQuantidade() + 1);
                notifyItemChanged(pos);
                if (listener != null) listener.onChanged();
            }
        });

        h.btnMenos.setOnClickListener(v -> {
            int pos = h.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                Produto x = itens.get(pos);
                if (x.getQuantidade() > 1) {
                    x.setQuantidade(x.getQuantidade() - 1);
                    notifyItemChanged(pos);
                } else {
                    itens.remove(pos);
                    notifyItemRemoved(pos);
                }
                if (listener != null) listener.onChanged();
            }
        });
    }

    @Override public int getItemCount() { return itens.size(); }
}