package com.guard.detection;

import com.guard.model.SensorReading;
import com.guard.model.Vehicle;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Sinal: QUEDA BRUSCA DE VELOCIDADE (GPS).
 * Regra do site: "perdeu X km/h em até 2 segundos" (X depende do veículo).
 *
 * Uma leitura sozinha não diz se a velocidade caiu, então o detector guarda uma
 * JANELA com as leituras dos últimos 2 s (fila privada = encapsulamento).
 * Compara a velocidade atual com a MAIOR velocidade da janela.
 */
public class DecelerationDetector implements IncidentDetector {

    public static final String SIGNAL = "speedDrop";

    private final Deque<SensorReading> window = new ArrayDeque<>();

    @Override
    public String getSignal() {
        return SIGNAL;
    }

    @Override
    public boolean analyze(SensorReading reading, Vehicle vehicle) {
        window.addLast(reading);

        // Remove do começo o que ficou mais velho que 2 s.
        long now = reading.getTimestampMillis();
        while (now - window.peekFirst().getTimestampMillis() > Vehicle.SPEED_DROP_WINDOW_MILLIS) {
            window.pollFirst();
        }

        double maxSpeed = 0;
        for (SensorReading r : window) {
            maxSpeed = Math.max(maxSpeed, r.getSpeedKmh());
        }
        return maxSpeed - reading.getSpeedKmh() >= vehicle.getSpeedDropThresholdKmh();
    }

    @Override
    public void reset() {
        window.clear();
    }
}
