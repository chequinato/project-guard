package com.guard.sensor;

import com.guard.model.SensorReading;

/**
 * PADRÃO OBSERVER: quem quer receber leituras implementa esta interface
 * e se inscreve num SensorMonitor.
 */
public interface SensorListener {
    void onReading(SensorReading reading);
}
