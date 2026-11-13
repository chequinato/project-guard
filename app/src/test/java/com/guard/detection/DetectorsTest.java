package com.guard.detection;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.guard.model.Car;
import com.guard.model.Motorcycle;
import com.guard.model.SensorReading;
import com.guard.model.Vehicle;

import org.junit.Test;

/**
 * Testes dos detectores.
 *
 * Para rodar: botão ▶ ao lado da classe no Android Studio (não precisa de emulador).
 */
public class DetectorsTest {

    private final Vehicle moto = new Motorcycle(); // limites: 3.0 g | 20 km/h em 2 s | 150 °/s

    private static SensorReading at(long t, double speed, double acc, double rot) {
        return new SensorReading(t, speed, acc, rot, 0, 0);
    }

    // ---------- Impacto ----------

    @Test
    public void impact_firesAtOrAboveTheLimit() {
        ImpactDetector d = new ImpactDetector();
        assertTrue(d.analyze(at(0, 50, 3.0, 0), moto));
        assertFalse(d.analyze(at(0, 50, 2.9, 0), moto));
    }

    @Test
    public void impact_sameHitMeansDifferentThingsPerVehicle() {
        ImpactDetector d = new ImpactDetector();
        SensorReading hit = at(0, 50, 3.5, 0);
        assertTrue(d.analyze(hit, moto));       // moto: 3.0 g
        assertFalse(d.analyze(hit, new Car())); // carro: 4.0 g
    }

    // ---------- Rotação ----------

    @Test
    public void rollover_firesAtOrAboveTheLimit() {
        RolloverDetector d = new RolloverDetector();
        assertTrue(d.analyze(at(0, 50, 0.2, 150), moto));
        assertFalse(d.analyze(at(0, 50, 0.2, 149), moto));
    }

    // ---------- Queda de velocidade ----------

    @Test
    public void deceleration_firesWhenSpeedFallsFastEnough() {
        DecelerationDetector d = new DecelerationDetector();
        assertFalse(d.analyze(at(0, 50, 0, 0), moto));
        assertFalse(d.analyze(at(500, 45, 0, 0), moto));
        assertTrue(d.analyze(at(1_500, 25, 0, 0), moto)); // perdeu 25 km/h em 1,5 s
    }

    @Test
    public void deceleration_ignoresSlowBraking() {
        DecelerationDetector d = new DecelerationDetector();
        // Perde 5 km/h a cada segundo: 50 -> 20 em 6 s. Nunca 20 km/h dentro de 2 s.
        boolean fired = false;
        for (int i = 0; i <= 12; i++) {
            fired |= d.analyze(at(i * 500L, 50 - i * 2.5, 0, 0), moto);
        }
        assertFalse(fired);
    }

    @Test
    public void deceleration_forgetsOldReadings() {
        DecelerationDetector d = new DecelerationDetector();
        d.analyze(at(0, 60, 0, 0), moto);
        // 5 s depois a 30 km/h: a leitura de 60 km/h já saiu da janela de 2 s.
        assertFalse(d.analyze(at(5_000, 30, 0, 0), moto));
    }

    // ---------- Imobilidade ----------

    @Test
    public void stillness_needsTenSecondsWithoutMovement() {
        StillnessDetector d = new StillnessDetector();
        assertFalse(d.analyze(at(0, 0, 0.02, 1), moto));
        assertFalse(d.analyze(at(9_500, 0, 0.02, 1), moto));
        assertTrue(d.analyze(at(10_000, 0, 0.02, 1), moto));
    }

    @Test
    public void stillness_restartsWhenSomethingMoves() {
        StillnessDetector d = new StillnessDetector();
        d.analyze(at(0, 0, 0.02, 1), moto);
        d.analyze(at(6_000, 0, 0.8, 1), moto);            // a pessoa se mexeu
        assertFalse(d.analyze(at(12_000, 0, 0.02, 1), moto)); // voltou a ficar parado agora
        assertFalse(d.analyze(at(16_000, 0, 0.02, 1), moto)); // só 4 s parado
        assertTrue(d.analyze(at(22_000, 0, 0.02, 1), moto));  // agora sim, 10 s
    }
}
