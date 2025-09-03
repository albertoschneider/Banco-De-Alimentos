package com.instituto.bancodealimentos;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PontosAdapter extends RecyclerView.Adapter<PontosAdapter.VH> {

    public interface Listener {
        void onEditar(Ponto p);
        void onExcluir(Ponto p);
    }

    private final List<Ponto> data;
    private final Listener listener;

    public PontosAdapter(List<Ponto> data, Listener l) {
        this.data = data;
        this.listener = l;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ponto_admin, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        Ponto p = data.get(pos);
        h.tvNome.setText(p.nome == null ? "" : p.nome);
        h.tvEndereco.setText(p.endereco == null ? "" : p.endereco);

        if (p.isAtivo()) {
            h.imgStatus.setImageResource(R.drawable.dot_green);
            h.tvStatus.setText("Ativo");
            h.tvStatus.setTextColor(0xFF16A34A);
        } else {
            h.imgStatus.setImageResource(R.drawable.dot_red);
            h.tvStatus.setText("Inativo");
            h.tvStatus.setTextColor(0xFFEF4444);
        }

        h.btnEditar.setOnClickListener(v -> { if (listener != null) listener.onEditar(p); });
        h.btnExcluir.setOnClickListener(v -> { if (listener != null) listener.onExcluir(p); });
    }

    @Override public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvNome, tvEndereco, tvStatus;
        ImageView imgStatus;
        ImageButton btnEditar, btnExcluir;
        VH(@NonNull View v) {
            super(v);
            tvNome = v.findViewById(R.id.tvNome);
            tvEndereco = v.findViewById(R.id.tvEndereco);
            tvStatus = v.findViewById(R.id.tvStatus);
            imgStatus = v.findViewById(R.id.imgStatus);
            btnEditar = v.findViewById(R.id.btnEditar);
            btnExcluir = v.findViewById(R.id.btnExcluir);
        }
    }
}
