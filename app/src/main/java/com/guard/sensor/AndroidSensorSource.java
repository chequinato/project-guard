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
import android.os.Looper;

import com.guard.model.SensorReading;

/**
 * Fonte de leituras REAIS do celular.
 *
 *   velocidade -> GPS (LocationManager)
 *   impacto    -> TYPE_LINEAR_ACCELERATION (já sem gravidade)
 *   rotação    -> TYPE_GYROSCOPE. Se o aparelho não tiver giroscópio, fica 0 (o grupo pode
 *                 trocar por um valor mocado aqui, sem mexer em nenhuma outra classe).
 *
 * A cada STEP_MILLIS juntamos o PICO de aceleração e rotação do intervalo numa SensorReading.
 * Quem chama start() precisa ter pedido a permissão de localização antes.
 */
public class AndroidSensorSource extends SensorMonitor implements SensorEventListener, LocationListener {

    private static final long STEP_MILLIS = 500;
    private static final float STANDARD_GRAVITY = SensorManager.GRAVITY_EARTH;

    private final SensorManager sensorManager;
    private final LocationManager locationManager;

    private double lastSpeedKmh;
    private double lastLat;
    private double lastLng;
    private double peakAccG;
    private double peakRotDegPerSec;
    private long lastPublishMillis;

    public AndroidSensorSource(Context context) {
        this.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    public boolean hasGyroscope() {
        return sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null;
    }

    @SuppressLint("MissingPermission") // a Activity pede a permissão antes de chamar start()
    @Override
    public void start() {
        Sensor acc = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        Sensor gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        if (acc != null) sensorManager.registerListener(this, acc, SensorManager.SENSOR_DELAY_GAME);
        if (gyro != null) sensorManager.registerListener(this, gyro, SensorManager.SENSOR_DELAY_GAME);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this, Looper.getMainLooper());
    }

    @Override
    public void stop() {
        sensorManager.unregisterListener(this);
        locationManager.removeUpdates(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        double magnitude = Math.sqrt(event.values[0] * event.values[0]
                + event.values[1] * event.values[1]
                + event.values[2] * event.values[2]);

        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            peakAccG = Math.max(peakAccG, magnitude / STANDARD_GRAVITY);  // m/s² -> g
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            peakRotDegPerSec = Math.max(peakRotDegPerSec, Math.toDegrees(magnitude)); // rad/s -> °/s
        }

        long now = System.currentTimeMillis();
        if (now - lastPublishMillis >= STEP_MILLIS) {
            publish(new SensorReading(now, lastSpeedKmh, peakAccG, peakRotDegPerSec, lastLat, lastLng));
            lastPublishMillis = now;
            peakAccG = 0;
            peakRotDegPerSec = 0;
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        lastSpeedKmh = location.hasSpeed() ? location.getSpeed() * 3.6 : 0; // m/s -> km/h
        lastLat = location.getLatitude();
        lastLng = location.getLongitude();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // não usamos
    }

    // Android 8-10 exige estes três métodos do LocationListener (a partir do 11 já têm implementação padrão).
    @Override
    public void onStatusChanged(String provider, int status, android.os.Bundle extras) { }

    @Override
    public void onProviderEnabled(String provider) { }

    @Override
    public void onProviderDisabled(String provider) { }
}
