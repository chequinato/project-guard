package com.guard.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * O dono do celular.
 *
 * POO: COMPOSIÇÃO. Um User "tem" uma lista de EmergencyContact e um Vehicle selecionado.
 * A lista é privada: de fora só dá pra mexer pelos métodos, que garantem as regras.
 */
public class User {

    private String name;
    private Vehicle selectedVehicle;
    private final List<EmergencyContact> contacts = new ArrayList<>();

    public User(String name, Vehicle selectedVehicle) {
        setName(name);
        setSelectedVehicle(selectedVehicle);
    }

    public String getName() { return name; }
    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Nome do usuário é obrigatório");
        }
        this.name = name.trim();
    }

    public Vehicle getSelectedVehicle() { return selectedVehicle; }
    public void setSelectedVehicle(Vehicle vehicle) {
        if (vehicle == null) {
            throw new IllegalArgumentException("Escolha um veículo");
        }
        this.selectedVehicle = vehicle;
    }

    /** Adiciona um contato. Não deixa duas pessoas com a mesma prioridade. */
    public void addContact(EmergencyContact contact) {
        if (contact == null) {
            throw new IllegalArgumentException("Contato nulo");
        }
        for (EmergencyContact c : contacts) {
            if (c.getPriority() == contact.getPriority()) {
                throw new IllegalStateException("Já existe um contato com prioridade " + contact.getPriority());
            }
        }
        contacts.add(contact);
    }

    public boolean removeContact(EmergencyContact contact) {
        return contacts.remove(contact);
    }

    /** Lista ordenada por prioridade (1, 2, 3...). É a ordem do escalonamento. */
    public List<EmergencyContact> getContactsByPriority() {
        List<EmergencyContact> sorted = new ArrayList<>(contacts);
        sorted.sort(Comparator.comparingInt(EmergencyContact::getPriority));
        return Collections.unmodifiableList(sorted);
    }

    public boolean hasContacts() {
        return !contacts.isEmpty();
    }
}
