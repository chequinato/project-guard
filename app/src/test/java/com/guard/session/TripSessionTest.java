package com.guard.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.guard.alert.AlertService;
import com.guard.alert.DemoAlert;
import com.guard.model.EmergencyContact;
import com.guard.model.Incident;
import com.guard.model.IncidentStatus;
import com.guard.model.Trip;
import com.guard.model.User;
import com.guard.model.vehicle.Motorcycle;
import com.guard.sensor.MockSensorSource;
import com.guard.sensor.Scenario;

import org.junit.Before;
import org.junit.Test;

import java.util.Map;

/**
 * Testa a viagem completa: sensor -> detecção -> incidente -> alerta -> fim da viagem.
 * Principal objetivo: provar que a viagem e o alerta usam o MESMO objeto Incident.
 */
public class TripSessionTest {

    private User user;
    private MockSensorSource source;
    private TripSession session;

    @Before
    public void setUp() {
        user = new User("Lucas", new Motorcycle());
        user.addContact(new EmergencyContact("Ana", "11988880001", "Sister", 1));
        source = new MockSensorSource();
        session = new TripSession(user, source, 0);
    }

    @Test
    public void crashBecomesTheActiveIncidentAndIsSavedInTheTrip() {
        source.play(Scenario.CRASH, 0);

        Incident active = session.getActiveIncident();
        assertNotNull(active);
        // assertSame confere se é o MESMO objeto na memória (não só "parecido").
        assertSame(active, session.getTrip().getIncidents().get(0));
    }

    @Test
    public void statusChangedByTheAlertIsSeenByTheTrip() {
        source.play(Scenario.CRASH, 0);

        // A tela de alerta faz exatamente isto com o incidente ativo:
        AlertService alert = new AlertService(new DemoAlert());
        alert.start(user, session.getActiveIncident());
        alert.confirm();

        // Como é o mesmo objeto, a viagem já enxerga o status final.
        assertEquals(IncidentStatus.CONFIRMED_BY_CONTACT, session.getTrip().getIncidents().get(0).getStatus());
    }

    @Test
    public void resumeClearsTheActiveIncident() {
        source.play(Scenario.CRASH, 0);
        session.resumeAfterIncident();
        assertNull(session.getActiveIncident());
    }

    @Test
    public void endingTheTripStopsTheSensorAndClosesTheTrip() {
        source.play(Scenario.NORMAL_RIDE, 0);
        Trip trip = session.end(60_000);

        assertFalse(trip.isActive());
        assertFalse(source.isRunning());
        assertEquals(0, trip.getIncidents().size());
        assertEquals(53, trip.getMaxSpeedKmh(), 0.001);   // 50 + (i % 4), no máximo 53
    }

    @Test
    public void activeSignalsReportsAllFourDetectorSignals() {
        // Antes de nenhuma leitura: todos os sinais estão "desativados".
        Map<String, Boolean> before = session.activeSignals(0);
        assertEquals(4, before.size());
        for (Boolean value : before.values()) {
            assertFalse(value);
        }

        // O CRASH dispara todos os detectores, então pelo menos um sinal fica ativo.
        source.play(Scenario.CRASH, 0);
        Incident active = session.getActiveIncident();
        assertNotNull(active);
        // Passamos o "agora" = horário do incidente, que está dentro da janela de memória dos sinais.
        Map<String, Boolean> after = session.activeSignals(active.getTimestampMillis());
        assertEquals(4, after.size());
        boolean anyActive = false;
        for (Boolean value : after.values()) {
            if (value) anyActive = true;
        }
        assertTrue("CRASH deve manter pelo menos um sinal ativo", anyActive);
    }
}
