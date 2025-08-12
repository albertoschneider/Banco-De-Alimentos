// Produto.java
package com.instituto.bancodealimentos;

import android.os.Parcel;
import android.os.Parcelable;

public class Produto implements Parcelable {
    private String nome;
    private double preco;
    private int imagemResId;
    private int quantidade;

    public Produto(String nome, double preco, int imagemResId) {
        this.nome = nome;
        this.preco = preco;
        this.imagemResId = imagemResId;
        this.quantidade = 0;
    }

    // 🔹 Construtor usado pelo Parcelable
    protected Produto(Parcel in) {
        nome = in.readString();
        preco = in.readDouble();
        imagemResId = in.readInt();
        quantidade = in.readInt();
    }

    // 🔹 CREATOR obrigatório
    public static final Creator<Produto> CREATOR = new Creator<Produto>() {
        @Override
        public Produto createFromParcel(Parcel in) {
            return new Produto(in);
        }

        @Override
        public Produto[] newArray(int size) {
            return new Produto[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(nome);
        parcel.writeDouble(preco);
        parcel.writeInt(imagemResId);
        parcel.writeInt(quantidade);
    }

    // Getters e Setters
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public double getPreco() { return preco; }
    public void setPreco(double preco) { this.preco = preco; }
    public int getImagemResId() { return imagemResId; }
    public void setImagemResId(int imagemResId) { this.imagemResId = imagemResId; }
    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }
}

