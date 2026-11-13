package com.guard.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Uma viagem: começa quando o modo viagem liga e termina quando desliga.
 *
 * POO: COMPOSIÇÃO. Uma Trip tem uma lista de SensorReading e uma lista de Incident.
 */
public class Trip {

    private long id;
    private final VehicleType vehicleType;
    private final long startMillis;
    private long endMillis;           // 0 = viagem ainda em andamento
    private double maxSpeedKmh;
    private final List<SensorReading> readings = new ArrayList<>();
    private final List<Incident> incidents = new ArrayList<>();

    public Trip(VehicleType vehicleType, long startMillis) {
        this.vehicleType = vehicleType;
        this.startMillis = startMillis;
    }

    public void addReading(SensorReading reading) {
        if (!isActive()) {
            throw new IllegalStateException("Viagem já terminou");
        }
        readings.add(reading);
        maxSpeedKmh = Math.max(maxSpeedKmh, reading.getSpeedKmh());
    }

    public void addIncident(Incident incident) {
        incidents.add(incident);
    }

    public void end(long endMillis) {
        if (!isActive()) {
            throw new IllegalStateException("Viagem já terminou");
        }
        if (endMillis < startMillis) {
            throw new IllegalArgumentException("Fim antes do início");
        }
        this.endMillis = endMillis;
    }

    public boolean isActive() { return endMillis == 0; }

    public long getDurationMillis() {
        return isActive() ? 0 : endMillis - startMillis;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public VehicleType getVehicleType() { return vehicleType; }
    public long getStartMillis() { return startMillis; }
    public long getEndMillis() { return endMillis; }
    public double getMaxSpeedKmh() { return maxSpeedKmh; }
    public List<SensorReading> getReadings() { return Collections.unmodifiableList(readings); }
    public List<Incident> getIncidents() { return Collections.unmodifiableList(incidents); }

    /** Usado só pelo banco, ao recarregar o histórico. */
    public void restore(long endMillis, double maxSpeedKmh) {
        this.endMillis = endMillis;
        this.maxSpeedKmh = maxSpeedKmh;
    }
}
