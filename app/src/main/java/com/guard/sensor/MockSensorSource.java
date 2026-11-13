package com.guard.sensor;

import com.guard.model.SensorReading;

import java.util.ArrayList;
import java.util.List;

/**
 * Fonte de leituras SIMULADAS. Usada nos testes e na apresentação (emulador não tem sensor de verdade).
 *
 * Convenção dos valores:
 *   - accelerationG é a aceleração LINEAR, sem a gravidade (parado = ~0 g).
 *   - Uma leitura a cada 500 ms.
 *   - Posição fixa perto da UniAnchieta, Jundiaí.
 *
 * Esta classe não tem nada de Android: quem decide "quando" tocar as leituras é quem chama.
 * Nos testes, play() publica tudo na hora. Na tela, a Activity toca uma por vez com um Handler.
 */
public class MockSensorSource extends SensorMonitor {

    public static final long STEP_MILLIS = 500;
    private static final double LAT = -23.1857;
    private static final double LNG = -46.8978;

    private boolean running;

    @Override
    public void start() { running = true; }

    @Override
    public void stop() { running = false; }

    public boolean isRunning() { return running; }

    /** Publica todas as leituras do cenário de uma vez (ótimo para testes). */
    public void play(Scenario scenario, long startMillis) {
        start();
        for (SensorReading r : generate(scenario, startMillis)) {
            if (!running) break;
            publish(r);
        }
    }

    /** Publica UMA leitura. A tela usa isso para tocar o cenário no ritmo real. */
    public void push(SensorReading reading) {
        if (running) publish(reading);
    }

    /** Gera a sequência de leituras de um cenário. */
    public static List<SensorReading> generate(Scenario scenario, long t0) {
        List<SensorReading> out = new ArrayList<>();
        long t = t0;

        // Todo cenário começa com 5 s de pilotagem normal a 50 km/h.
        for (int i = 0; i < 10; i++, t += STEP_MILLIS) {
            out.add(r(t, 50, 0.2, 8));
        }

        switch (scenario) {
            case NORMAL_RIDE:
                for (int i = 0; i < 30; i++, t += STEP_MILLIS) out.add(r(t, 50 + (i % 4), 0.3, 10));
                break;

            case CRASH:
                // Batida: impacto forte + velocidade despenca em 1 s + capotamento...
                out.add(r(t, 30, 6.5, 320)); t += STEP_MILLIS;
                out.add(r(t, 8, 3.2, 260));  t += STEP_MILLIS;
                out.add(r(t, 0, 1.0, 90));   t += STEP_MILLIS;
                // ...e depois ninguém se mexe por 12 s.
                for (int i = 0; i < 24; i++, t += STEP_MILLIS) out.add(r(t, 0, 0.02, 1));
                break;

            case PHONE_DROPPED:
                // Celular caiu no chão da moto: pancada e giro, mas a velocidade continua.
                out.add(r(t, 50, 4.5, 450)); t += STEP_MILLIS;
                for (int i = 0; i < 24; i++, t += STEP_MILLIS) out.add(r(t, 49, 0.3, 5));
                break;

            case HARD_BRAKE:
                // Freada brusca: velocidade cai rápido, mas sem impacto nem giro.
                out.add(r(t, 35, 0.9, 6)); t += STEP_MILLIS;
                out.add(r(t, 18, 0.9, 5)); t += STEP_MILLIS;
                out.add(r(t, 4, 0.7, 4));  t += STEP_MILLIS;
                for (int i = 0; i < 24; i++, t += STEP_MILLIS) out.add(r(t, 0, 0.02, 1));
                break;

            case SPEED_BUMP:
                // Lombada: um solavanco médio, tudo volta ao normal.
                out.add(r(t, 30, 1.6, 40)); t += STEP_MILLIS;
                for (int i = 0; i < 24; i++, t += STEP_MILLIS) out.add(r(t, 32, 0.3, 8));
                break;

            case PARKED:
                // Parado, mexendo no celular: gira, mas sem velocidade e sem impacto.
                for (int i = 0; i < 4; i++, t += STEP_MILLIS) out.add(r(t, 0, 0.4, 220));
                for (int i = 0; i < 24; i++, t += STEP_MILLIS) out.add(r(t, 0, 0.02, 1));
                break;
        }
        return out;
    }

    private static SensorReading r(long t, double speed, double acc, double rot) {
        return new SensorReading(t, speed, acc, rot, LAT, LNG);
    }
}
