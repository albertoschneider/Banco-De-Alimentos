package com.instituto.bancodealimentos;

public class AdminUser {
    private String id;       // id do documento no Firestore
    private String nome;
    private String email;
    private boolean isAdmin;

    public AdminUser() {} // Firestore precisa

    public AdminUser(String id, String nome, String email, boolean isAdmin) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.isAdmin = isAdmin;
    }

    public String getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public boolean isAdmin() { return isAdmin; }

    public void setId(String id) { this.id = id; }
    public void setNome(String nome) { this.nome = nome; }
    public void setEmail(String email) { this.email = email; }
    public void setAdmin(boolean admin) { isAdmin = admin; }
}
