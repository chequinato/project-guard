package com.guard.model;

import java.util.Locale;

/**
 * Um possível acidente detectado.
 *
 * Ciclo de vida (o status só anda pra frente):
 *   COUNTDOWN  -> CANCELLED_BY_USER     (apertou "I'm OK" a tempo)
 *   COUNTDOWN  -> ALERT_SENT -> CONFIRMED_BY_CONTACT
 *                            -> NO_RESPONSE        (acabou a lista de contatos)
 *                            -> CANCELLED_BY_USER  (o alerta saiu, mas a pessoa estava bem)
 */
public class Incident {

    private long id;
    private final long timestampMillis;
    private final double latitude;
    private final double longitude;
    private final VehicleType vehicleType;
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
    public IncidentStatus getStatus() { return status; }

    /** Link do mapa que vai na mensagem de emergência. */
    public String getMapLink() {
        return String.format(Locale.US, "https://maps.google.com/?q=%.6f,%.6f", latitude, longitude);
    }

    public void cancelByUser() {
        if (status != IncidentStatus.COUNTDOWN && status != IncidentStatus.ALERT_SENT) {
            throw new IllegalStateException("Não dá para cancelar um incidente em " + status);
        }
        status = IncidentStatus.CANCELLED_BY_USER;
    }

    public void markAlertSent() {
        requireStatus(IncidentStatus.COUNTDOWN);
        status = IncidentStatus.ALERT_SENT;
    }

    public void markConfirmedByContact() {
        requireStatus(IncidentStatus.ALERT_SENT);
        status = IncidentStatus.CONFIRMED_BY_CONTACT;
    }

    public void markNoResponse() {
        requireStatus(IncidentStatus.ALERT_SENT);
        status = IncidentStatus.NO_RESPONSE;
    }

    /** Usado só pelo banco, ao recarregar o histórico. */
    public void restoreStatus(IncidentStatus status) {
        this.status = status;
    }

    private void requireStatus(IncidentStatus expected) {
        if (status != expected) {
            throw new IllegalStateException("Incidente está em " + status + ", esperava " + expected);
        }
    }
}
