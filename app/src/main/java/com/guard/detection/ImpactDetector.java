package com.guard.detection;

import com.guard.model.SensorReading;
import com.guard.model.Vehicle;

/**
 * Sinal: IMPACTO (acelerômetro).
 * Dispara quando a aceleração linear passa do limite do veículo.
 *
 * Este é o EXEMPLO RESOLVIDO. Usem como modelo para os outros três detectores.
 */
public class ImpactDetector implements IncidentDetector {

    public static final String SIGNAL = "impact";

    @Override
    public String getSignal() {
        return SIGNAL;
    }

    @Override
    public boolean analyze(SensorReading reading, Vehicle vehicle) {
        return reading.getAccelerationG() >= vehicle.getImpactThresholdG();
    }

    @Override
    public void reset() {
        // Não guarda histórico: cada leitura é avaliada sozinha.
    }
}
