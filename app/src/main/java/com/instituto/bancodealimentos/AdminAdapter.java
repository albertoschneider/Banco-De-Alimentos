package com.instituto.bancodealimentos;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AdminAdapter extends RecyclerView.Adapter<AdminAdapter.AdminVH> {

    public interface OnExcluirClick {
        void onExcluir(AdminUser user);
    }

    private final List<AdminUser> data = new ArrayList<>();
    private final OnExcluirClick callback;

    public AdminAdapter(OnExcluirClick callback) {
        this.callback = callback;
    }

    public void setItems(List<AdminUser> items) {
        data.clear();
        data.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public AdminVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin, parent, false);
        return new AdminVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull AdminVH h, int i) {
        AdminUser u = data.get(i);
        h.nome.setText(u.getNome());
        h.email.setText(u.getEmail());
        h.excluir.setOnClickListener(v -> {
            if (callback != null) callback.onExcluir(u);
        });
    }

    @Override
    public int getItemCount() { return data.size(); }

    static class AdminVH extends RecyclerView.ViewHolder {
        TextView nome, email;
        ImageButton excluir;
        AdminVH(@NonNull View itemView) {
            super(itemView);
            nome = itemView.findViewById(R.id.txtNome);
            email = itemView.findViewById(R.id.txtEmail);
            excluir = itemView.findViewById(R.id.btnExcluir);
        }
    }
}
