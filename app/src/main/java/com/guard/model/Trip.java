package com.guard.model;

import com.guard.model.vehicle.VehicleType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Uma viagem: começa quando o usuário toca em "Start trip" e termina em "End trip".
 *
 * CONCEITO: composição
 * Uma Trip TEM uma lista de leituras (SensorReading) e uma lista de incidentes (Incident).
 * As listas são privadas; de fora só dá para ler (getReadings/getIncidents devolvem cópias "vitrine").
 */
public class Trip {

    private long id;
    private final VehicleType vehicleType;
    private final long startMillis;
    private long endMillis;          // 0 = viagem ainda em andamento
    private double maxSpeedKmh;      // maior velocidade vista até agora
    private final List<SensorReading> readings = new ArrayList<>();
    private final List<Incident> incidents = new ArrayList<>();

    public Trip(VehicleType vehicleType, long startMillis) {
        this.vehicleType = vehicleType;
        this.startMillis = startMillis;
    }

    /** Guarda uma leitura nova e atualiza a velocidade máxima. */
    public void addReading(SensorReading reading) {
        if (!isActive()) {
            throw new IllegalStateException("Trip already ended");
        }
        readings.add(reading);
        // Math.max devolve o maior dos dois números.
        maxSpeedKmh = Math.max(maxSpeedKmh, reading.getSpeedKmh());
    }

    public void addIncident(Incident incident) {
        incidents.add(incident);
    }

    /** Encerra a viagem. Depois disso ela não aceita mais leituras. */
    public void end(long endMillis) {
        if (!isActive()) {
            throw new IllegalStateException("Trip already ended");
        }
        if (endMillis < startMillis) {
            throw new IllegalArgumentException("End is before start");
        }
        this.endMillis = endMillis;
    }

    /** Ativa = ainda não tem horário de fim. */
    public boolean isActive() {
        return endMillis == 0;
    }

    /** Duração em milissegundos (0 enquanto a viagem não terminou). */
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
}
