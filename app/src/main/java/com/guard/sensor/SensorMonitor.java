package com.guard.sensor;

import com.guard.model.SensorReading;

import java.util.ArrayList;
import java.util.List;

/**
 * PADRÃO OBSERVER: o "sujeito" que avisa todos os inscritos a cada leitura nova.
 *
 * É abstrata porque a ORIGEM das leituras muda:
 *   - AndroidSensorSource  -> sensores reais do celular
 *   - MockSensorSource     -> cenários simulados (crash, celular caindo, freada...)
 * Quem escuta (IncidentManager, tela de viagem) não sabe e nem precisa saber qual é.
 * É assim que o dado de rotação mocado entra no app sem gambiarra.
 */
public abstract class SensorMonitor {

    private final List<SensorListener> listeners = new ArrayList<>();

    public void addListener(SensorListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(SensorListener listener) {
        listeners.remove(listener);
    }

    /** As subclasses chamam isto sempre que tiverem uma leitura nova. */
    protected void publish(SensorReading reading) {
        // Copia a lista: assim um listener pode se desinscrever no meio do aviso sem quebrar o loop.
        for (SensorListener listener : new ArrayList<>(listeners)) {
            listener.onReading(reading);
        }
    }

    public abstract void start();

    public abstract void stop();
}
