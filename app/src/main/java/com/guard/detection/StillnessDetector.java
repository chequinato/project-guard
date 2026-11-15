package com.guard.detection;

import com.guard.model.SensorReading;
import com.guard.model.vehicle.Vehicle;

/**
 * Sinal: IMOBILIDADE ("nada se mexe por 10 segundos").
 *
 * Uma leitura é "parada" quando velocidade, aceleração e rotação estão todas perto de zero.
 *
 * Também precisa de memória, mas bem menos que o DecelerationDetector:
 * não importa COMO foram as leituras paradas, só DESDE QUANDO está parado.
 * Então basta guardar um número (stillSince), em vez de uma fila inteira.
 */
public class StillnessDetector implements IncidentDetector {

    public static final String SIGNAL = "stillness";

    // Sem "public/private" = visíveis só dentro do pacote "detection" (e para os testes dele).
    static final double STILL_SPEED_KMH = 1.0;
    static final double STILL_ACC_G = 0.1;
    static final double STILL_ROT_DEG = 5.0;

    /** Valor especial que significa "não está parado". */
    private static final long NOT_STILL = -1;

    /** Instante em que começou a ficar parado (ou NOT_STILL). */
    private long stillSince = NOT_STILL;

    @Override
    public String getSignal() {
        return SIGNAL;
    }

    @Override
    public boolean analyze(SensorReading reading, Vehicle vehicle) {
        // "&&" = E: só está parado se as TRÊS condições forem verdadeiras.
        boolean still = reading.getSpeedKmh() < STILL_SPEED_KMH
                && reading.getAccelerationG() < STILL_ACC_G
                && reading.getRotationDegPerSec() < STILL_ROT_DEG;

        if (!still) {
            stillSince = NOT_STILL;   // mexeu: zera a contagem
            return false;
        }
        if (stillSince == NOT_STILL) {
            stillSince = reading.getTimestampMillis();   // acabou de parar: marca o início
        }
        // Já está parado há 10 s ou mais?
        return reading.getTimestampMillis() - stillSince >= Vehicle.STILLNESS_MILLIS;
    }

    @Override
    public void reset() {
        stillSince = NOT_STILL;
    }
}
