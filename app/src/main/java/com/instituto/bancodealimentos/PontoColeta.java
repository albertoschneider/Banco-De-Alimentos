package com.instituto.bancodealimentos;

public class PontoColeta {
    private final String nome;
    private final String endereco;
    private final String disponibilidade; // pode ser null
    private final Double lat;             // pode ser null
    private final Double lng;             // pode ser null
    private double distanciaKm;           // calculado em tempo real

    public PontoColeta(String nome, String endereco, String disponibilidade, Double lat, Double lng) {
        this.nome = nome;
        this.endereco = endereco;
        this.disponibilidade = disponibilidade;
        this.lat = lat;
        this.lng = lng;
        this.distanciaKm = 0.0;
    }

    public String getNome() { return nome; }
    public String getEndereco() { return endereco; }
    public String getDisponibilidade() { return disponibilidade; }
    public Double getLat() { return lat; }
    public Double getLng() { return lng; }

    public double getDistanciaKm() { return distanciaKm; }
    public void setDistanciaKm(double distanciaKm) { this.distanciaKm = distanciaKm; }
}
