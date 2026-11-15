package com.guard.detection;

import com.guard.model.Incident;

/**
 * Quem quer ser avisado de que um possível acidente aconteceu.
 * Interface funcional (um método só): dá para passar uma lambda, como nos testes:
 *     manager.setListener(incident -> ...);
 */
public interface IncidentListener {

    void onIncidentDetected(Incident incident);
}
