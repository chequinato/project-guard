package com.guard.alert;

import com.guard.model.EmergencyContact;
import com.guard.model.Incident;
import com.guard.model.User;

import java.util.List;

/**
 * Envia o alerta e cuida do ESCALONAMENTO de contatos.
 *
 * Fluxo (igual ao site):
 *   1. start()        -> manda para o contato de prioridade 1
 *   2. sem resposta em ESCALATION_TIMEOUT_MILLIS -> quem controla o relógio (a tela) chama escalate()
 *   3. escalate()     -> manda para o próximo da lista
 *   4. confirm()      -> um contato respondeu: para tudo
 *
 * O AlertService NÃO tem relógio. Isso deixa a classe testável sem esperar 2 minutos de verdade.
 */
public class AlertService {

    public static final long COUNTDOWN_MILLIS = 30_000;
    public static final long ESCALATION_TIMEOUT_MILLIS = 120_000;

    private final AlertChannel channel;
    private User user;
    private Incident incident;
    private List<EmergencyContact> queue;
    private int currentIndex = -1;

    public AlertService(AlertChannel channel) {
        if (channel == null) throw new IllegalArgumentException("Canal obrigatório");
        this.channel = channel;
    }

    /** Começa o alerta: manda para o primeiro contato da lista. */
    public void start(User user, Incident incident) {
        if (!user.hasContacts()) {
            throw new IllegalStateException("Cadastre pelo menos um contato de emergência");
        }
        this.user = user;
        this.incident = incident;
        this.queue = user.getContactsByPriority();
        this.currentIndex = 0;
        incident.markAlertSent();
        channel.send(queue.get(0), buildMessage(user, incident));
    }

    /**
     * O contato atual não respondeu a tempo: passa para o próximo da lista.
     *
     * @return true se mandou para alguém; false se a lista acabou (incidente fica NO_RESPONSE).
     */
    public boolean escalate() {
        if (currentIndex + 1 < queue.size()) {
            currentIndex++;
            channel.send(queue.get(currentIndex), buildMessage(user, incident));
            return true;
        }
        incident.markNoResponse();
        return false;
    }

    /** Algum contato respondeu "estou indo". */
    public void confirm() {
        incident.markConfirmedByContact();
    }

    public EmergencyContact getCurrentContact() {
        return currentIndex >= 0 ? queue.get(currentIndex) : null;
    }

    public EmergencyContact getNextContact() {
        return currentIndex >= 0 && currentIndex + 1 < queue.size() ? queue.get(currentIndex + 1) : null;
    }

    public String getChannelName() {
        return channel.getName();
    }

    /** Texto do SMS. Curto, em inglês, com o link do mapa. */
    public static String buildMessage(User user, Incident incident) {
        return "GUARD ALERT: " + user.getName() + " may have been in an accident ("
                + user.getSelectedVehicle().getDisplayName().toLowerCase() + "). "
                + "Location: " + incident.getMapLink();
    }

}
