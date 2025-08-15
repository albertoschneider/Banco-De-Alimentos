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
import java.util.List;
import java.util.Locale;

public class ProdutoAdapter extends RecyclerView.Adapter<ProdutoAdapter.VH> {

    public interface OnQuantityChange {
        void onChanged();
    }

    private final Context ctx;
    private final List<Produto> data;
    private final OnQuantityChange onQuantityChange;
    private final NumberFormat currencyBr =
            NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

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

        h.tvNome.setText(p.getNome());
        h.tvPreco.setText(currencyBr.format(p.getPreco()));
        h.tvQtd.setText(String.valueOf(p.getQuantidade()));

        // Imagem (Glide com placeholder)
        if (p.getImagemUrl() != null && !p.getImagemUrl().isEmpty()) {
            Glide.with(ctx)
                    .load(p.getImagemUrl())
                    .centerCrop()
                    .placeholder(R.drawable.bg_image_placeholder)
                    .into(h.ivFoto);
        } else {
            h.ivFoto.setImageResource(R.drawable.bg_image_placeholder);
        }

        h.btnMais.setOnClickListener(v -> {
            p.setQuantidade(p.getQuantidade() + 1);
            h.tvQtd.setText(String.valueOf(p.getQuantidade()));
            if (onQuantityChange != null) onQuantityChange.onChanged();
        });

        h.btnMenos.setOnClickListener(v -> {
            if (p.getQuantidade() > 0) {
                p.setQuantidade(p.getQuantidade() - 1);
                h.tvQtd.setText(String.valueOf(p.getQuantidade()));
                if (onQuantityChange != null) onQuantityChange.onChanged();
            }
        });
    }

    @Override public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivFoto;
        TextView tvNome, tvQtd, tvPreco;
        ImageButton btnMais, btnMenos;

        VH(@NonNull View itemView) {
            super(itemView);
            ivFoto = itemView.findViewById(R.id.ivFoto);
            tvNome = itemView.findViewById(R.id.tvNome);
            tvQtd  = itemView.findViewById(R.id.tvQtd);
            tvPreco = itemView.findViewById(R.id.tvPreco);
            btnMais = itemView.findViewById(R.id.btnMais);
            btnMenos = itemView.findViewById(R.id.btnMenos);
        }
    }
}