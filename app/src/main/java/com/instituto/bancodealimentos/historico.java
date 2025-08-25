package com.instituto.bancodealimentos;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import androidx.annotation.Nullable;

public class historico extends AppCompatActivity {

    private RecyclerView rv;
    private HistoricoAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private final Locale LOCALE_BR = new Locale("pt", "BR");
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd 'de' MMMM, yyyy", LOCALE_BR);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);
        setContentView(R.layout.activity_historico);

        ImageButton back = findViewById(R.id.btn_voltar);
        back.setOnClickListener(v -> onBackPressed());

        rv = findViewById(R.id.rvHistorico);
        adapter = new HistoricoAdapter(this, new HistoricoAdapter.OnDonationClick() {
            @Override
            public void onClick(Doacao d) {
                if (d.status == Status.PENDENTE) {
                    // Abre sua tela de pagamento passando dados essenciais
                    Intent i = new Intent(historico.this, pagamento.class);
                    i.putExtra("donationId", d.id);
                    i.putExtra("orderId", d.orderId);
                    i.putExtra("amount", d.valorCentavos);
                    i.putExtra("txid", d.txid);         // se já existir cobrança aberta
                    i.putExtra("expiresAt", d.expiresAt); // millis
                    startActivity(i);
                } else {
                    Toast.makeText(historico.this, "Pagamento não disponível para este item.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        rv.setAdapter(adapter);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Faça login para ver seu histórico.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Estrutura: users/{uid}/donations
        Query q = db.collection("users").document(user.getUid())
                .collection("donations")
                .orderBy("createdAt", Query.Direction.DESCENDING);

        q.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Toast.makeText(historico.this, "Erro ao carregar histórico.", Toast.LENGTH_SHORT).show();
                    return;
                }
                List<Doacao> list = new ArrayList<>();
                if (snapshots != null) {
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Doacao d = Doacao.from(doc);
                        if (d != null) list.add(d);
                    }
                }
                adapter.submitList(list);
            }
        });
    }

    // ======== MODELS ========

    enum Status { PAGO, PENDENTE, CANCELADO }

    static class ItemDoado {
        String nome;   // ex: "Arroz"
        String detalhe; // ex: "(5kg)" ou "(1)"
        static ItemDoado from(Map<String, Object> m) {
            if (m == null) return null;
            ItemDoado it = new ItemDoado();
            it.nome = (String) m.get("nome");
            it.detalhe = (String) m.get("detalhe");
            return it;
        }
    }

    static class Doacao {
        String id;              // id do doc em donations
        String orderId;         // id lógico do pedido
        long createdAt;         // millis
        long expiresAt;         // millis da cobrança PIX (se pendente)
        int valorCentavos;
        Status status;
        String txid;            // txid da cobrança PIX (se houver)
        String codigoCurto;     // exibir no "ID: 12345ABC"
        List<ItemDoado> itens = new ArrayList<>();

        static Doacao from(DocumentSnapshot doc) {
            try {
                Doacao d = new Doacao();
                d.id = doc.getId();
                d.orderId = doc.getString("orderId");
                d.createdAt = doc.getLong("createdAt") == null ? 0 : doc.getLong("createdAt");
                d.expiresAt = doc.getLong("expiresAt") == null ? 0 : doc.getLong("expiresAt");
                d.valorCentavos = doc.getLong("amount") == null ? 0 : doc.getLong("amount").intValue();
                String st = doc.getString("status");
                if (st == null) st = "PENDENTE";
                switch (st.toUpperCase()) {
                    case "PAGO": d.status = Status.PAGO; break;
                    case "CANCELADO": d.status = Status.CANCELADO; break;
                    default: d.status = Status.PENDENTE; break;
                }
                d.txid = doc.getString("txid");
                d.codigoCurto = doc.getString("shortId");
                List<Map<String, Object>> arr = (List<Map<String, Object>>) doc.get("items");
                if (arr != null) {
                    for (Map<String, Object> it : arr) {
                        ItemDoado i = ItemDoado.from(it);
                        if (i != null) d.itens.add(i);
                    }
                }
                return d;
            } catch (Exception e) { return null; }
        }
    }

    // ======== ADAPTER ========

    static class HistoricoAdapter extends ListAdapter<Doacao, HistoricoAdapter.Holder> {

        interface OnDonationClick { void onClick(Doacao d); }

        private final Context ctx;
        private final OnDonationClick clickCb;

        protected HistoricoAdapter(Context ctx, OnDonationClick cb) {
            super(DIFF);
            this.ctx = ctx;
            this.clickCb = cb;
        }

        @NonNull @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_historico_doacao, parent, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            Doacao d = getItem(position);
            h.bind(ctx, d, clickCb);
        }

        static final DiffUtil.ItemCallback<Doacao> DIFF = new DiffUtil.ItemCallback<Doacao>() {
            @Override public boolean areItemsTheSame(@NonNull Doacao a, @NonNull Doacao b) { return TextUtils.equals(a.id, b.id); }
            @Override
            public boolean areContentsTheSame(@NonNull Doacao a, @NonNull Doacao b) {
                return a.id.equals(b.id)
                        && a.valorCentavos == b.valorCentavos
                        && ((a.status == null && b.status == null) || (a.status != null && a.status.equals(b.status)))
                        && a.createdAt == b.createdAt
                        && a.expiresAt == b.expiresAt;
            }
        };

        static class Holder extends RecyclerView.ViewHolder {
            private final TextView tvData, tvStatus, tvValor, tvId;
            private final LinearLayout llItens;
            private final MaterialCardView card;
            private final NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("pt","BR"));
            private final SimpleDateFormat sdf = new SimpleDateFormat("dd 'de' MMMM, yyyy", new Locale("pt","BR"));

            Holder(@NonNull View itemView) {
                super(itemView);
                tvData = itemView.findViewById(R.id.tvData);
                tvStatus = itemView.findViewById(R.id.tvStatus);
                tvValor = itemView.findViewById(R.id.tvValor);
                tvId = itemView.findViewById(R.id.tvId);
                llItens = itemView.findViewById(R.id.llItens);
                card = (MaterialCardView) itemView;
            }

            void bind(Context ctx, Doacao d, OnDonationClick clickCb) {
                tvData.setText(sdf.format(d.createdAt));
                tvValor.setText(nf.format(d.valorCentavos / 100.0));
                tvId.setText("ID: " + (TextUtils.isEmpty(d.codigoCurto) ? d.id : d.codigoCurto));

                // Chip de status
                switch (d.status) {
                    case PAGO:
                        tvStatus.setText("Pago");
                        tvStatus.setBackgroundResource(R.drawable.bg_chip_pago);
                        break;
                    case PENDENTE:
                        tvStatus.setText("Pendente");
                        tvStatus.setBackgroundResource(R.drawable.bg_chip_pendente);
                        break;
                    case CANCELADO:
                        tvStatus.setText("Cancelado");
                        tvStatus.setBackgroundResource(R.drawable.bg_chip_cancelado);
                        break;
                }

                // Lista de itens (bullets)
                llItens.removeAllViews();
                if (d.itens != null) {
                    for (ItemDoado it : d.itens) {
                        TextView t = new TextView(ctx);
                        t.setText("• " + it.nome + (TextUtils.isEmpty(it.detalhe) ? "" : " " + it.detalhe));
                        t.setTextSize(14);
                        t.setTextColor(0xFF0F2640);
                        t.setPadding(0, 4, 0, 4);
                        llItens.addView(t);
                    }
                }

                // Clique apenas se pendente
                itemView.setClickable(d.status == Status.PENDENTE);
                itemView.setOnClickListener(v -> {
                    if (d.status == Status.PENDENTE) clickCb.onClick(d);
                });

                // Feedback visual: pendente tem leve sombra; demais, sem
                card.setCardElevation(d.status == Status.PENDENTE ? 2f : 0f);
            }
        }
    }
}
