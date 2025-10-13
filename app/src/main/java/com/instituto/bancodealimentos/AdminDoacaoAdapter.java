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
import java.util.Map;

public class AdminDoacaoAdapter extends RecyclerView.Adapter<AdminDoacaoAdapter.VH> {

    private final List<Doacao> full = new ArrayList<>();
    private final List<Doacao> filtered = new ArrayList<>();
    private Map<String, String> uidNameMap;

    private final Locale ptBR = new Locale("pt", "BR");
    private final NumberFormat currency = NumberFormat.getCurrencyInstance(ptBR);
    private final SimpleDateFormat sdfDate = new SimpleDateFormat("dd 'de' MMMM, yyyy", ptBR);
    private final SimpleDateFormat sdfId = new SimpleDateFormat("ddHHmmss", ptBR);

    public void setUidNameMap(Map<String, String> map) {
        this.uidNameMap = map;
        notifyDataSetChanged();
    }

    public void setItems(List<Doacao> items) {
        full.clear();
        if (items != null) full.addAll(items);
        applyFilter(null);
    }

    public void applyFilter(String q) {
        filtered.clear();
        if (q == null || q.trim().isEmpty()) {
            filtered.addAll(full);
        } else {
            String s = q.trim().toLowerCase(ptBR);
            for (Doacao d : full) {
                if (d == null) continue;

                String name = nameOf(d.getUid()).toLowerCase(ptBR);
                String desc = (d.getDescription() == null ? "" : d.getDescription()).toLowerCase(ptBR);
                String orderStr = String.valueOf(d.getOrderNumber() == null ? "" : d.getOrderNumber());

                // match: nome contém, ou nº do pedido contém, ou descrição contém
                if (name.contains(s) || orderStr.contains(s) || desc.contains(s)) {
                    filtered.add(d);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_doacao_admin, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Doacao d = filtered.get(position);

        // data
        h.tvDate.setText(formatDate(d.getCreatedAt()));

        // status
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

        // valor
        h.tvAmount.setText(currency.format(d.getAmountCents() / 100.0));

        // descrição
        String desc = d.getDescription();
        if (desc == null || desc.trim().isEmpty()) desc = "Doação via PIX";
        h.tvDescription.setText(desc);

        // nome
        h.tvUserName.setText("por " + nameOf(d.getUid()));

        // id à direita
        String id = d.getReferenceId();
        if (id == null || id.isEmpty()) {
            id = "PD" + (d.getOrderNumber() != null ? d.getOrderNumber() : 0) + "-" +
                    (d.getCreatedAt() != null ? sdfId.format(d.getCreatedAt().toDate()) : "000000");
        }
        h.tvId.setText("ID: " + id);

        // ADMIN: itens NÃO são clicáveis (pendentes inclusive)
        h.itemView.setOnClickListener(null);
        h.itemView.setClickable(false);
    }

    @Override public int getItemCount() { return filtered.size(); }

    private String nameOf(String uid) {
        if (uidNameMap == null || uid == null) return "—";
        String n = uidNameMap.get(uid);
        return n == null || n.trim().isEmpty() ? "—" : n;
    }

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
        TextView tvDate, tvStatusChip, tvAmount, tvDescription, tvUserName, tvId;
        VH(@NonNull View v) {
            super(v);
            tvDate = v.findViewById(R.id.tvDate);
            tvStatusChip = v.findViewById(R.id.tvStatusChip);
            tvAmount = v.findViewById(R.id.tvAmount);
            tvDescription = v.findViewById(R.id.tvDescription);
            tvUserName = v.findViewById(R.id.tvUserName);
            tvId = v.findViewById(R.id.tvId);
        }
    }
}
