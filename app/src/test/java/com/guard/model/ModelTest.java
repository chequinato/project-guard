package com.guard.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

/** Testes das classes de modelo. Todos devem passar desde já. */
public class ModelTest {

    @Test
    public void eachVehicleHasItsOwnLimits() {
        assertEquals(4.0, new Car().getImpactThresholdG(), 0.001);
        assertEquals(20.0, new Motorcycle().getSpeedDropThresholdKmh(), 0.001);
        assertEquals(120.0, new Bicycle().getRotationThresholdDegPerSec(), 0.001);
    }

    @Test
    public void factoryCreatesTheRightSubclass() {
        assertTrue(Vehicle.of(VehicleType.MOTORCYCLE) instanceof Motorcycle);
        assertEquals(VehicleType.BICYCLE, Vehicle.of(VehicleType.BICYCLE).getType());
    }

    @Test
    public void contactsComeBackInPriorityOrder() {
        User user = new User("Lucas", new Motorcycle());
        user.addContact(new EmergencyContact("Bruno", "11 98888-0003", "Friend", 3));
        user.addContact(new EmergencyContact("Ana", "11 98888-0001", "Sister", 1));
        user.addContact(new EmergencyContact("Dad", "11 98888-0002", "Father", 2));

        List<EmergencyContact> list = user.getContactsByPriority();
        assertEquals("Ana", list.get(0).getName());
        assertEquals("Bruno", list.get(2).getName());
    }

    @Test(expected = IllegalStateException.class)
    public void twoContactsCannotShareAPriority() {
        User user = new User("Lucas", new Car());
        user.addContact(new EmergencyContact("Ana", "11988880001", "", 1));
        user.addContact(new EmergencyContact("Dad", "11988880002", "", 1));
    }

    @Test
    public void phoneIsCleanedUp() {
        EmergencyContact c = new EmergencyContact("Ana", "+55 (11) 98888-0001", "Sister", 1);
        assertEquals("+5511988880001", c.getPhone());
    }

    @Test(expected = IllegalArgumentException.class)
    public void tooShortPhoneIsRejected() {
        new EmergencyContact("Ana", "123", "", 1);
    }

    @Test
    public void incidentFollowsItsLifecycle() {
        Incident incident = new Incident(0, -23.1857, -46.8978, VehicleType.MOTORCYCLE);
        assertEquals(IncidentStatus.COUNTDOWN, incident.getStatus());
        incident.markAlertSent();
        incident.markConfirmedByContact();
        assertEquals(IncidentStatus.CONFIRMED_BY_CONTACT, incident.getStatus());
        assertTrue(incident.getMapLink().contains("-23.185700,-46.897800"));
    }

    @Test(expected = IllegalStateException.class)
    public void cannotCancelAfterAContactConfirmed() {
        Incident incident = new Incident(0, 0, 0, VehicleType.CAR);
        incident.markAlertSent();
        incident.markConfirmedByContact();
        incident.cancelByUser();
    }

    @Test
    public void tripTracksMaxSpeedAndEnds() {
        Trip trip = new Trip(VehicleType.BICYCLE, 1_000);
        trip.addReading(new SensorReading(1_500, 18, 0.2, 5, 0, 0));
        trip.addReading(new SensorReading(2_000, 24, 0.2, 5, 0, 0));
        trip.end(61_000);
        assertEquals(24, trip.getMaxSpeedKmh(), 0.001);
        assertEquals(60_000, trip.getDurationMillis());
        assertFalse(trip.isActive());
    }
}
