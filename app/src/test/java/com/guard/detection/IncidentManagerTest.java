package com.guard.detection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.guard.model.Incident;
import com.guard.model.Motorcycle;
import com.guard.sensor.MockSensorSource;
import com.guard.sensor.Scenario;

import org.junit.Test;

/**
 * Teste de integração: liga o simulador (Observer) ao IncidentManager e confere
 * se cada cenário do site dá a resposta certa.
 */
public class IncidentManagerTest {

    private Incident runScenario(Scenario scenario) {
        IncidentManager manager = IncidentManager.withDefaultDetectors(new Motorcycle());
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
            assertEquals("Cenário " + scenario.getLabel(), scenario.shouldAlert(), alerted);
        }
    }

    @Test
    public void crashCreatesAnIncidentWithTheLocation() {
        Incident incident = runScenario(Scenario.CRASH);
        assertNotNull(incident);
        assertEquals(-23.1857, incident.getLatitude(), 0.0001);
    }
}
