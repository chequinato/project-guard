// "package" diz em qual pasta/pacote a classe mora. Tem que bater com o caminho do arquivo:
// app/src/main/java/com/guard/model/vehicle/VehicleType.java -> com.guard.model.vehicle
package com.guard.model.vehicle;

/**
 * Os tipos de veículo que o usuário pode escolher.
 *
 * CONCEITO: enum
 * Um enum é um tipo com uma lista FECHADA de valores. Não dá para criar um VehicleType novo
 * em tempo de execução: só existem estes três. Isso evita erros como digitar "Moto" num lugar
 * e "motorcycle" em outro, o que aconteceria se usássemos String.
 *
 * Também é o valor salvo no banco: VehicleType.CAR.name() vira o texto "CAR",
 * e VehicleType.valueOf("CAR") transforma o texto de volta no enum.
 */
public enum VehicleType {
    CAR,
    MOTORCYCLE,
    BICYCLE
}
