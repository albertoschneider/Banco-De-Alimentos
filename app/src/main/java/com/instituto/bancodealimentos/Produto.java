package com.instituto.bancodealimentos;

import java.io.Serializable;

public class Produto implements Serializable {
    private final String docId;
    private final String nome;
    private final double preco;
    private final String imagemUrl; // pode ser null
    private int quantidade;

    public Produto(String docId, String nome, double preco, String imagemUrl) {
        this.docId = docId;
        this.nome = nome;
        this.preco = preco;
        this.imagemUrl = imagemUrl;
        this.quantidade = 0;
    }

    public String getDocId() { return docId; }
    public String getNome() { return nome; }
    public double getPreco() { return preco; }
    public String getImagemUrl() { return imagemUrl; }
    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int q) { this.quantidade = Math.max(q, 0); }
}