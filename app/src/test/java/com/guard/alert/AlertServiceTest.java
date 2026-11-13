package com.guard.alert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.guard.model.EmergencyContact;
import com.guard.model.Incident;
import com.guard.model.IncidentStatus;
import com.guard.model.Motorcycle;
import com.guard.model.User;
import com.guard.model.VehicleType;

import org.junit.Before;
import org.junit.Test;

public class AlertServiceTest {

    private User user;
    private Incident incident;
    private DemoAlert channel;
    private AlertService service;

    @Before
    public void setUp() {
        user = new User("Lucas", new Motorcycle());
        user.addContact(new EmergencyContact("Ana", "11988880001", "Sister", 1));
        user.addContact(new EmergencyContact("Dad", "11988880002", "Father", 2));
        incident = new Incident(0, -23.1857, -46.8978, VehicleType.MOTORCYCLE);
        channel = new DemoAlert();
        service = new AlertService(channel);
    }

    @Test
    public void startSendsToTheFirstContact() {
        service.start(user, incident);
        assertEquals(1, channel.getSentMessages().size());
        assertTrue(channel.getSentMessages().get(0).startsWith("Ana"));
        assertEquals(IncidentStatus.ALERT_SENT, incident.getStatus());
    }

    @Test
    public void messageHasNameVehicleAndMapLink() {
        String msg = AlertService.buildMessage(user, incident);
        assertTrue(msg.contains("Lucas"));
        assertTrue(msg.contains("motorcycle"));
        assertTrue(msg.contains("https://maps.google.com/?q="));
    }

    @Test
    public void escalateGoesToTheNextContact() {
        service.start(user, incident);
        assertTrue(service.escalate());
        assertEquals("Dad", service.getCurrentContact().getName());
        assertEquals(2, channel.getSentMessages().size());
    }

    @Test
    public void escalateAfterTheLastContactMeansNoResponse() {
        service.start(user, incident);
        service.escalate();               // Dad
        assertFalse(service.escalate());  // acabou a lista
        assertEquals(IncidentStatus.NO_RESPONSE, incident.getStatus());
    }
}
