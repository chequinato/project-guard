package com.guard.model.vehicle;

/**
 * Carro.
 *
 * "extends Vehicle" = Car É UM Vehicle (herança). Ganha de graça todos os getters da mãe.
 * O carro protege mais o motorista, então precisa de um impacto MAIOR para disparar o alerta.
 */
public class Car extends Vehicle {

    public Car() {
        // super(...) chama o construtor da classe mãe (Vehicle).
        // Tem que ser a PRIMEIRA linha do construtor da filha.
        //    tipo            nome   impacto(g) queda(km/h) rotação(°/s)
        super(VehicleType.CAR, "Car", 4.0,       25.0,       200.0);
    }
}
