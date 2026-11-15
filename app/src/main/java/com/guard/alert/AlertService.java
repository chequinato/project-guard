package com.guard.alert;

import com.guard.model.EmergencyContact;
import com.guard.model.Incident;
import com.guard.model.User;

import java.util.List;

/**
 * Envia o alerta e cuida do ESCALONAMENTO dos contatos.
 *
 * Fluxo (igual ao site):
 *   1. start()    -> manda para o contato de prioridade 1
 *   2. ninguém respondeu a tempo -> quem controla o relógio (a tela) chama escalate()
 *   3. escalate() -> manda para o próximo da lista
 *   4. confirm()  -> um contato respondeu: o incidente fica CONFIRMED_BY_CONTACT
 *
 * Decisão de projeto: o AlertService NÃO tem relógio próprio.
 * Assim os testes chamam escalate() na hora, sem esperar 2 minutos de verdade.
 *
 * Recebe o canal pelo construtor ("injeção de dependência"): DemoAlert na apresentação,
 * SmsAlert de verdade. A classe não sabe qual é (polimorfismo).
 */
public class AlertService {

    /** Tempo do "Are you OK?" antes de enviar o alerta. */
    public static final long COUNTDOWN_MILLIS = 30_000;

    /** Tempo de espera por resposta antes de passar para o próximo contato (versão real). */
    public static final long ESCALATION_TIMEOUT_MILLIS = 120_000;

    private final AlertChannel channel;
    private User user;
    private Incident incident;
    private List<EmergencyContact> queue;   // contatos em ordem de prioridade
    private int currentIndex = -1;          // -1 = ainda não começou

    public AlertService(AlertChannel channel) {
        if (channel == null) throw new IllegalArgumentException("Channel is required");
        this.channel = channel;
    }

    /** Começa o alerta: marca o incidente como enviado e avisa o primeiro contato. */
    public void start(User user, Incident incident) {
        if (!user.hasContacts()) {
            throw new IllegalStateException("Register at least one emergency contact");
        }
        this.user = user;
        this.incident = incident;
        this.queue = user.getContactsByPriority();
        this.currentIndex = 0;
        incident.markAlertSent();
        channel.send(queue.get(0), buildMessage(user, incident));
    }

    /**
     * O contato atual não respondeu a tempo: passa para o próximo.
     *
     * @return true se mandou para alguém; false se a lista acabou (incidente fica NO_RESPONSE).
     */
    public boolean escalate() {
        if (currentIndex + 1 < queue.size()) {
            currentIndex++;   // "++" soma 1 na variável
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

    /** Quem foi avisado por último (null se ainda não começou). */
    public EmergencyContact getCurrentContact() {
        return currentIndex >= 0 ? queue.get(currentIndex) : null;
    }

    /** Quem será avisado se o atual não responder (null se for o último). */
    public EmergencyContact getNextContact() {
        boolean hasNext = currentIndex >= 0 && currentIndex + 1 < queue.size();
        return hasNext ? queue.get(currentIndex + 1) : null;
    }

    public String getChannelName() {
        return channel.getName();
    }

    /**
     * Texto do SMS: curto, em inglês, com o link do mapa.
     * "static" porque só usa os parâmetros: dá para chamar sem criar um AlertService.
     */
    public static String buildMessage(User user, Incident incident) {
        return "GUARD ALERT: " + user.getName() + " may have been in an accident ("
                + user.getSelectedVehicle().getDisplayName().toLowerCase() + "). "
                + "Location: " + incident.getMapLink();
    }
}
