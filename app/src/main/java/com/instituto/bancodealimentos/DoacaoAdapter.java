package com.instituto.bancodealimentos;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DoacaoAdapter extends RecyclerView.Adapter<DoacaoAdapter.VH> {

    private final List<Doacao> items = new ArrayList<>();
    private final Locale ptBR = new Locale("pt", "BR");
    private final NumberFormat currency = NumberFormat.getCurrencyInstance(ptBR);
    private final SimpleDateFormat sdfDate = new SimpleDateFormat("dd 'de' MMMM, yyyy", ptBR);
    private final SimpleDateFormat sdfId = new SimpleDateFormat("ddHHmmss", ptBR); // fallback p/ id curto

    public void setItems(List<Doacao> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_doacao, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Doacao d = items.get(position);

        // Data (linha de cima, esquerda)
        h.tvDate.setText(formatDate(d.getCreatedAt()));

        // Status chip (cores)
        String status = d.getStatus();
        h.tvStatusChip.setText(statusLabel(status));
        int bg = R.drawable.bg_chip_neutral;
        int fg = 0xFF1F2937;
        switch (status) {
            case "paid":    bg = R.drawable.bg_chip_success; fg = 0xFF14532D; break;
            case "pending": bg = R.drawable.bg_chip_warning; fg = 0xFF7C2D12; break;
            case "expired": bg = R.drawable.bg_chip_error;   fg = 0xFF7F1D1D; break;
        }
        h.tvStatusChip.setBackgroundResource(bg);
        h.tvStatusChip.setTextColor(fg);

        // Valor
        h.tvAmount.setText(currency.format(d.getAmountCents() / 100.0));

        // “Itens doados:” – como é PIX, usamos a descrição ou PIX
        String desc = d.getDescription();
        if (desc == null || desc.trim().isEmpty()) {
            desc = "Doação via PIX";
        }
        h.tvDescription.setText(desc);

        // ID (direita, cinza) – usa referenceId se tiver, senão gera um curto
        String id = d.getReferenceId();
        if (id == null || id.isEmpty()) {
            id = "PD" + (d.getOrderNumber() != null ? d.getOrderNumber() : 0) + "-" +
                    (d.getCreatedAt() != null ? sdfId.format(d.getCreatedAt().toDate()) : "000000");
        }
        h.tvId.setText("ID: " + id);
    }

    @Override public int getItemCount() { return items.size(); }

    private String statusLabel(String s) {
        if ("paid".equals(s)) return "Pago";
        if ("pending".equals(s)) return "Pendente";
        if ("expired".equals(s)) return "Cancelado";
        return "—";
    }

    private String formatDate(Timestamp ts) {
        if (ts == null) return "—";
        return sdfDate.format(ts.toDate());
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvDate, tvStatusChip, tvAmount, tvDescription, tvId;
        VH(@NonNull View v) {
            super(v);
            tvDate = v.findViewById(R.id.tvDate);
            tvStatusChip = v.findViewById(R.id.tvStatusChip);
            tvAmount = v.findViewById(R.id.tvAmount);
            tvDescription = v.findViewById(R.id.tvDescription);
            tvId = v.findViewById(R.id.tvId);
        }
    }
}
