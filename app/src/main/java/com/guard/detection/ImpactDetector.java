package com.guard.detection;

import com.guard.model.SensorReading;
import com.guard.model.vehicle.Vehicle;

/**
 * Sinal: IMPACTO (acelerômetro).
 * Dispara quando a aceleração passa do limite do veículo (ex.: 3.0 g na moto).
 *
 * "implements IncidentDetector" = esta classe assina o contrato da interface
 * e por isso é OBRIGADA a ter getSignal(), analyze() e reset().
 */
public class ImpactDetector implements IncidentDetector {

    /** Nome do sinal. "public static final" para outras classes poderem usar a mesma constante. */
    public static final String SIGNAL = "impact";

    @Override
    public String getSignal() {
        return SIGNAL;
    }

    @Override
    public boolean analyze(SensorReading reading, Vehicle vehicle) {
        // Uma comparação já é um boolean (true/false): dá para retornar direto, sem if.
        return reading.getAccelerationG() >= vehicle.getImpactThresholdG();
    }

    @Override
    public void reset() {
        // Nada a fazer: cada leitura é avaliada sozinha, não guardamos histórico.
    }
}
