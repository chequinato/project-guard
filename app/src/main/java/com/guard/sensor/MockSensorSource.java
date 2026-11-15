package com.guard.sensor;

import com.guard.model.SensorReading;

import java.util.ArrayList;
import java.util.List;

/**
 * Fonte de leituras SIMULADAS (o "mock"). Usada nos testes e na apresentação,
 * porque o emulador não tem sensores de verdade.
 *
 * Convenções dos valores:
 *   - accelerationG é a aceleração LINEAR, sem a gravidade (parado = ~0 g).
 *   - Uma leitura a cada 500 ms.
 *   - Posição fixa perto da UniAnchieta, em Jundiaí.
 *
 * Esta classe não tem nada de Android: ela só GERA leituras.
 * Quem decide QUANDO entregar cada uma é quem chama:
 *   - nos testes, play() entrega tudo de uma vez;
 *   - na tela, a Activity entrega uma por vez com push(), no ritmo de um relógio.
 */
public class MockSensorSource extends SensorMonitor {

    /** Intervalo entre duas leituras simuladas. */
    public static final long STEP_MILLIS = 500;

    // Coordenadas fixas (latitude/longitude). "private static final" = constante só desta classe.
    private static final double LAT = -23.1857;
    private static final double LNG = -46.8978;

    private boolean running;

    // ---------- Métodos obrigatórios da mãe (SensorMonitor) ----------

    @Override
    public void start() { running = true; }

    @Override
    public void stop() { running = false; }

    public boolean isRunning() { return running; }

    // ---------- Formas de entregar leituras ----------

    /** Entrega TODAS as leituras do cenário de uma vez. Ótimo para testes. */
    public void play(Scenario scenario, long startMillis) {
        start();
        for (SensorReading r : generate(scenario, startMillis)) {
            if (!running) break;   // "break" sai do loop na hora
            publish(r);
        }
    }

    /** Entrega UMA leitura. A tela usa isso para tocar o cenário aos poucos. */
    public void push(SensorReading reading) {
        if (running) publish(reading);
    }

    /**
     * Gera a lista de leituras de um cenário, começando no instante t0.
     *
     * "static" porque não depende de nenhum objeto: é só uma "receita" de números.
     * Por isso dá para chamar direto: MockSensorSource.generate(Scenario.CRASH, 0).
     */
    public static List<SensorReading> generate(Scenario scenario, long t0) {
        List<SensorReading> out = new ArrayList<>();
        long t = t0;   // relógio da simulação

        // Todo cenário começa com 5 s de pilotagem normal a 50 km/h (10 leituras x 500 ms).
        // Este "for" tem DUAS atualizações separadas por vírgula: i++ e t += STEP_MILLIS.
        for (int i = 0; i < 10; i++, t += STEP_MILLIS) {
            out.add(reading(t, 50, 0.2, 8));
        }

        // "switch" escolhe um bloco conforme o valor do enum.
        // Cada "case" termina em "break"; sem ele, o código continuaria no próximo case.
        switch (scenario) {
            case NORMAL_RIDE:
                // i % 4 = resto da divisão por 4 (0,1,2,3,0,1...): faz a velocidade oscilar um pouco.
                for (int i = 0; i < 30; i++, t += STEP_MILLIS) out.add(reading(t, 50 + (i % 4), 0.3, 10));
                break;

            case CRASH:
                // Batida: impacto forte + velocidade despenca em 1 s + capotamento...
                out.add(reading(t, 30, 6.5, 320)); t += STEP_MILLIS;
                out.add(reading(t, 8, 3.2, 260));  t += STEP_MILLIS;
                out.add(reading(t, 0, 1.0, 90));   t += STEP_MILLIS;
                // ...e depois ninguém se mexe por 12 s.
                for (int i = 0; i < 24; i++, t += STEP_MILLIS) out.add(reading(t, 0, 0.02, 1));
                break;

            case PHONE_DROPPED:
                // O celular caiu: pancada e giro fortes, mas a velocidade CONTINUA (não foi a moto).
                out.add(reading(t, 50, 4.5, 450)); t += STEP_MILLIS;
                for (int i = 0; i < 24; i++, t += STEP_MILLIS) out.add(reading(t, 49, 0.3, 5));
                break;

            case HARD_BRAKE:
                // Freada brusca: a velocidade cai rápido, mas SEM impacto e sem giro.
                out.add(reading(t, 35, 0.9, 6)); t += STEP_MILLIS;
                out.add(reading(t, 18, 0.9, 5)); t += STEP_MILLIS;
                out.add(reading(t, 4, 0.7, 4));  t += STEP_MILLIS;
                for (int i = 0; i < 24; i++, t += STEP_MILLIS) out.add(reading(t, 0, 0.02, 1));
                break;

            case SPEED_BUMP:
                // Lombada: um solavanco médio e tudo volta ao normal.
                out.add(reading(t, 30, 1.6, 40)); t += STEP_MILLIS;
                for (int i = 0; i < 24; i++, t += STEP_MILLIS) out.add(reading(t, 32, 0.3, 8));
                break;

            case PARKED:
                // Parado, mexendo no celular: gira, mas sem velocidade e sem impacto.
                for (int i = 0; i < 4; i++, t += STEP_MILLIS) out.add(reading(t, 0, 0.4, 220));
                for (int i = 0; i < 24; i++, t += STEP_MILLIS) out.add(reading(t, 0, 0.02, 1));
                break;
        }
        return out;
    }

    /** Atalho para criar uma leitura com a posição fixa. Deixa o código acima mais curto. */
    private static SensorReading reading(long t, double speed, double acc, double rot) {
        return new SensorReading(t, speed, acc, rot, LAT, LNG);
    }
}
