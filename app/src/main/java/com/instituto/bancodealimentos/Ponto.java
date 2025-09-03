package com.instituto.bancodealimentos;

import java.util.Map;

public class Ponto {
    public String id;
    public String nome;
    public String endereco;
    public String disponibilidade; // "Disponível" | "Indisponível"
    public Double lat;
    public Double lng;

    public static Ponto from(String id, Map<String, Object> m) {
        Ponto p = new Ponto();
        p.id = id;
        if (m == null) return p;
        p.nome = (String) m.get("nome");
        p.endereco = (String) m.get("endereco");
        p.disponibilidade = (String) m.get("disponibilidade");
        Object loc = m.get("location");
        if (loc instanceof Map) {
            try {
                Map<?, ?> lm = (Map<?, ?>) loc;
                Object la = lm.get("lat");
                Object lo = lm.get("lng");
                if (la != null) p.lat = Double.valueOf(la.toString());
                if (lo != null) p.lng = Double.valueOf(lo.toString());
            } catch (Exception ignored) {}
        }
        return p;
    }

    public boolean isAtivo() {
        return "Disponível".equalsIgnoreCase(disponibilidade) || "Ativo".equalsIgnoreCase(disponibilidade);
    }
}
