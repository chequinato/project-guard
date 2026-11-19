package com.guard.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.guard.model.vehicle.Bicycle;
import com.guard.model.vehicle.Car;
import com.guard.model.vehicle.Motorcycle;
import com.guard.model.vehicle.Vehicle;
import com.guard.model.vehicle.VehicleType;

import org.junit.Test;

/**
 * Edge cases da camada pura: validação de entrada, estados do Incident e fábrica do veículo.
 *
 * Não altera nenhuma regra do projeto — só confere que as regras existentes funcionam
 * nos casos limites. Rodagem sem emulador (classes Java puro).
 */
public class ModelEdgeCaseTest {

    // Subclasse local do mesmo pacote (com.guard.model) para testar o construtor protegido de Vehicle.
    private static class BadVehicle extends Vehicle {
        BadVehicle(VehicleType type, String displayName, double impact, double drop, double rot) {
            super(type, displayName, impact, drop, rot);
        }
    }

    // ---------- EmergencyContact: validação de entrada ----------

    @Test
    public void nameIsRequired() {
        assertThrows(IllegalArgumentException.class, () -> new EmergencyContact(null, "11999990001", "Father", 1));
        assertThrows(IllegalArgumentException.class, () -> new EmergencyContact("   ", "11999990001", "Father", 1));
    }

    @Test
    public void nameIsTrimmed() {
        EmergencyContact contact = new EmergencyContact("  Ana  ", "11999990001", "Father", 1);
        assertEquals("Ana", contact.getName());
    }

    @Test
    public void phoneIsCleanedAndMustHaveAtLeastEightDigits() {
        EmergencyContact contact = new EmergencyContact("Ana", "+55 (11) 98888-0001", "Sister", 1);
        assertEquals("+5511988880001", contact.getPhone());

        // Menos 8 dígitos: inválido.
        assertThrows(IllegalArgumentException.class, () -> new EmergencyContact("Ana", "1111111", "Sister", 1));
        // O + é contado? Não: só os dígitos contam.
        EmergencyContact withPlus = new EmergencyContact("Ana", "+5511988880001", "Sister", 1);
        assertEquals("+5511988880001", withPlus.getPhone());
    }

    @Test
    public void relationshipIsOptionalAndTrimmed() {
        EmergencyContact contact = new EmergencyContact("Ana", "11999990001", null, 1);
        assertEquals("", contact.getRelationship());

        EmergencyContact trimmed = new EmergencyContact("Ana", "11999990001", "  Brother  ", 1);
        assertEquals("Brother", trimmed.getRelationship());
    }

    @Test
    public void priorityStartsAtOne() {
        assertThrows(IllegalArgumentException.class, () -> new EmergencyContact("Ana", "11999990001", "Father", 0));
        assertEquals(1, new EmergencyContact("Ana", "11999990001", "Father", 1).getPriority());
    }

    @Test
    public void toStringShowsThePriorityOrder() {
        assertEquals("1. Ana (Sister)",
                new EmergencyContact("Ana", "11999990001", "Sister", 1).toString());
    }

    // ---------- User: validação e regras de prioridade ----------

    @Test
    public void nameAndVehicleAreRequired() {
        assertThrows(IllegalArgumentException.class, () -> new User("  ", new Car()));
        assertThrows(IllegalArgumentException.class, () -> new User("Lucas", null));
    }

    @Test
    public void addContactRejectsDuplicatePriority() {
        User user = new User("Lucas", new Car());
        user.addContact(new EmergencyContact("Ana", "11999990001", "Sister", 1));
        assertThrows(IllegalStateException.class,
                () -> user.addContact(new EmergencyContact("Bruno", "11999990002", "Brother", 1)));
    }

    @Test
    public void removeContactReturnsFalseWhenAbsent() {
        User user = new User("Lucas", new Car());
        user.addContact(new EmergencyContact("Ana", "11999990001", "Sister", 1));
        assertFalse(user.removeContact(new EmergencyContact("Nobody", "11999990002", "Sister", 9)));
        assertTrue(user.hasContacts());
    }

    @Test
    public void contactsAreSortedByPriority() {
        User user = new User("Lucas", new Car());
        user.addContact(new EmergencyContact("Ana", "11999990001", "Sister", 2));
        user.addContact(new EmergencyContact("Bruno", "11999990002", "Brother", 1));
        user.addContact(new EmergencyContact("Carlos", "11999990003", "Sister", 3));

        assertEquals(3, user.getContactsByPriority().size());
        assertEquals("Bruno", user.getContactsByPriority().get(0).getName());
        assertEquals("Ana", user.getContactsByPriority().get(1).getName());
        assertEquals("Carlos", user.getContactsByPriority().get(2).getName());
    }

