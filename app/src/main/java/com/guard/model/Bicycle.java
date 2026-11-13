package com.guard.model;

/** Bicicleta: velocidades baixas, então a queda de velocidade exigida é menor. */
public class Bicycle extends Vehicle {
    public Bicycle() {
        super(VehicleType.BICYCLE, "Bicycle", 2.5, 12.0, 120.0);
    }
}
