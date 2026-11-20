package com.guard.alert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.guard.model.EmergencyContact;
import com.guard.model.Incident;
import com.guard.model.IncidentStatus;
import com.guard.model.User;
import com.guard.model.vehicle.Motorcycle;
import com.guard.model.vehicle.VehicleType;

import org.junit.Before;
import org.junit.Test;

/** Testes do envio de alerta e do escalonamento, usando o DemoAlert (não envia SMS). */
public class AlertServiceTest {

    private User user;
    private Incident incident;
    private DemoAlert channel;
    private AlertService service;

    /** @Before roda ANTES DE CADA teste: todo teste começa com objetos novos e limpos. */
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

    @Test
    public void customTemplateIsWhatGetsSent() {
        // Lambda no lugar do MessageTemplate: é assim que a tela manda o texto traduzido.
        AlertService pt = new AlertService(channel, (u, i) -> "ALERTA: " + u.getName());
        pt.start(user, incident);
        assertEquals("Ana <- ALERTA: Lucas", channel.getSentMessages().get(0));
        assertEquals("ALERTA: Lucas", pt.getMessage());
    }

    @Test
    public void reportsWhenTheChannelFails() {
        // Classe anônima: um canal que sempre falha (como um celular sem chip).
        AlertChannel broken = new AlertChannel() {
            @Override public String getName() { return "Broken"; }
            @Override public boolean send(EmergencyContact contact, String message) { return false; }
        };
        AlertService failing = new AlertService(broken);
        failing.start(user, incident);
        assertFalse(failing.wasLastSendOk());
        service.start(user, new Incident(0, -23.1857, -46.8978, VehicleType.MOTORCYCLE));
        assertTrue(service.wasLastSendOk());
    }

    @Test(expected = IllegalArgumentException.class)
    public void templateIsRequired() {
        new AlertService(channel, null);
    }

    @Test
    public void recognizesTheReplyOnlyFromAlertedContacts() {
        service.start(user, incident);   // só a Ana foi avisada
        // A operadora pode mandar o remetente em outro formato: ainda é a Ana.
        assertEquals("Ana", service.findAlertedContact("+55 11 98888-0001").getName());
        // O Dad ainda não foi avisado: um SMS dele não conta como resposta.
        assertNull(service.findAlertedContact("11988880002"));
        // Número de um estranho também não.
        assertNull(service.findAlertedContact("+5521999990000"));

        service.escalate();              // agora o Dad também foi avisado
        assertEquals("Dad", service.findAlertedContact("011988880002").getName());
    }

    @Test
    public void replyConfirmsTheIncidentAndRemembersWho() {
        service.start(user, incident);
        service.escalate();
        EmergencyContact ana = service.findAlertedContact("11988880001");
        service.confirmBy(ana);
        assertEquals(IncidentStatus.CONFIRMED_BY_CONTACT, incident.getStatus());
        assertEquals("Ana", service.getConfirmedBy().getName());
    }
}
