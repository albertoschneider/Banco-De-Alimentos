package com.instituto.bancodealimentos;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class ProdutoAdapter extends RecyclerView.Adapter<ProdutoAdapter.ProdutoViewHolder> {

    // Callback para avisar a Activity quando a quantidade mudar
    public interface OnQuantidadeChangeListener {
        void onQuantidadeChanged();
    }

    private final Context context;
    private final List<Produto> listaProdutos;
    private final NumberFormat currencyBr = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    private final OnQuantidadeChangeListener quantidadeChangeListener;

    public ProdutoAdapter(Context context, List<Produto> listaProdutos,
                          OnQuantidadeChangeListener listener) {
        this.context = context;
        this.listaProdutos = listaProdutos;
        this.quantidadeChangeListener = listener;
    }

    @NonNull
    @Override
    public ProdutoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_produto, parent, false);
        return new ProdutoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProdutoViewHolder holder, int position) {
        Produto produto = listaProdutos.get(position);

        holder.tvNomeProduto.setText(produto.getNome());
        holder.tvPrecoProduto.setText("Valor: " + currencyBr.format(produto.getPreco()));
        holder.imgProduto.setImageResource(produto.getImagemResId());
        holder.tvQuantidade.setText(String.valueOf(produto.getQuantidade()));

        holder.btnMais.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                Produto p = listaProdutos.get(pos);
                p.setQuantidade(p.getQuantidade() + 1);
                notifyItemChanged(pos);
                if (quantidadeChangeListener != null) quantidadeChangeListener.onQuantidadeChanged();
            }
        });

        holder.btnMenos.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                Produto p = listaProdutos.get(pos);
                if (p.getQuantidade() > 0) {
                    p.setQuantidade(p.getQuantidade() - 1);
                    notifyItemChanged(pos);
                    if (quantidadeChangeListener != null) quantidadeChangeListener.onQuantidadeChanged();
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return (listaProdutos != null) ? listaProdutos.size() : 0;
    }

    public static class ProdutoViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProduto;
        TextView tvNomeProduto, tvPrecoProduto, tvQuantidade;
        ImageButton btnMais, btnMenos;

        public ProdutoViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProduto = itemView.findViewById(R.id.imgProduto);
            tvNomeProduto = itemView.findViewById(R.id.tvNomeProduto);
            tvPrecoProduto = itemView.findViewById(R.id.tvPrecoProduto);
            tvQuantidade = itemView.findViewById(R.id.tvQuantidade);
            btnMais = itemView.findViewById(R.id.btnMais);
            btnMenos = itemView.findViewById(R.id.btnMenos);
        }
    }
}
