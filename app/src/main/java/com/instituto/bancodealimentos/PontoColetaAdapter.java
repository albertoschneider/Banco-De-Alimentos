package com.instituto.bancodealimentos;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

public class PontoColetaAdapter extends RecyclerView.Adapter<PontoColetaAdapter.VH> {

    private final List<PontoColeta> data;

    public PontoColetaAdapter(List<PontoColeta> data) { this.data = data; }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ponto_coleta, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        PontoColeta p = data.get(position);

        h.tvNome.setText(p.getNome());
        h.tvEndereco.setText(p.getEndereco());

        // Distância (canto superior direito)
        if (p.getDistanciaKm() > 0) {
            h.tvDistancia.setText(String.format(Locale.getDefault(), "%.1fkm", p.getDistanciaKm()));
        } else {
            h.tvDistancia.setText("—");
        }

        // Disponibilidade (ícone + texto) + cor do botão
        final String disp = p.getDisponibilidade() != null ? p.getDisponibilidade() : "";
        boolean disponivel = disp.equalsIgnoreCase("Disponível");

        if (disponivel) {
            h.imgStatus.setImageResource(R.drawable.ic_status_ok);
            h.tvStatus.setText("Disponível");
            h.btnRota.setEnabled(true);
            h.btnRota.setBackground(ContextCompat.getDrawable(h.itemView.getContext(), R.drawable.bg_btn_rota_primary));
            h.btnRota.setTextColor(0xFFFFFFFF);
        } else {
            h.imgStatus.setImageResource(R.drawable.ic_status_off);
            h.tvStatus.setText("Indisponível");
            h.btnRota.setEnabled(false);
            h.btnRota.setBackground(ContextCompat.getDrawable(h.itemView.getContext(), R.drawable.bg_btn_rota_disabled));
            h.btnRota.setTextColor(0xFFFFFFFF);
        }

        // Abrir rota no Maps
        h.btnRota.setOnClickListener(v -> {
            if (!disponivel) return;
            if (p.getLat() != null && p.getLng() != null) {
                Uri gmm = Uri.parse("google.navigation:q=" + p.getLat() + "," + p.getLng() + "&mode=d");
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmm).setPackage("com.google.android.apps.maps");
                v.getContext().startActivity(mapIntent);
            }
        });
    }

    @Override
    public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvNome, tvEndereco, tvDistancia, tvStatus, btnRota;
        ImageView imgStatus;

        VH(@NonNull View itemView) {
            super(itemView);
            tvNome      = itemView.findViewById(R.id.tvNome);
            tvEndereco  = itemView.findViewById(R.id.tvEndereco);
            tvDistancia = itemView.findViewById(R.id.tvDistancia);
            tvStatus    = itemView.findViewById(R.id.tvStatus);
            imgStatus   = itemView.findViewById(R.id.imgStatus);
            btnRota     = itemView.findViewById(R.id.btnRota); // é um TextView com fundo arredondado
        }
    }
}