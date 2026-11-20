package com.guard.model;

// "import static" traz MÉTODOS estáticos para usar sem o nome da classe:
// escrevemos assertEquals(...) em vez de Assert.assertEquals(...).
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.guard.model.vehicle.Bicycle;
import com.guard.model.vehicle.Car;
import com.guard.model.vehicle.Motorcycle;
import com.guard.model.vehicle.Vehicle;
import com.guard.model.vehicle.VehicleType;

import org.junit.Test;

import java.util.List;

/**
 * Testes das classes de modelo.
 *
 * Como funciona um teste JUnit:
 * - Cada método marcado com @Test é um teste independente.
 * - assertEquals(esperado, real) falha se os dois forem diferentes.
 * - Para rodar no Android Studio: botão ▶ ao lado do nome da classe. Não precisa de emulador.
 */
public class ModelTest {

    @Test
    public void eachVehicleHasItsOwnLimits() {
        // O terceiro número (0.001) é a tolerância: double pode ter erros minúsculos de arredondamento.
        assertEquals(4.0, new Car().getImpactThresholdG(), 0.001);
        assertEquals(20.0, new Motorcycle().getSpeedDropThresholdKmh(), 0.001);
        assertEquals(120.0, new Bicycle().getRotationThresholdDegPerSec(), 0.001);
    }

    @Test
    public void factoryCreatesTheRightSubclass() {
        // instanceof pergunta: "este objeto é desta classe (ou de uma filha dela)?"
        assertTrue(Vehicle.of(VehicleType.MOTORCYCLE) instanceof Motorcycle);
        assertEquals(VehicleType.BICYCLE, Vehicle.of(VehicleType.BICYCLE).getType());
    }

    @Test
    public void contactsComeBackInPriorityOrder() {
        User user = new User("Lucas", new Motorcycle());
        // Adicionados FORA de ordem de propósito.
        user.addContact(new EmergencyContact("Bruno", "11 98888-0003", "Friend", 3));
        user.addContact(new EmergencyContact("Ana", "11 98888-0001", "Sister", 1));
        user.addContact(new EmergencyContact("Dad", "11 98888-0002", "Father", 2));

        List<EmergencyContact> list = user.getContactsByPriority();
        assertEquals("Ana", list.get(0).getName());
        assertEquals("Bruno", list.get(2).getName());
    }

    // "expected" = este teste PASSA se a exceção acontecer (estamos testando a proteção).
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
        incident.cancelByUser();   // tarde demais: deve lançar exceção
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

    @Test
    public void realLocationReplacesTheSimulatedOneInTheMapLink() {
        Incident incident = new Incident(0, -23.1857, -46.8978, VehicleType.MOTORCYCLE);
        incident.updateLocation(-23.550520, -46.633308);   // posição real (GPS)
        assertEquals("https://maps.google.com/?q=-23.550520,-46.633308", incident.getMapLink());
        assertTrue(incident.isFromGps());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsAnImpossibleLocation() {
        new Incident(0, 0, 0, VehicleType.CAR).updateLocation(123, 0);   // latitude não passa de 90
    }
}
