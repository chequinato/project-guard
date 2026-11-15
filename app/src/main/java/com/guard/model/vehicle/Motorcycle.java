package com.guard.model.vehicle;

/**
 * Moto. O piloto fica exposto, então os limites são mais sensíveis que os do carro.
 * Mesma ideia do Car: só muda os números passados para a mãe.
 */
public class Motorcycle extends Vehicle {

    public Motorcycle() {
        //    tipo                   nome          impacto(g) queda(km/h) rotação(°/s)
        super(VehicleType.MOTORCYCLE, "Motorcycle", 3.0,       20.0,       150.0);
    }
}
