package com.guard.model;

import com.guard.model.vehicle.VehicleType;

import java.util.Locale;

/**
 * Um possível acidente detectado.
 *
 * CONCEITO: máquina de estados
 * O status só pode andar por caminhos permitidos. Cada método confere se o passo é válido:
 *
 *   COUNTDOWN  -> CANCELLED_BY_USER     (apertou "I'm OK" a tempo)
 *   COUNTDOWN  -> ALERT_SENT -> CONFIRMED_BY_CONTACT
 *                            -> NO_RESPONSE        (acabou a lista de contatos)
 *                            -> CANCELLED_BY_USER  (o alerta saiu, mas a pessoa estava bem)
 *
 * Como o status é private e só muda por esses métodos, é impossível, por exemplo,
 * "confirmar" um incidente que nem teve alerta enviado.
 */
public class Incident {

    private long id;
    private final long timestampMillis;
    // NÃO são final: começam com a posição da leitura do sensor e são trocadas pelo GPS
    // de verdade do celular assim que ele responde (updateLocation).
    private double latitude;
    private double longitude;
    private boolean fromGps;   // true = a posição atual veio do GPS do celular
    private final VehicleType vehicleType;

    // Valor inicial direto na declaração: todo incidente nasce em COUNTDOWN.
    private IncidentStatus status = IncidentStatus.COUNTDOWN;

    public Incident(long timestampMillis, double latitude, double longitude, VehicleType vehicleType) {
        this.timestampMillis = timestampMillis;
        this.latitude = latitude;
        this.longitude = longitude;
        this.vehicleType = vehicleType;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getTimestampMillis() { return timestampMillis; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public VehicleType getVehicleType() { return vehicleType; }
    public boolean isFromGps() { return fromGps; }

    /**
     * Posição real do celular (GPS). A tela de alerta chama isto a cada nova leitura,
     * então cada SMS (inclusive o do próximo contato) sai com a posição mais recente.
     */
    public void updateLocation(double latitude, double longitude) {
        // Latitude vai de -90 a 90 e longitude de -180 a 180: fora disso é dado quebrado.
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Invalid location: " + latitude + ", " + longitude);
        }
        this.latitude = latitude;
        this.longitude = longitude;
        this.fromGps = true;
    }
    public IncidentStatus getStatus() { return status; }

    /** Link do Google Maps que vai na mensagem de emergência. */
    public String getMapLink() {
        // %.6f = número com 6 casas decimais (precisão de ~10 cm no mapa).
        return String.format(Locale.US, "https://maps.google.com/?q=%.6f,%.6f", latitude, longitude);
    }

    /** A pessoa disse que está bem. Vale durante a contagem ou depois do alerta enviado. */
    public void cancelByUser() {
        // "&&" = E lógico. Só dá erro se NÃO estiver em nenhum dos dois estados permitidos.
        if (status != IncidentStatus.COUNTDOWN && status != IncidentStatus.ALERT_SENT) {
            throw new IllegalStateException("Cannot cancel an incident in " + status);
        }
        status = IncidentStatus.CANCELLED_BY_USER;
    }

    /** A contagem zerou: o alerta foi enviado. */
    public void markAlertSent() {
        requireStatus(IncidentStatus.COUNTDOWN);
        status = IncidentStatus.ALERT_SENT;
    }

    /** Um contato respondeu "estou indo". */
    public void markConfirmedByContact() {
        requireStatus(IncidentStatus.ALERT_SENT);
        status = IncidentStatus.CONFIRMED_BY_CONTACT;
    }

    /** Ninguém da lista respondeu. */
    public void markNoResponse() {
        requireStatus(IncidentStatus.ALERT_SENT);
        status = IncidentStatus.NO_RESPONSE;
    }

    /**
     * Método auxiliar "private": só esta classe usa.
     * Evita repetir o mesmo if em todos os métodos acima.
     */
    private void requireStatus(IncidentStatus expected) {
        if (status != expected) {
            throw new IllegalStateException("Incident is " + status + ", expected " + expected);
        }
    }
}
