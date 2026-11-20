package com.guard.sensor;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;

import androidx.core.content.ContextCompat;

/**
 * Localização REAL do celular (GPS e rede), usada no link do mapa que vai no SMS.
 *
 * Usa o LocationManager do próprio Android (não precisa de biblioteca do Google).
 * Como funciona:
 *   1. start(): pega a última posição conhecida (resposta na hora) e pede atualizações
 *      ao GPS e à rede (Wi-Fi/antenas), a cada 2 segundos.
 *   2. Cada posição nova que for MELHOR que a anterior vai para o Listener.
 *   3. stop(): para de pedir (economiza bateria).
 *
 * Precisa da permissão de localização (pedida no "Start trip") e da localização LIGADA no celular.
 */
public class PhoneLocation {

    /** Quem quer receber as posições (a tela de alerta). */
    public interface Listener {
        void onLocation(Location location);
    }

    private static final long INTERVAL_MILLIS = 2_000;
    /** Uma posição mais nova que isto sempre substitui a anterior, mesmo sendo menos precisa. */
    private static final long FRESH_MILLIS = 30_000;

    private final LocationManager manager;
    private final Context context;
    private Location best;          // a melhor posição recebida até agora
    private Listener listener;

    /**
     * CLASSE ANÔNIMA implementando LocationListener. Escrevemos TODOS os métodos (mesmo vazios):
     * no Android 10 ou mais antigo eles não têm implementação padrão, e faltar algum fecharia o app.
     */
    private final LocationListener updates = new LocationListener() {
        @Override public void onLocationChanged(Location location) { offer(location); }
        @Override public void onStatusChanged(String provider, int status, Bundle extras) { }
        @Override public void onProviderEnabled(String provider) { }
        @Override public void onProviderDisabled(String provider) { }
    };

    public PhoneLocation(Context context) {
        this.context = context.getApplicationContext();
        this.manager = this.context.getSystemService(LocationManager.class);
    }

    /** Tem permissão de localização? */
    public boolean hasPermission() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    /** Começa a acompanhar a posição. Devolve false se não houver permissão ou localização ligada. */
    @SuppressWarnings("MissingPermission")   // a permissão é conferida em hasPermission()
    public boolean start(Listener listener) {
        this.listener = listener;
        if (manager == null || !hasPermission()) return false;
        boolean anyProvider = false;
        for (String provider : new String[]{LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER}) {
            if (!manager.isProviderEnabled(provider)) continue;   // GPS ou rede desligados
            anyProvider = true;
            offer(manager.getLastKnownLocation(provider));        // resposta imediata (pode ser antiga)
            manager.requestLocationUpdates(provider, INTERVAL_MILLIS, 0f, updates, Looper.getMainLooper());
        }
        return anyProvider;
    }

    public void stop() {
        if (manager != null) manager.removeUpdates(updates);
        listener = null;
    }

    /** Fica com a posição só se for melhor: mais nova (bem mais nova) ou mais precisa. */
    private void offer(Location candidate) {
        if (candidate == null) return;
        boolean better = best == null
                || candidate.getTime() - best.getTime() > FRESH_MILLIS
                || candidate.getAccuracy() <= best.getAccuracy();
        if (!better) return;
        best = candidate;
        if (listener != null) listener.onLocation(candidate);
    }
}
