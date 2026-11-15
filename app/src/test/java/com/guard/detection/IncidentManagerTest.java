package com.guard.detection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.guard.model.Incident;
import com.guard.model.vehicle.Motorcycle;
import com.guard.sensor.MockSensorSource;
import com.guard.sensor.Scenario;

import org.junit.Test;

/**
 * Teste de INTEGRAÇÃO: liga o simulador (Observer) ao IncidentManager e confere
 * se cada cenário do site dá a resposta certa. É o teste mais importante da detecção.
 */
public class IncidentManagerTest {

    /** Toca um cenário inteiro e devolve o incidente detectado (ou null se nada disparou). */
    private Incident runScenario(Scenario scenario) {
        IncidentManager manager = IncidentManager.withDefaultDetectors(new Motorcycle());

        // Truque do "array de 1 posição": uma lambda não pode REATRIBUIR uma variável de fora,
        // mas pode mudar o CONTEÚDO de um array. Assim a lambda "devolve" o incidente para cá.
        Incident[] detected = new Incident[1];
        manager.setListener(incident -> detected[0] = incident);

        MockSensorSource source = new MockSensorSource();
        source.addListener(manager);
        source.play(scenario, 0);
        return detected[0];
    }

    @Test
    public void everyScenarioMatchesTheExpectedAnswer() {
        for (Scenario scenario : Scenario.values()) {
            boolean alerted = runScenario(scenario) != null;
            // A primeira String é a mensagem mostrada se o teste falhar.
            assertEquals("Scenario " + scenario.getLabel(), scenario.shouldAlert(), alerted);
        }
    }

    @Test
    public void crashCreatesAnIncidentWithTheLocation() {
        Incident incident = runScenario(Scenario.CRASH);
        assertNotNull(incident);
        assertEquals(-23.1857, incident.getLatitude(), 0.0001);
    }
}
