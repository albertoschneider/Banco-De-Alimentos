package com.instituto.bancodealimentos;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

public class PontoColetaAdapter extends RecyclerView.Adapter<PontoColetaAdapter.VH> {

    private final List<PontoColeta> data;

    public PontoColetaAdapter(List<PontoColeta> data) {
        this.data = data;
    }

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

        if (p.getDisponibilidade() != null && !p.getDisponibilidade().isEmpty()) {
            h.tvDisponibilidade.setText(p.getDisponibilidade());
            h.tvDisponibilidade.setVisibility(View.VISIBLE);
        } else {
            h.tvDisponibilidade.setVisibility(View.GONE);
        }

        if (p.getDistanciaKm() > 0) {
            h.chipDistancia.setText(String.format(Locale.getDefault(), "%.1f km", p.getDistanciaKm()));
        } else {
            h.chipDistancia.setText("—");
        }

        h.btnVerRota.setOnClickListener(v -> {
            if (p.getLat() != null && p.getLng() != null) {
                Uri gmmIntentUri = Uri.parse("google.navigation:q=" + p.getLat() + "," + p.getLng() + "&mode=d");
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                v.getContext().startActivity(mapIntent);
            }
        });
    }

    @Override
    public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvNome, tvEndereco, tvDisponibilidade;
        Chip chipDistancia;
        MaterialButton btnVerRota;

        VH(@NonNull View itemView) {
            super(itemView);
            tvNome = itemView.findViewById(R.id.tvNome);
            tvEndereco = itemView.findViewById(R.id.tvEndereco);
            tvDisponibilidade = itemView.findViewById(R.id.tvDisponibilidade);
            chipDistancia = itemView.findViewById(R.id.chipDistancia);
            btnVerRota = itemView.findViewById(R.id.btnVerRota);
        }
    }
}
