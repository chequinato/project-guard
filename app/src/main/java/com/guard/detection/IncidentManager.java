package com.guard.detection;

import com.guard.model.Incident;
import com.guard.model.SensorReading;
import com.guard.model.Vehicle;
import com.guard.sensor.SensorListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * O "cérebro" da detecção combinada.
 *
 * - É um SensorListener (Observer): recebe cada leitura do SensorMonitor.
 * - Passa a leitura por TODOS os detectores (polimorfismo: é só uma lista de IncidentDetector).
 * - Lembra quando cada sinal disparou pela última vez.
 * - Decide se os sinais concordam e, se sim, cria um Incident e avisa o IncidentListener.
 *
 * Por que "pela última vez"? Os sinais não acontecem no mesmo instante:
 * o impacto e a queda de velocidade vêm juntos, mas a imobilidade só aparece 10 s depois.
 * Por isso um sinal conta como "ativo" se disparou nos últimos SIGNAL_MEMORY_MILLIS.
 */
public class IncidentManager implements SensorListener {

    public static final long SIGNAL_MEMORY_MILLIS = 15_000;

    private final List<IncidentDetector> detectors;
    private final Map<String, Long> lastFiredAt = new HashMap<>();
    private Vehicle vehicle;
    private IncidentListener listener;
    private boolean paused;

    public IncidentManager(Vehicle vehicle, List<IncidentDetector> detectors) {
        if (vehicle == null || detectors == null || detectors.isEmpty()) {
            throw new IllegalArgumentException("Precisa de um veículo e de pelo menos um detector");
        }
        this.vehicle = vehicle;
        this.detectors = new ArrayList<>(detectors);
    }

    /** Conjunto padrão: os quatro sinais do Guard. */
    public static IncidentManager withDefaultDetectors(Vehicle vehicle) {
        List<IncidentDetector> list = new ArrayList<>();
        list.add(new ImpactDetector());
        list.add(new DecelerationDetector());
        list.add(new RolloverDetector());
        list.add(new StillnessDetector());
        return new IncidentManager(vehicle, list);
    }

    public void setListener(IncidentListener listener) { this.listener = listener; }

    /** Strategy: trocar o perfil troca os limites usados por todos os detectores. */
    public void setVehicle(Vehicle vehicle) { this.vehicle = vehicle; }
    public Vehicle getVehicle() { return vehicle; }

    /** Enquanto a contagem regressiva está na tela, não detecta de novo. */
    public void pause() { paused = true; }
    public void resume() { reset(); paused = false; }

    public void reset() {
        lastFiredAt.clear();
        for (IncidentDetector d : detectors) d.reset();
    }

    @Override
    public void onReading(SensorReading reading) {
        if (paused) return;

        long now = reading.getTimestampMillis();
        for (IncidentDetector detector : detectors) {
            if (detector.analyze(reading, vehicle)) {
                lastFiredAt.put(detector.getSignal(), now);
            }
        }

        if (signalsAgree(now)) {
            Incident incident = new Incident(now, reading.getLatitude(), reading.getLongitude(), vehicle.getType());
            pause();
            if (listener != null) listener.onIncidentDetected(incident);
        }
    }

    /** O sinal disparou nos últimos SIGNAL_MEMORY_MILLIS? */
    public boolean isActive(String signal, long now) {
        Long firedAt = lastFiredAt.get(signal);
        return firedAt != null && now - firedAt <= SIGNAL_MEMORY_MILLIS;
    }

    /** Usado pela tela para acender os indicadores de cada sinal. */
    public Map<String, Boolean> activeSignals(long now) {
        Map<String, Boolean> out = new HashMap<>();
        for (IncidentDetector d : detectors) out.put(d.getSignal(), isActive(d.getSignal(), now));
        return Collections.unmodifiableMap(out);
    }

    /**
     * A regra do Guard (site, seção "Why it doesn't cry wolf"):
     *
     *     Impact  AND  Speed drop  AND  (Rotation OR Stillness)
     *
     * Celular caiu no chão: tem impacto e giro, mas a velocidade não cai  -> não dispara.
     * Freada brusca: a velocidade cai, mas não tem impacto               -> não dispara.
     */
    boolean signalsAgree(long now) {
        return isActive(ImpactDetector.SIGNAL, now)
                && isActive(DecelerationDetector.SIGNAL, now)
                && (isActive(RolloverDetector.SIGNAL, now) || isActive(StillnessDetector.SIGNAL, now));
    }
}
