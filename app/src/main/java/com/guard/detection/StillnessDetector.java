package com.guard.detection;

import com.guard.model.SensorReading;
import com.guard.model.Vehicle;

/**
 * Sinal: IMOBILIDADE ("nada se mexe por 10 segundos").
 *
 * Uma leitura é "parada" quando velocidade, aceleração e rotação estão todas perto de zero.
 * Não precisa guardar as leituras: basta lembrar DESDE QUANDO está parado.
 */
public class StillnessDetector implements IncidentDetector {

    public static final String SIGNAL = "stillness";

    static final double STILL_SPEED_KMH = 1.0;
    static final double STILL_ACC_G = 0.1;
    static final double STILL_ROT_DEG = 5.0;

    private static final long NOT_STILL = -1;
    private long stillSince = NOT_STILL;

    @Override
    public String getSignal() {
        return SIGNAL;
    }

    @Override
    public boolean analyze(SensorReading reading, Vehicle vehicle) {
        boolean still = reading.getSpeedKmh() < STILL_SPEED_KMH
                && reading.getAccelerationG() < STILL_ACC_G
                && reading.getRotationDegPerSec() < STILL_ROT_DEG;

        if (!still) {
            stillSince = NOT_STILL;
            return false;
        }
        if (stillSince == NOT_STILL) {
            stillSince = reading.getTimestampMillis();
        }
        return reading.getTimestampMillis() - stillSince >= Vehicle.STILLNESS_MILLIS;
    }

    @Override
    public void reset() {
        stillSince = NOT_STILL;
    }
}
