package com.guard.sensor;

import com.guard.model.SensorReading;

import java.util.ArrayList;
import java.util.List;

/**
 * PADRÃO OBSERVER, parte 2: o "sujeito" (quem avisa).
 *
 * Guarda uma lista de ouvintes e, a cada leitura nova, avisa todos eles.
 *
 * Ela é ABSTRATA porque a ORIGEM das leituras muda:
 *   - MockSensorSource    -> leituras simuladas (cenários do site)
 *   - AndroidSensorSource -> sensores reais do celular
 * Quem escuta não sabe, nem precisa saber, qual das duas está falando.
 * É assim que o dado mocado entra no app sem gambiarra: trocar uma pela outra
 * não muda NENHUMA linha do detector.
 */
public abstract class SensorMonitor {

    private final List<SensorListener> listeners = new ArrayList<>();

    /** Inscreve um ouvinte (sem repetir o mesmo duas vezes). */
    public void addListener(SensorListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /** Cancela a inscrição. */
    public void removeListener(SensorListener listener) {
        listeners.remove(listener);
    }

    /**
     * Avisa todos os ouvintes. As subclasses chamam isto quando têm uma leitura nova.
     *
     * "protected" = só as filhas (e o pacote) podem chamar. Quem está de fora
     * não consegue "inventar" leituras e empurrar para os ouvintes.
     */
    protected void publish(SensorReading reading) {
        // Percorremos uma CÓPIA da lista (new ArrayList<>(listeners)).
        // Assim, se um ouvinte se desinscrever no meio do aviso, o loop não quebra.
        for (SensorListener listener : new ArrayList<>(listeners)) {
            listener.onReading(reading);
        }
    }

    // MÉTODOS ABSTRATOS: sem corpo (sem chaves). Cada filha é OBRIGADA a escrever o seu.

    /** Começa a produzir leituras. */
    public abstract void start();

    /** Para de produzir leituras. */
    public abstract void stop();
}
