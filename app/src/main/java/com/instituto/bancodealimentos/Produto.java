package com.instituto.bancodealimentos;

public class Produto {
    private String nome;
    private double preco;
    private int imagemResId;
    private int quantidade;

    public Produto(String nome, double preco, int imagemResId) {
        this.nome = nome;
        this.preco = preco;
        this.imagemResId = imagemResId;
        this.quantidade = 0; // Começa com zero
    }

    public String getNome() { return nome; }
    public double getPreco() { return preco; }
    public int getImagemResId() { return imagemResId; }
    public int getQuantidade() { return quantidade; }

    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }
}

