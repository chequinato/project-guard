package com.guard.model.vehicle;

/**
 * Um veículo e os LIMITES de detecção dele.
 *
 * CONCEITOS: herança + classe abstrata
 * - "abstract" significa que esta classe não pode virar objeto sozinha: new Vehicle(...) dá erro.
 *   Faz sentido, porque no app não existe "um veículo genérico", só Car, Motorcycle e Bicycle.
 * - As filhas usam "extends Vehicle" e HERDAM todos os campos e métodos daqui.
 *   Elas só precisam dizer quais são os seus limites (chamando o construtor da mãe com super(...)).
 *
 * Limites oficiais (os mesmos do site):
 *   Car         4.0 g | 25 km/h perdidos em 2 s | 200 °/s
 *   Motorcycle  3.0 g | 20 km/h perdidos em 2 s | 150 °/s
 *   Bicycle     2.5 g | 12 km/h perdidos em 2 s | 120 °/s
 */
public abstract class Vehicle {

    // ---------- Constantes ----------
    // "static" = pertence à CLASSE, não a cada objeto (todos os veículos compartilham o mesmo valor).
    // "final"  = não pode mudar depois de atribuído. static + final = constante.
    // Por convenção, constantes são escritas em MAIÚSCULAS_COM_UNDERLINE.
    // "long" porque tempo em milissegundos pode passar do limite de um int.
    // O "_" em 2_000 é só para facilitar a leitura: o valor é 2000.

    /** Janela da queda de velocidade: "perdeu X km/h em até 2 segundos". */
    public static final long SPEED_DROP_WINDOW_MILLIS = 2_000;

    /** Quanto tempo parado depois do evento conta como "imobilidade". */
    public static final long STILLNESS_MILLIS = 10_000;

    // ---------- Campos (atributos) ----------
    // "private" = só esta classe enxerga. Quem está fora usa os getters (ENCAPSULAMENTO).
    // "final"   = cada veículo recebe seus limites no construtor e eles nunca mudam.
    private final VehicleType type;
    private final String displayName;          // nome mostrado na tela ("Car", "Motorcycle"...)
    private final double impactThresholdG;     // impacto mínimo, em "g" (1 g = gravidade da Terra)
    private final double speedDropThresholdKmh;// quantos km/h precisa perder em 2 s
    private final double rotationThresholdDegPerSec; // giro mínimo, em graus por segundo

    /**
     * Construtor: roda quando um objeto é criado.
     *
     * "protected" = só as classes filhas (e o próprio pacote) podem chamar.
     * Assim ninguém de fora cria um Vehicle "solto"; só através de Car, Motorcycle ou Bicycle.
     */
    protected Vehicle(VehicleType type, String displayName, double impactThresholdG,
                      double speedDropThresholdKmh, double rotationThresholdDegPerSec) {
        // Validação: impede criar um veículo com limites sem sentido.
        // "throw" interrompe o construtor e o objeto nem chega a existir.
        if (impactThresholdG <= 0 || speedDropThresholdKmh <= 0 || rotationThresholdDegPerSec <= 0) {
            throw new IllegalArgumentException("Detection limits must be positive");
        }
        // "this.campo" é o campo do objeto; sem "this" é o parâmetro de mesmo nome.
        this.type = type;
        this.displayName = displayName;
        this.impactThresholdG = impactThresholdG;
        this.speedDropThresholdKmh = speedDropThresholdKmh;
        this.rotationThresholdDegPerSec = rotationThresholdDegPerSec;
    }

    // ---------- Getters ----------
    // Só leitura: não existem setters, porque os limites são fixos.
    public VehicleType getType() { return type; }
    public String getDisplayName() { return displayName; }
    public double getImpactThresholdG() { return impactThresholdG; }
    public double getSpeedDropThresholdKmh() { return speedDropThresholdKmh; }
    public double getRotationThresholdDegPerSec() { return rotationThresholdDegPerSec; }

    /**
     * PADRÃO FACTORY ("fábrica"): recebe o tipo e devolve o objeto certo.
     *
     * Quem chama escreve Vehicle.of(VehicleType.CAR) e recebe um Car,
     * sem precisar conhecer as subclasses. O retorno é "Vehicle" (o tipo da mãe),
     * mas o objeto de verdade é um Car: isso é POLIMORFISMO.
     *
     * "static" = chamamos pela classe (Vehicle.of), sem precisar de um objeto antes.
     */
    public static Vehicle of(VehicleType type) {
        switch (type) {
            case CAR:        return new Car();
            case MOTORCYCLE: return new Motorcycle();
            case BICYCLE:    return new Bicycle();
            default:         throw new IllegalArgumentException("Unknown vehicle type: " + type);
        }
    }

    /**
     * toString() existe em TODA classe Java (vem de Object, a "mãe de todas").
     * "@Override" avisa que estamos SOBRESCREVENDO um método herdado.
     * Se o nome estiver errado, o compilador reclama, o que evita bugs bobos.
     */
    @Override
    public String toString() {
        return displayName;
    }
}
