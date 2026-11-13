package com.guard.model;

/**
 * POO: HERANÇA + ABSTRAÇÃO.
 *
 * Vehicle é abstrata: não existe "um veículo genérico" no app, só Car, Motorcycle e Bicycle.
 * Cada filha define os próprios limites de detecção chamando o construtor da mãe (super).
 *
 * Limites oficiais (iguais aos do site):
 *   Car         4.0 g | 25 km/h perdidos em 2 s | 200 °/s
 *   Motorcycle  3.0 g | 20 km/h perdidos em 2 s | 150 °/s
 *   Bicycle     2.5 g | 12 km/h perdidos em 2 s | 120 °/s
 */
public abstract class Vehicle {

    /** Janela usada pela queda de velocidade: "X km/h perdidos em 2 segundos". */
    public static final long SPEED_DROP_WINDOW_MILLIS = 2_000;
    /** Tempo parado depois do evento para contar como "imobilidade". */
    public static final long STILLNESS_MILLIS = 10_000;

    private final VehicleType type;
    private final String displayName;
    private final double impactThresholdG;
    private final double speedDropThresholdKmh;
    private final double rotationThresholdDegPerSec;

    protected Vehicle(VehicleType type, String displayName, double impactThresholdG,
                      double speedDropThresholdKmh, double rotationThresholdDegPerSec) {
        if (impactThresholdG <= 0 || speedDropThresholdKmh <= 0 || rotationThresholdDegPerSec <= 0) {
            throw new IllegalArgumentException("Limites de detecção precisam ser positivos");
        }
        this.type = type;
        this.displayName = displayName;
        this.impactThresholdG = impactThresholdG;
        this.speedDropThresholdKmh = speedDropThresholdKmh;
        this.rotationThresholdDegPerSec = rotationThresholdDegPerSec;
    }

    public VehicleType getType() { return type; }
    public String getDisplayName() { return displayName; }
    public double getImpactThresholdG() { return impactThresholdG; }
    public double getSpeedDropThresholdKmh() { return speedDropThresholdKmh; }
    public double getRotationThresholdDegPerSec() { return rotationThresholdDegPerSec; }

    /**
     * Fábrica simples: transforma o tipo salvo no banco/na tela no objeto certo.
     * Quem chama não precisa saber qual subclasse existe (polimorfismo).
     */
    public static Vehicle of(VehicleType type) {
        switch (type) {
            case CAR: return new Car();
            case MOTORCYCLE: return new Motorcycle();
            case BICYCLE: return new Bicycle();
            default: throw new IllegalArgumentException("Tipo desconhecido: " + type);
        }
    }

    @Override
    public String toString() { return displayName; }
}
