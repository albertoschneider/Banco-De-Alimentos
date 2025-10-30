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
    private final String currentUserId;

    public AdminAdapter(OnExcluirClick callback, String currentUserId) {
        this.callback = callback;
        this.currentUserId = currentUserId;
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

        // Verifica se é o usuário atual
        boolean isCurrentUser = u.getId().equals(currentUserId);

        // Adiciona "Você" ao nome se for o usuário atual
        String nomeExibir = u.getNome();
        if (isCurrentUser) {
            nomeExibir = u.getNome() + " (Você)";
        }

        h.nome.setText(nomeExibir);
        h.email.setText(u.getEmail());

        // Esconde o botão de excluir se for o usuário atual
        if (isCurrentUser) {
            h.excluir.setVisibility(View.GONE);
        } else {
            h.excluir.setVisibility(View.VISIBLE);
            h.excluir.setOnClickListener(v -> {
                if (callback != null) callback.onExcluir(u);
            });
        }
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