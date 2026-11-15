package com.guard.detection;

import com.guard.model.Incident;
import com.guard.model.SensorReading;
import com.guard.model.vehicle.Vehicle;
import com.guard.sensor.SensorListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * O "cérebro" da detecção combinada.
 *
 * O que ele faz a cada leitura:
 *   1. Passa a leitura por TODOS os detectores (polimorfismo: é só uma lista de IncidentDetector).
 *   2. Anota QUANDO cada sinal disparou pela última vez.
 *   3. Confere a regra do Guard. Se os sinais concordam, cria um Incident e avisa o ouvinte.
 *
 * Por que lembrar "quando disparou"? Os sinais não acontecem no mesmo instante:
 * o impacto e a queda de velocidade vêm juntos, mas a imobilidade só aparece 10 s depois.
 * Então um sinal conta como "ativo" se disparou nos últimos 15 segundos.
 *
 * "implements SensorListener" = ele é um OUVINTE do sensor (padrão Observer).
 */
public class IncidentManager implements SensorListener {

    /** Por quanto tempo um sinal que disparou continua valendo. */
    public static final long SIGNAL_MEMORY_MILLIS = 15_000;

    private final List<IncidentDetector> detectors;

    // MAP = dicionário chave -> valor. Aqui: nome do sinal -> instante em que disparou.
    // Usamos "Long" (com L maiúsculo, a classe) e não "long" (o tipo primitivo),
    // porque coleções em Java só guardam objetos. O Java converte sozinho ("autoboxing").
    private final Map<String, Long> lastFiredAt = new HashMap<>();

    private Vehicle vehicle;
    private IncidentListener listener;
    private boolean paused;   // boolean começa como false por padrão

    public IncidentManager(Vehicle vehicle, List<IncidentDetector> detectors) {
        if (vehicle == null || detectors == null || detectors.isEmpty()) {
            throw new IllegalArgumentException("Needs a vehicle and at least one detector");
        }
        this.vehicle = vehicle;
        // Cópia da lista recebida: se quem passou a lista mexer nela depois, não nos afeta.
        this.detectors = new ArrayList<>(detectors);
    }

    /** Atalho com os quatro detectores do Guard. "static" = chamamos pela classe. */
    public static IncidentManager withDefaultDetectors(Vehicle vehicle) {
        List<IncidentDetector> list = new ArrayList<>();
        list.add(new ImpactDetector());
        list.add(new DecelerationDetector());
        list.add(new RolloverDetector());
        list.add(new StillnessDetector());
        return new IncidentManager(vehicle, list);
    }

    public void setListener(IncidentListener listener) { this.listener = listener; }

    /** Trocar o veículo troca os limites usados por TODOS os detectores (Strategy). */
    public void setVehicle(Vehicle vehicle) { this.vehicle = vehicle; }
    public Vehicle getVehicle() { return vehicle; }

    /** Pausa a detecção (enquanto a contagem "Are you OK?" está na tela). */
    public void pause() { paused = true; }

    /** Volta a detectar, começando do zero. */
    public void resume() {
        reset();
        paused = false;
    }

    /** Apaga a memória de todos os detectores e dos sinais. */
    public void reset() {
        lastFiredAt.clear();
        for (IncidentDetector d : detectors) d.reset();
    }

    /** Recebe cada leitura do sensor (método do contrato SensorListener). */
    @Override
    public void onReading(SensorReading reading) {
        if (paused) return;   // "return" num método void só encerra o método

        long now = reading.getTimestampMillis();

        // 1 e 2) Cada detector analisa; se disparou, anotamos o horário.
        for (IncidentDetector detector : detectors) {
            if (detector.analyze(reading, vehicle)) {
                lastFiredAt.put(detector.getSignal(), now);   // put = guarda/atualiza no mapa
            }
        }

        // 3) Os sinais concordam? Então é um possível acidente.
        if (signalsAgree(now)) {
            Incident incident = new Incident(now, reading.getLatitude(), reading.getLongitude(), vehicle.getType());
            pause();   // não detecta de novo enquanto a pessoa decide se está bem
            if (listener != null) listener.onIncidentDetected(incident);
        }
    }

    /** O sinal disparou nos últimos SIGNAL_MEMORY_MILLIS? */
    public boolean isActive(String signal, long now) {
        // get devolve null se a chave não existe. Por isso a variável é "Long" (aceita null).
        Long firedAt = lastFiredAt.get(signal);
        return firedAt != null && now - firedAt <= SIGNAL_MEMORY_MILLIS;
    }

    /** Estado de cada sinal (ligado/desligado). A tela usa para acender as bolinhas ● ○. */
    public Map<String, Boolean> activeSignals(long now) {
        Map<String, Boolean> out = new HashMap<>();
        for (IncidentDetector d : detectors) {
            out.put(d.getSignal(), isActive(d.getSignal(), now));
        }
        return Collections.unmodifiableMap(out);
    }

    /**
     * A REGRA DO GUARD (site, seção "Why it doesn't cry wolf"):
     *
     *     Impact  E  Speed drop  E  (Rotation  OU  Stillness)
     *
     * - Celular caiu no chão: tem impacto e giro, mas a velocidade não cai  -> não dispara.
     * - Freada brusca: a velocidade cai, mas não tem impacto               -> não dispara.
     *
     * Sem "public": só este pacote (e os testes dele) enxerga.
     */
    boolean signalsAgree(long now) {
        return isActive(ImpactDetector.SIGNAL, now)
                && isActive(DecelerationDetector.SIGNAL, now)
                && (isActive(RolloverDetector.SIGNAL, now) || isActive(StillnessDetector.SIGNAL, now));
    }
}
