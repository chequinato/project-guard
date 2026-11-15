package com.guard.model.vehicle;

/**
 * Bicicleta. Anda mais devagar, então a queda de velocidade exigida é bem menor.
 */
public class Bicycle extends Vehicle {

    public Bicycle() {
        //    tipo                nome       impacto(g) queda(km/h) rotação(°/s)
        super(VehicleType.BICYCLE, "Bicycle", 2.5,       12.0,       120.0);
    }
}
