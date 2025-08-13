package com.instituto.bancodealimentos;

public class PontoColeta {
    private final String nome;
    private final String endereco;
    private final double distanciaKm;
    private final String disponibilidade; // pode ser null

    public PontoColeta(String nome, String endereco, double distanciaKm, String disponibilidade) {
        this.nome = nome;
        this.endereco = endereco;
        this.distanciaKm = distanciaKm;
        this.disponibilidade = disponibilidade;
    }

    public String getNome() { return nome; }
    public String getEndereco() { return endereco; }
    public double getDistanciaKm() { return distanciaKm; }
    public String getDisponibilidade() { return disponibilidade; }
}