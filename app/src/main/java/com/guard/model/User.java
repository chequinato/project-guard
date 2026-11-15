package com.guard.model;

import com.guard.model.vehicle.Vehicle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * O dono do celular.
 *
 * CONCEITO: composição ("tem um")
 * - Herança é "É UM"  -> Car é um Vehicle.
 * - Composição é "TEM UM" -> User tem um Vehicle e tem vários EmergencyContact.
 *
 * A lista de contatos é privada: de fora só dá para mexer pelos métodos daqui,
 * que garantem a regra "duas pessoas não podem ter a mesma prioridade".
 */
public class User {

    private String name;
    private Vehicle selectedVehicle;

    // GENERICS: List<EmergencyContact> = uma lista que SÓ aceita EmergencyContact.
    // O "<>" vazio à direita (diamond) deixa o Java deduzir o tipo pelo lado esquerdo.
    // "List" é a interface (o contrato); "ArrayList" é a implementação de verdade.
    // "final" aqui significa que a variável sempre aponta para a MESMA lista
    // (a lista em si pode ganhar e perder itens).
    private final List<EmergencyContact> contacts = new ArrayList<>();

    public User(String name, Vehicle selectedVehicle) {
        setName(name);
        setSelectedVehicle(selectedVehicle);
    }

    public String getName() { return name; }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("User name is required");
        }
        this.name = name.trim();
    }

    public Vehicle getSelectedVehicle() { return selectedVehicle; }

    public void setSelectedVehicle(Vehicle vehicle) {
        if (vehicle == null) {
            throw new IllegalArgumentException("Choose a vehicle");
        }
        this.selectedVehicle = vehicle;
    }

    /** Adiciona um contato. Não deixa duas pessoas com a mesma prioridade. */
    public void addContact(EmergencyContact contact) {
        if (contact == null) {
            throw new IllegalArgumentException("Contact is null");
        }
        // FOR-EACH: "para cada contato c dentro de contacts, faça..."
        for (EmergencyContact c : contacts) {
            if (c.getPriority() == contact.getPriority()) {
                // IllegalStateException = "o pedido até é válido, mas o estado atual não permite".
                throw new IllegalStateException("There is already a contact with priority " + contact.getPriority());
            }
        }
        contacts.add(contact);
    }

    /** Remove um contato. Retorna true se ele estava na lista. */
    public boolean removeContact(EmergencyContact contact) {
        return contacts.remove(contact);
    }

    /**
     * Devolve os contatos ordenados por prioridade (1, 2, 3...). É a ordem do escalonamento.
     */
    public List<EmergencyContact> getContactsByPriority() {
        // Fazemos uma CÓPIA para ordenar: assim a lista original não muda.
        List<EmergencyContact> sorted = new ArrayList<>(contacts);

        // METHOD REFERENCE: "EmergencyContact::getPriority" quer dizer
        // "para comparar dois contatos, use o getPriority() de cada um".
        sorted.sort(Comparator.comparingInt(EmergencyContact::getPriority));

        // unmodifiableList devolve uma "vitrine": dá para ler, mas não para adicionar ou remover.
        // Se alguém tentar, o Java lança erro. Isso protege as regras desta classe.
        return Collections.unmodifiableList(sorted);
    }

    /** "!" = NÃO lógico. Tem contatos = a lista NÃO está vazia. */
    public boolean hasContacts() {
        return !contacts.isEmpty();
    }
}
