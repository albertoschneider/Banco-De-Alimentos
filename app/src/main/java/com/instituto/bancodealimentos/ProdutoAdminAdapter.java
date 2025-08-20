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

public class ProdutoAdminAdapter extends RecyclerView.Adapter<ProdutoAdminAdapter.VH> {

    public interface OnEdit { void onEdit(Produto p); }

    private final List<Produto> data;
    private final OnEdit onEdit;

    public ProdutoAdminAdapter(List<Produto> data, OnEdit onEdit) {
        this.data = data;
        this.onEdit = onEdit;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_produto_admin, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        Produto p = data.get(pos);
        h.nome.setText(p.getNome());
        h.preco.setText(Money.fmt(p.getPreco() == null ? 0.0 : p.getPreco()));
        if (p.getImagemUrl() != null && !p.getImagemUrl().isEmpty())
            Glide.with(h.img.getContext()).load(p.getImagemUrl()).into(h.img);
        else h.img.setImageResource(android.R.color.darker_gray);

        h.btnEdit.setOnClickListener(v -> onEdit.onEdit(p));
    }

    @Override public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView img; TextView nome; TextView preco; ImageButton btnEdit;
        VH(@NonNull View v) {
            super(v);
            img = v.findViewById(R.id.img);
            nome = v.findViewById(R.id.txtNome);
            preco = v.findViewById(R.id.txtPreco);
            btnEdit = v.findViewById(R.id.btnEdit);
        }
    }
}