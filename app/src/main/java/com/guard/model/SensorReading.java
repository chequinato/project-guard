package com.guard.model;

/**
 * Uma "foto" de todos os sensores num instante.
 *
 * POO: ENCAPSULAMENTO + IMUTABILIDADE.
 * Os campos são privados e finais: depois de criada, uma leitura nunca muda.
 * Assim nenhum detector consegue "estragar" o dado que outro detector vai ler.
 */
public final class SensorReading {

    private final long timestampMillis;
    private final double speedKmh;           // GPS
    private final double accelerationG;      // acelerômetro (módulo, em "g")
    private final double rotationDegPerSec;  // giroscópio (pode vir mocado)
    private final double latitude;
    private final double longitude;

    public SensorReading(long timestampMillis, double speedKmh, double accelerationG,
                         double rotationDegPerSec, double latitude, double longitude) {
        if (speedKmh < 0) {
            throw new IllegalArgumentException("Velocidade não pode ser negativa");
        }
        if (accelerationG < 0 || rotationDegPerSec < 0) {
            throw new IllegalArgumentException("Aceleração e rotação são módulos (>= 0)");
        }
        this.timestampMillis = timestampMillis;
        this.speedKmh = speedKmh;
        this.accelerationG = accelerationG;
        this.rotationDegPerSec = rotationDegPerSec;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public long getTimestampMillis() { return timestampMillis; }
    public double getSpeedKmh() { return speedKmh; }
    public double getAccelerationG() { return accelerationG; }
    public double getRotationDegPerSec() { return rotationDegPerSec; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }

    @Override
    public String toString() {
        return String.format(java.util.Locale.US,
                "t=%dms speed=%.1fkm/h acc=%.2fg rot=%.0f°/s",
                timestampMillis, speedKmh, accelerationG, rotationDegPerSec);
    }
}
