package com.guard.detection;

import com.guard.model.SensorReading;
import com.guard.model.Vehicle;

/**
 * Sinal: ROTAÇÃO / CAPOTAMENTO (giroscópio, pode vir mocado).
 * Dispara quando o celular gira mais rápido que o limite do veículo.
 */
public class RolloverDetector implements IncidentDetector {

    public static final String SIGNAL = "rotation";

    @Override
    public String getSignal() {
        return SIGNAL;
    }

    @Override
    public boolean analyze(SensorReading reading, Vehicle vehicle) {
        return reading.getRotationDegPerSec() >= vehicle.getRotationThresholdDegPerSec();
    }

    @Override
    public void reset() {
        // Não guarda histórico.
    }
}
