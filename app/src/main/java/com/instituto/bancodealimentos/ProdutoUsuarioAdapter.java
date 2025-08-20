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

import java.util.List;

public class ProdutoUsuarioAdapter extends RecyclerView.Adapter<ProdutoUsuarioAdapter.VH> {

    public interface OnChange { void onChanged(); }

    private final List<Produto> data;
    private final int[] quantidades; // por posição
    private final OnChange onChange;

    public ProdutoUsuarioAdapter(List<Produto> data, OnChange onChange) {
        this.data = data;
        this.onChange = onChange;
        this.quantidades = new int[1000]; // simples; cresce de acordo com sua lista
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_produto_card, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        Produto p = data.get(pos);

        h.nome.setText(p.getNome());
        h.preco.setText(Money.fmt(p.getPreco() == null ? 0.0 : p.getPreco()));
        h.qtd.setText(String.valueOf(quantidades[pos]));

        if (p.getImagemUrl() != null && !p.getImagemUrl().isEmpty())
            Glide.with(h.img.getContext()).load(p.getImagemUrl()).into(h.img);
        else h.img.setImageResource(android.R.color.darker_gray);

        h.btnMinus.setOnClickListener(v -> {
            if (quantidades[pos] > 0) {
                quantidades[pos]--;
                h.qtd.setText(String.valueOf(quantidades[pos]));
                onChange.onChanged();
            }
        });

        h.btnPlus.setOnClickListener(v -> {
            quantidades[pos]++;
            h.qtd.setText(String.valueOf(quantidades[pos]));
            onChange.onChanged();
        });
    }

    @Override public int getItemCount() { return data.size(); }

    public int getQuantidadeByPosition(int position) {
        if (position < 0 || position >= quantidades.length) return 0;
        return quantidades[position];
    }

    public int[] dumpQuantidadesByOrder() {
        int n = Math.min(data.size(), quantidades.length);
        int[] out = new int[n];
        System.arraycopy(quantidades, 0, out, 0, n);
        return out;
    }

    public void restoreQuantidadesByOrder(int[] qs) {
        if (qs == null) return;
        int n = Math.min(qs.length, quantidades.length);
        System.arraycopy(qs, 0, quantidades, 0, n);
        notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView img; TextView nome; TextView preco; ImageButton btnMinus; TextView qtd; ImageButton btnPlus;
        VH(@NonNull View v) {
            super(v);
            img = v.findViewById(R.id.img);
            nome = v.findViewById(R.id.txtNome);
            preco = v.findViewById(R.id.txtPreco);
            btnMinus = v.findViewById(R.id.btnMinus);
            qtd = v.findViewById(R.id.txtQtd);
            btnPlus = v.findViewById(R.id.btnPlus);
        }
    }
}