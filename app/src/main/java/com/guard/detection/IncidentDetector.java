package com.guard.detection;

import com.guard.model.SensorReading;
import com.guard.model.Vehicle;

/**
 * ABSTRAÇÃO (interface) + POLIMORFISMO + STRATEGY.
 *
 * Cada detector olha para UM sinal e responde "disparei ou não" para a leitura atual.
 * O IncidentManager tem uma lista de IncidentDetector e chama analyze() em todos,
 * sem saber qual classe concreta está do outro lado.
 */
public interface IncidentDetector {

    /** Nome curto do sinal: "impact", "speedDrop", "rotation", "stillness". */
    String getSignal();

    /**
     * @return true se ESTA leitura dispara o sinal para o veículo informado.
     *         Os limites vêm do Vehicle (cada perfil tem os seus).
     */
    boolean analyze(SensorReading reading, Vehicle vehicle);

    /** Esquece o histórico (usado ao começar uma viagem nova ou depois de um alerta). */
    void reset();
}
