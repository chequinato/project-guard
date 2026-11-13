package com.guard.model;

/** Moto: piloto exposto, limites mais sensíveis que o carro. */
public class Motorcycle extends Vehicle {
    public Motorcycle() {
        super(VehicleType.MOTORCYCLE, "Motorcycle", 3.0, 20.0, 150.0);
    }
}
