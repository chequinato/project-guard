package com.guard.model;

/** Carro: o mais protegido, então precisa de um impacto maior para disparar. */
public class Car extends Vehicle {
    public Car() {
        super(VehicleType.CAR, "Car", 4.0, 25.0, 200.0);
    }
}
