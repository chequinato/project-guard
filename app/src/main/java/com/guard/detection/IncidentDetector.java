package com.guard.detection;

import com.guard.model.SensorReading;
import com.guard.model.vehicle.Vehicle;

/**
 * Contrato de um detector de UM sinal (impacto, queda de velocidade, rotação ou imobilidade).
 *
 * CONCEITOS: abstração + polimorfismo + padrão Strategy
 * - Abstração: a interface diz O QUE um detector faz (analisar uma leitura), não COMO.
 * - Polimorfismo: o IncidentManager guarda uma lista de IncidentDetector e chama analyze()
 *   em todos, sem saber qual classe concreta está do outro lado.
 * - Strategy: cada detector é uma "estratégia" de análise. Para criar um sinal novo,
 *   basta escrever uma classe que implementa esta interface; o resto do código não muda.
 */
public interface IncidentDetector {

    /** Nome curto do sinal: "impact", "speedDrop", "rotation" ou "stillness". */
    String getSignal();

    /**
     * Analisa uma leitura.
     *
     * @param reading a leitura dos sensores
     * @param vehicle o veículo escolhido (cada um tem limites diferentes)
     * @return true se ESTA leitura dispara o sinal
     */
    boolean analyze(SensorReading reading, Vehicle vehicle);

    /** Esquece o histórico guardado (ao começar uma viagem ou depois de um alerta). */
    void reset();
}
