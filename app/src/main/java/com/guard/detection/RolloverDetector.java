package com.guard.detection;

import com.guard.model.SensorReading;
import com.guard.model.vehicle.Vehicle;

/**
 * Sinal: ROTAÇÃO / CAPOTAMENTO (giroscópio; no app hoje, valor simulado).
 * Dispara quando o celular gira mais rápido que o limite do veículo (ex.: 150 °/s na moto).
 *
 * Repare que é igualzinho ao ImpactDetector, só muda o campo comparado.
 * Cada detector é pequeno e faz UMA coisa só: isso facilita testar e entender.
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
