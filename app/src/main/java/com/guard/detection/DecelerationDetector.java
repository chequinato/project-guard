package com.guard.detection;

import com.guard.model.SensorReading;
import com.guard.model.vehicle.Vehicle;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Sinal: QUEDA BRUSCA DE VELOCIDADE (GPS).
 * Regra do site: "perdeu X km/h em até 2 segundos" (X depende do veículo; 20 km/h na moto).
 *
 * Uma leitura sozinha não diz se a velocidade caiu: precisamos LEMBRAR das anteriores.
 * Por isso este detector guarda uma JANELA com as leituras dos últimos 2 segundos
 * e compara a velocidade atual com a MAIOR velocidade dessa janela.
 *
 * Exemplo (moto, limite 20 km/h):
 *   t=0,0s  50 km/h  ┐
 *   t=0,5s  45 km/h  │ janela de 2 s -> maior = 50
 *   t=1,5s  25 km/h  ┘ 50 - 25 = 25 >= 20 -> DISPARA
 */
public class DecelerationDetector implements IncidentDetector {

    public static final String SIGNAL = "speedDrop";

    // DEQUE ("deck") = fila de duas pontas: dá para pôr e tirar tanto no começo quanto no fim.
    // Novas leituras entram no FIM; as velhas saem pelo COMEÇO.
    // É "private": ninguém de fora mexe na memória do detector (encapsulamento).
    private final Deque<SensorReading> window = new ArrayDeque<>();

    @Override
    public String getSignal() {
        return SIGNAL;
    }

    @Override
    public boolean analyze(SensorReading reading, Vehicle vehicle) {
        // 1) A leitura nova entra no fim da fila.
        window.addLast(reading);

        // 2) Tira do começo tudo o que ficou mais velho que 2 segundos.
        //    peekFirst() OLHA o primeiro sem tirar; pollFirst() TIRA o primeiro.
        long now = reading.getTimestampMillis();
        while (now - window.peekFirst().getTimestampMillis() > Vehicle.SPEED_DROP_WINDOW_MILLIS) {
            window.pollFirst();
        }

        // 3) Acha a maior velocidade que ainda está na janela.
        double maxSpeed = 0;
        for (SensorReading r : window) {
            maxSpeed = Math.max(maxSpeed, r.getSpeedKmh());
        }

        // 4) A queda é "maior velocidade recente" menos "velocidade agora".
        return maxSpeed - reading.getSpeedKmh() >= vehicle.getSpeedDropThresholdKmh();
    }

    @Override
    public void reset() {
        window.clear();   // esvazia a fila
    }
}
