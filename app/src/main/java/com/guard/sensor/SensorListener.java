package com.guard.sensor;

import com.guard.model.SensorReading;

/**
 * PADRÃO OBSERVER, parte 1: o "ouvinte".
 *
 * CONCEITO: interface
 * Uma interface é um CONTRATO: só diz QUAIS métodos existem, não COMO funcionam.
 * Qualquer classe que escreva "implements SensorListener" é obrigada a ter onReading().
 *
 * Quem quer receber leituras (o detector, a viagem, a tela) implementa esta interface
 * e se inscreve num SensorMonitor com addListener(...).
 *
 * Como ela tem UM método só, é uma "interface funcional": dá para passar uma lambda
 * ou um method reference no lugar de criar uma classe inteira, por exemplo:
 *     source.addListener(trip::addReading);
 */
public interface SensorListener {

    /** Chamado a cada leitura nova. */
    void onReading(SensorReading reading);
}
