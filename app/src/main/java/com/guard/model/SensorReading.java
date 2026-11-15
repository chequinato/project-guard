package com.guard.model;

// "import" traz uma classe de outro pacote para podermos usar só pelo nome curto.
import java.util.Locale;

/**
 * Uma "foto" de todos os sensores num instante (uma leitura a cada 500 ms).
 *
 * CONCEITOS: encapsulamento + imutabilidade
 * - Todos os campos são "private final": depois de criada, a leitura NUNCA muda.
 * - Não existem setters, só getters.
 * - A classe também é "final": ninguém pode criar uma filha dela e mudar esse comportamento.
 *
 * Por que isso importa? A mesma leitura é entregue para vários objetos ao mesmo tempo
 * (detectores, viagem, tela). Se um deles pudesse alterar o valor, os outros leriam lixo.
 */
public final class SensorReading {

    private final long timestampMillis;      // quando a leitura foi feita (milissegundos)
    private final double speedKmh;           // velocidade, vinda do GPS
    private final double accelerationG;      // aceleração linear, sem a gravidade (parado = ~0 g)
    private final double rotationDegPerSec;  // giro do celular, vindo do giroscópio (ou do mock)
    private final double latitude;           // posição, para o link do mapa
    private final double longitude;

    public SensorReading(long timestampMillis, double speedKmh, double accelerationG,
                         double rotationDegPerSec, double latitude, double longitude) {
        // Validamos na entrada: se o objeto existe, ele é válido.
        if (speedKmh < 0) {
            throw new IllegalArgumentException("Speed cannot be negative");
        }
        // "||" = OU lógico. Aceleração e rotação são MÓDULOS (tamanhos), então nunca negativos.
        if (accelerationG < 0 || rotationDegPerSec < 0) {
            throw new IllegalArgumentException("Acceleration and rotation must be >= 0");
        }
        this.timestampMillis = timestampMillis;
        this.speedKmh = speedKmh;
        this.accelerationG = accelerationG;
        this.rotationDegPerSec = rotationDegPerSec;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Getters escritos em uma linha só: é o mesmo que abrir chaves em linhas separadas.
    public long getTimestampMillis() { return timestampMillis; }
    public double getSpeedKmh() { return speedKmh; }
    public double getAccelerationG() { return accelerationG; }
    public double getRotationDegPerSec() { return rotationDegPerSec; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }

    /** Texto legível para debug, por exemplo: "t=500ms speed=50.0km/h acc=0.20g rot=8°/s". */
    @Override
    public String toString() {
        // String.format monta um texto com "buracos": %d = inteiro, %.1f = número com 1 casa decimal.
        // Locale.US garante ponto como separador decimal (senão no Brasil sairia "50,0").
        return String.format(Locale.US, "t=%dms speed=%.1fkm/h acc=%.2fg rot=%.0f°/s",
                timestampMillis, speedKmh, accelerationG, rotationDegPerSec);
    }
}
