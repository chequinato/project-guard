package com.guard.detection;

import com.guard.model.Incident;

/** Quem quer saber que um possível acidente aconteceu (normalmente a tela de viagem). */
public interface IncidentListener {
    void onIncidentDetected(Incident incident);
}
