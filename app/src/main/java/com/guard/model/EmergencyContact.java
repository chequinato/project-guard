package com.guard.model;

/**
 * Um contato de emergência (quem será avisado se acontecer um acidente).
 *
 * CONCEITO: encapsulamento com validação
 * Os campos são privados e só mudam pelos setters, que VALIDAM antes de aceitar.
 * Resultado: é impossível existir um contato sem nome ou com telefone inválido.
 *
 * Prioridade 1 = primeiro a ser avisado, 2 = o segundo, e assim por diante.
 */
public class EmergencyContact {

    private long id;             // id do banco de dados (0 = ainda não foi salvo)
    private String name;
    private String phone;
    private String relationship; // "Sister", "Father"... (opcional)
    private int priority;

    public EmergencyContact(String name, String phone, String relationship, int priority) {
        // O construtor usa os próprios setters: a validação fica escrita em UM lugar só.
        setName(name);
        setPhone(phone);
        setRelationship(relationship);
        setPriority(priority);
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }

    public void setName(String name) {
        // name == null       -> ninguém passou nada
        // name.trim()        -> tira espaços do começo e do fim
        // .isEmpty()         -> sobrou texto vazio
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Contact name is required");
        }
        this.name = name.trim();
    }

    public String getPhone() { return phone; }

    public void setPhone(String phone) {
        if (phone == null) {
            throw new IllegalArgumentException("Phone is required");
        }
        // replaceAll usa uma "expressão regular" (regex):
        // [^0-9+] = "qualquer caractere que NÃO seja dígito nem +". Trocamos tudo isso por nada.
        // "+55 (11) 98888-0001" vira "+5511988880001".
        String clean = phone.replaceAll("[^0-9+]", "");
        // Contamos só os dígitos (sem o +). Menos de 8 não é um telefone válido.
        if (clean.replace("+", "").length() < 8) {
            throw new IllegalArgumentException("Invalid phone: " + phone);
        }
        this.phone = clean;
    }

    public String getRelationship() { return relationship; }

    public void setRelationship(String relationship) {
        // OPERADOR TERNÁRIO: condição ? valorSeVerdadeiro : valorSeFalso
        // É um if/else em uma linha. Aqui: se veio null, guarda texto vazio.
        this.relationship = relationship == null ? "" : relationship.trim();
    }

    public int getPriority() { return priority; }

    public void setPriority(int priority) {
        if (priority < 1) {
            throw new IllegalArgumentException("Priority starts at 1");
        }
        this.priority = priority;
    }

    /** Exemplo: "1. Ana (Sister)". */
    @Override
    public String toString() {
        return priority + ". " + name + (relationship.isEmpty() ? "" : " (" + relationship + ")");
    }
}
