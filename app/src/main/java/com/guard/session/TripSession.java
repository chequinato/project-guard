package com.guard.session;

import com.guard.detection.IncidentListener;
import com.guard.detection.IncidentManager;
import com.guard.model.Incident;
import com.guard.model.SensorReading;
import com.guard.model.Trip;
import com.guard.model.User;
import com.guard.model.vehicle.Vehicle;
import com.guard.sensor.SensorListener;
import com.guard.sensor.SensorMonitor;

import java.util.Map;

/**
 * Uma viagem EM ANDAMENTO: junta todas as peças que precisam trabalhar juntas.
 *
 *     SensorMonitor ──(Observer)──► IncidentManager ──► detectores
 *          │                              │
 *          ├──► Trip (guarda leituras)    └──► possível acidente ──► Incident
 *          └──► TripSession ──► tela (Listener)
 *
 * Por que esta classe existe?
 * - Antes, a tela de viagem (Activity) fazia tudo isso sozinha e ficava enorme.
 *   Agora a tela só desenha; quem coordena é esta classe, que é Java puro e tem testes.
 * - Ela guarda o incidente ATIVO. A tela "Are you OK?" pega exatamente ESTE objeto,
 *   então quando o status muda (cancelado, confirmado...) a viagem também enxerga a mudança.
 *
 * Ela implementa DUAS interfaces: é ouvinte do sensor E ouvinte de incidentes.
 */
public class TripSession implements SensorListener, IncidentListener {

    /**
     * INTERFACE ANINHADA: declarada dentro da classe porque só faz sentido junto com ela.
     * De fora, o nome completo é TripSession.Listener. A tela de viagem implementa isto.
     */
    public interface Listener {
        /** Chegou uma leitura nova (para atualizar a velocidade e as barras). */
        void onReading(SensorReading reading);

        /** Possível acidente: hora de mostrar o "Are you OK?". */
        void onIncident(Incident incident);
    }

    private final User user;
    private final Trip trip;
    private final SensorMonitor source;
    private final IncidentManager manager;
    private Incident activeIncident;   // null = nenhum acidente em andamento
    private Listener listener;

    /**
     * @param user        quem está viajando (com veículo e contatos)
     * @param source      de onde vêm as leituras: MockSensorSource ou AndroidSensorSource
     * @param startMillis horário de início
     */
    public TripSession(User user, SensorMonitor source, long startMillis) {
        this.user = user;
        this.source = source;
        this.trip = new Trip(user.getSelectedVehicle().getType(), startMillis);
        this.manager = IncidentManager.withDefaultDetectors(user.getSelectedVehicle());
        this.manager.setListener(this);   // "me avise quando detectar um acidente"

        // Inscrevemos os ouvintes do sensor. A ORDEM importa:
        // primeiro o detector analisa, depois a viagem guarda, por fim a tela desenha
        // (assim a tela já mostra os sinais atualizados daquela leitura).
        source.addListener(manager);
        source.addListener(trip::addReading);   // method reference: "chame trip.addReading(leitura)"
        source.addListener(this);
    }

    public void setListener(Listener listener) { this.listener = listener; }

    /** Começa a receber leituras. */
    public void start() {
        source.start();
    }

    /** Recebe cada leitura (contrato SensorListener) e repassa para a tela. */
    @Override
    public void onReading(SensorReading reading) {
        if (listener != null) listener.onReading(reading);
    }

    /** O IncidentManager detectou um possível acidente (contrato IncidentListener). */
    @Override
    public void onIncidentDetected(Incident incident) {
        activeIncident = incident;
        trip.addIncident(incident);   // a viagem guarda o MESMO objeto
        if (listener != null) listener.onIncident(incident);
    }

    /** Depois que o alerta terminou: limpa o incidente e volta a detectar do zero. */
    public void resumeAfterIncident() {
        activeIncident = null;
        manager.resume();
    }

    /** Termina a viagem e devolve a Trip pronta para salvar no banco. */
    public Trip end(long endMillis) {
        source.stop();
        trip.end(endMillis);
        return trip;
    }

    public User getUser() { return user; }
    public Trip getTrip() { return trip; }
    public Vehicle getVehicle() { return manager.getVehicle(); }
    public Incident getActiveIncident() { return activeIncident; }

    /** Quais sinais estão ligados agora (para a tela acender ● ou ○). */
    public Map<String, Boolean> activeSignals(long now) {
        return manager.activeSignals(now);
    }
}