    // ---------- Vehicle: fábrica e limites ----------

    @Test
    public void ofReturnsTheRightSubclassForEachType() {
        assertSame(Car.class, Vehicle.of(VehicleType.CAR).getClass());
        assertSame(Motorcycle.class, Vehicle.of(VehicleType.MOTORCYCLE).getClass());
        assertSame(Bicycle.class, Vehicle.of(VehicleType.BICYCLE).getClass());
    }

    @Test
    public void ofRejectsUnknownType() {
        assertThrows(IllegalArgumentException.class, () -> Vehicle.of((VehicleType) null));
    }

    @Test
    public void subclassesExposeTheOfficialLimits() {
        assertEquals(4.0, new Car().getImpactThresholdG(), 0.0001);
        assertEquals(25.0, new Car().getSpeedDropThresholdKmh(), 0.0001);
        assertEquals(200.0, new Car().getRotationThresholdDegPerSec(), 0.0001);

        assertEquals(3.0, new Motorcycle().getImpactThresholdG(), 0.0001);
        assertEquals(20.0, new Motorcycle().getSpeedDropThresholdKmh(), 0.0001);
        assertEquals(150.0, new Motorcycle().getRotationThresholdDegPerSec(), 0.0001);

        assertEquals(2.5, new Bicycle().getImpactThresholdG(), 0.0001);
        assertEquals(12.0, new Bicycle().getSpeedDropThresholdKmh(), 0.0001);
        assertEquals(120.0, new Bicycle().getRotationThresholdDegPerSec(), 0.0001);
    }

    @Test
    public void nonPositiveLimitsAreRejected() {
        // O construtor de Vehicle é protegido; esta subclasse vive no mesmo pacote, então pode chamar.
        assertThrows(IllegalArgumentException.class,
                () -> new BadVehicle(VehicleType.CAR, "Bad", 0, 25.0, 200.0));
        assertThrows(IllegalArgumentException.class,
                () -> new BadVehicle(VehicleType.MOTORCYCLE, "Bad", 3.0, 0, 150.0));
        assertThrows(IllegalArgumentException.class,
                () -> new BadVehicle(VehicleType.BICYCLE, "Bad", 2.5, 12.0, 0));
    }

    // ---------- Incident: máquina de estados (estados limites) ----------

    @Test
    public void canBeCancelledWhileCountingDown() {
        Incident incident = new Incident(0, 0, 0, VehicleType.MOTORCYCLE);
        assertEquals(IncidentStatus.COUNTDOWN, incident.getStatus());
        incident.cancelByUser();
        assertEquals(IncidentStatus.CANCELLED_BY_USER, incident.getStatus());
    }

    @Test
    public void canBeCancelledAfterTheAlertWasSent() {
        Incident incident = new Incident(0, 0, 0, VehicleType.MOTORCYCLE);
        incident.markAlertSent();
        incident.cancelByUser();
        assertEquals(IncidentStatus.CANCELLED_BY_USER, incident.getStatus());
    }

    @Test
    public void canEndAsNoResponseAfterAllContactsWereAvoided() {
        Incident incident = new Incident(0, 0, 0, VehicleType.MOTORCYCLE);
        incident.markAlertSent();
        incident.markNoResponse();
        assertEquals(IncidentStatus.NO_RESPONSE, incident.getStatus());
    }

    @Test
    public void confirmRequiresTheAlertToHaveBeenSent() {
        Incident incident = new Incident(0, 0, 0, VehicleType.MOTORCYCLE);
        assertThrows(IllegalStateException.class, incident::markConfirmedByContact);
        incident.markAlertSent();
        incident.markConfirmedByContact();
        assertEquals(IncidentStatus.CONFIRMED_BY_CONTACT, incident.getStatus());
    }

    @Test
    public void markNoResponseRequiresTheAlertToHaveBeenSent() {
        Incident incident = new Incident(0, 0, 0, VehicleType.MOTORCYCLE);
        assertThrows(IllegalStateException.class, incident::markNoResponse);
    }

    @Test
    public void mapLinkUsesSixDecimals() {
        String link = new Incident(0, 1.5, 2.5, VehicleType.MOTORCYCLE).getMapLink();
        assertEquals("https://maps.google.com/?q=1.500000,2.500000", link);
    }

    // ---------- SensorReading ----------

    @Test
    public void readingCarriesTimestampAndCoordinates() {
        SensorReading reading = new SensorReading(1234, 50, 0.5, 10, 1.5, 2.5);
        assertEquals(1234L, reading.getTimestampMillis());
        assertEquals(50.0, reading.getSpeedKmh(), 0.0001);
        assertEquals(1.5, reading.getLatitude(), 0.0001);
        assertEquals(2.5, reading.getLongitude(), 0.0001);
    }
}
