package com.instituto.bancodealimentos;

import java.io.Serializable;

public class Produto implements Serializable {
    private String id;
    private String nome;
    private Double preco;
    private String imagemUrl;

    public Produto() {}

    public Produto(String id, String nome, Double preco, String imagemUrl) {
        this.id = id;
        this.nome = nome;
        this.preco = preco;
        this.imagemUrl = imagemUrl;
    }

    public String getId() { return id; }
    public String getNome() { return nome; }
    public Double getPreco() { return preco; }
    public String getImagemUrl() { return imagemUrl; }

    public void setId(String id) { this.id = id; }
    public void setNome(String nome) { this.nome = nome; }
    public void setPreco(Double preco) { this.preco = preco; }
    public void setImagemUrl(String imagemUrl) { this.imagemUrl = imagemUrl; }
}