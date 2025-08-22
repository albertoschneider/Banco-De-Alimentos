package com.instituto.bancodealimentos;

import java.io.Serializable;

public class Produto implements Serializable {
    private String id;
    private String nome;
    private Double preco;
    private String imagemUrl;

    // >>> NOVO: quantidade
    private int quantidade;

    public Produto() {
        this.quantidade = 0;
    }

    public Produto(String id, String nome, Double preco, String imagemUrl) {
        this.id = id;
        this.nome = nome;
        this.preco = preco;
        this.imagemUrl = imagemUrl;
        this.quantidade = 0; // default
    }

    public String getId() { return id; }
    public String getNome() { return nome; }
    public Double getPreco() { return preco; }
    public String getImagemUrl() { return imagemUrl; }

    public void setId(String id) { this.id = id; }
    public void setNome(String nome) { this.nome = nome; }
    public void setPreco(Double preco) { this.preco = preco; }
    public void setImagemUrl(String imagemUrl) { this.imagemUrl = imagemUrl; }

    // >>> getters/setters de quantidade
    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }
}