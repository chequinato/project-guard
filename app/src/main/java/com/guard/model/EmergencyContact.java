package com.guard.model;

/**
 * Um contato de emergência.
 *
 * POO: ENCAPSULAMENTO. Ninguém altera os campos direto: os setters validam antes.
 * Prioridade 1 = primeiro a ser avisado.
 */
public class EmergencyContact {

    private long id;            // id do banco (0 = ainda não salvo)
    private String name;
    private String phone;
    private String relationship; // "Sister", "Father"... opcional
    private int priority;

    public EmergencyContact(String name, String phone, String relationship, int priority) {
        setName(name);
        setPhone(phone);
        setRelationship(relationship);
        setPriority(priority);
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Nome do contato é obrigatório");
        }
        this.name = name.trim();
    }

    public String getPhone() { return phone; }
    public void setPhone(String phone) {
        if (phone == null) {
            throw new IllegalArgumentException("Telefone é obrigatório");
        }
        // Guarda só dígitos e o "+" do código do país.
        String clean = phone.replaceAll("[^0-9+]", "");
        if (clean.replace("+", "").length() < 8) {
            throw new IllegalArgumentException("Telefone inválido: " + phone);
        }
        this.phone = clean;
    }

    public String getRelationship() { return relationship; }
    public void setRelationship(String relationship) {
        this.relationship = relationship == null ? "" : relationship.trim();
    }

    public int getPriority() { return priority; }
    public void setPriority(int priority) {
        if (priority < 1) {
            throw new IllegalArgumentException("Prioridade começa em 1");
        }
        this.priority = priority;
    }

    @Override
    public String toString() {
        return priority + ". " + name + (relationship.isEmpty() ? "" : " (" + relationship + ")");
    }
}
