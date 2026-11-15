package com.guard.sensor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;

import com.guard.model.SensorReading;

/**
 * Fonte de leituras REAIS do celular. (Hoje o app usa o MockSensorSource; esta classe está pronta
 * para quando quiserem testar num celular físico. Veja o README.)
 *
 *   velocidade -> GPS (LocationManager)
 *   impacto    -> TYPE_LINEAR_ACCELERATION (aceleração já sem a gravidade)
 *   rotação    -> TYPE_GYROSCOPE. Sem giroscópio no aparelho, fica 0.
 *
 * A cada 500 ms juntamos o PICO de aceleração e de rotação do intervalo numa SensorReading.
 * Pico (e não média) porque uma batida dura frações de segundo e a média "apagaria" o impacto.
 *
 * Esta classe herda de SensorMonitor (extends) e TAMBÉM implementa duas interfaces do Android.
 * Em Java uma classe só pode ter UMA mãe, mas pode implementar VÁRIAS interfaces.
 *
 * Atenção: quem chamar start() precisa ter pedido a permissão de localização antes.
 */
public class AndroidSensorSource extends SensorMonitor implements SensorEventListener, LocationListener {

    private static final long STEP_MILLIS = 500;

    private final SensorManager sensorManager;     // dá acesso aos sensores de movimento
    private final LocationManager locationManager; // dá acesso ao GPS

    // Últimos valores conhecidos e picos do intervalo atual.
    private double lastSpeedKmh;
    private double lastLat;
    private double lastLng;
    private double peakAccG;
    private double peakRotDegPerSec;
    private long lastPublishMillis;

    public AndroidSensorSource(Context context) {
        // getSystemService devolve um "Object"; o (SensorManager) na frente é um CAST:
        // estamos dizendo ao compilador "confia, esse objeto é um SensorManager".
        this.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    /** O aparelho tem giroscópio? Se não tiver, a rotação ficará sempre 0. */
    public boolean hasGyroscope() {
        return sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null;
    }

    // @SuppressLint esconde o aviso de "falta permissão": quem chama já pediu antes.
    @SuppressLint("MissingPermission")
    @Override
    public void start() {
        Sensor acc = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        Sensor gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        // "this" vai como ouvinte: o Android chamará onSensorChanged() deste objeto.
        if (acc != null) sensorManager.registerListener(this, acc, SensorManager.SENSOR_DELAY_GAME);
        if (gyro != null) sensorManager.registerListener(this, gyro, SensorManager.SENSOR_DELAY_GAME);
        // GPS: uma atualização por segundo (1000 ms), sem distância mínima (0 m).
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this, Looper.getMainLooper());
    }

    @Override
    public void stop() {
        sensorManager.unregisterListener(this);
        locationManager.removeUpdates(this);
    }

    /** O Android chama isto MUITAS vezes por segundo, para cada sensor registrado. */
    @Override
    public void onSensorChanged(SensorEvent event) {
        // values[0], [1], [2] = eixos x, y, z. O tamanho do vetor é √(x² + y² + z²).
        double magnitude = Math.sqrt(event.values[0] * event.values[0]
                + event.values[1] * event.values[1]
                + event.values[2] * event.values[2]);

        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            // O sensor mede em m/s². Dividindo pela gravidade (9,8 m/s²) temos o valor em "g".
            peakAccG = Math.max(peakAccG, magnitude / SensorManager.GRAVITY_EARTH);
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            // O giroscópio mede em radianos/s. Math.toDegrees converte para graus/s.
            peakRotDegPerSec = Math.max(peakRotDegPerSec, Math.toDegrees(magnitude));
        }

        // Passaram 500 ms desde a última publicação? Então publica o pico e zera para o próximo intervalo.
        long now = System.currentTimeMillis();
        if (now - lastPublishMillis >= STEP_MILLIS) {
            publish(new SensorReading(now, lastSpeedKmh, peakAccG, peakRotDegPerSec, lastLat, lastLng));
            lastPublishMillis = now;
            peakAccG = 0;
            peakRotDegPerSec = 0;
        }
    }

    /** O Android chama isto quando o GPS tem uma posição nova. */
    @Override
    public void onLocationChanged(Location location) {
        // getSpeed() vem em m/s. Multiplicar por 3,6 converte para km/h.
        lastSpeedKmh = location.hasSpeed() ? location.getSpeed() * 3.6 : 0;
        lastLat = location.getLatitude();
        lastLng = location.getLongitude();
    }

    // Métodos que as interfaces exigem, mas que não usamos. Ficam vazios de propósito.
    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) { }
    @Override public void onStatusChanged(String provider, int status, Bundle extras) { }
    @Override public void onProviderEnabled(String provider) { }
    @Override public void onProviderDisabled(String provider) { }
}
