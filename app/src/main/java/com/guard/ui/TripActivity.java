package com.guard.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.guard.GuardApplication;
import com.guard.R;
import com.guard.detection.DecelerationDetector;
import com.guard.detection.ImpactDetector;
import com.guard.detection.IncidentManager;
import com.guard.detection.RolloverDetector;
import com.guard.detection.StillnessDetector;
import com.guard.model.Incident;
import com.guard.model.SensorReading;
import com.guard.model.Trip;
import com.guard.model.User;
import com.guard.model.Vehicle;
import com.guard.sensor.MockSensorSource;
import com.guard.sensor.Scenario;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;

/**
 * Tela 2 · Trip mode.
 *
 * Liga as peças: MockSensorSource --(Observer)--> IncidentManager --(callback)--> CountdownActivity.
 * Na apresentação usamos o simulador. Para sensores reais, troque MockSensorSource por
 * AndroidSensorSource (e peça a permissão de localização antes): o resto não muda.
 */
public class TripActivity extends AppCompatActivity {

    /** Cada leitura simulada vale 500 ms; tocamos a cada 250 ms (2x mais rápido) para a demo não arrastar. */
    private static final long TICK_MILLIS = 250;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Deque<SensorReading> queue = new ArrayDeque<>();

    private User user;
    private Trip trip;
    private MockSensorSource source;
    private IncidentManager manager;
    private long nextTimestamp;
    private boolean waitingForCountdown;

    private TextView txtSpeed, txtImpact, txtRotation, txtSignals, txtStatus;
    private LinearProgressIndicator barImpact, barRotation;

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (queue.isEmpty()) {
                queue.addAll(MockSensorSource.generate(Scenario.NORMAL_RIDE, nextTimestamp));
            }
            SensorReading reading = queue.poll();
            nextTimestamp = reading.getTimestampMillis() + MockSensorSource.STEP_MILLIS;
            source.push(reading);
            if (!waitingForCountdown) handler.postDelayed(this, TICK_MILLIS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_Guard_Dark);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip);

        user = ((GuardApplication) getApplication()).loadUser();
        Vehicle vehicle = user.getSelectedVehicle();
        trip = new Trip(vehicle.getType(), System.currentTimeMillis());

        txtSpeed = findViewById(R.id.txt_speed);
        txtImpact = findViewById(R.id.txt_impact);
        txtRotation = findViewById(R.id.txt_rotation);
        txtSignals = findViewById(R.id.txt_signals);
        txtStatus = findViewById(R.id.txt_status);
        barImpact = findViewById(R.id.bar_impact);
        barRotation = findViewById(R.id.bar_rotation);
        ((TextView) findViewById(R.id.txt_vehicle)).setText(vehicle.getDisplayName());

        // Observer: o mesmo sujeito avisa o detector, a viagem e a tela.
        manager = IncidentManager.withDefaultDetectors(vehicle);
        manager.setListener(this::onIncident);
        source = new MockSensorSource();
        source.addListener(manager);
        source.addListener(trip::addReading);
        source.addListener(this::render);
        source.start();

        buildScenarioChips();
        findViewById(R.id.btn_end).setOnClickListener(v -> endTrip());
        // Botão "voltar" do celular também encerra (e salva) a viagem.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                endTrip();
            }
        });
    }

    private void buildScenarioChips() {
        ChipGroup group = findViewById(R.id.group_scenarios);
        for (Scenario scenario : Scenario.values()) {
            if (scenario == Scenario.NORMAL_RIDE) continue;
            Chip chip = new Chip(this);
            chip.setText(scenario.getLabel());
            chip.setOnClickListener(v -> play(scenario));
            group.addView(chip);
        }
    }

    private void play(Scenario scenario) {
        queue.clear();
        queue.addAll(MockSensorSource.generate(scenario, nextTimestamp));
        txtStatus.setText(String.format("Simulating: %s. Expected: %s.", scenario.getLabel(),
                scenario.shouldAlert() ? "countdown starts" : "Guard keeps watching"));
    }

    private void render(SensorReading r) {
        Vehicle v = manager.getVehicle();
        txtSpeed.setText(String.format(Locale.US, "%.0f", r.getSpeedKmh()));
        txtImpact.setText(String.format(Locale.US, "%.1f g", r.getAccelerationG()));
        txtRotation.setText(String.format(Locale.US, "%.0f°/s", r.getRotationDegPerSec()));
        // Barra cheia = 2x o limite do veículo; o meio da barra é o limite.
        barImpact.setProgress(percent(r.getAccelerationG(), v.getImpactThresholdG() * 2));
        barRotation.setProgress(percent(r.getRotationDegPerSec(), v.getRotationThresholdDegPerSec() * 2));

        Map<String, Boolean> s = manager.activeSignals(r.getTimestampMillis());
        txtSignals.setText(String.format("%s IMPACT   %s SPEED DROP\n%s ROTATION   %s STILLNESS",
                dot(s.get(ImpactDetector.SIGNAL)), dot(s.get(DecelerationDetector.SIGNAL)),
                dot(s.get(RolloverDetector.SIGNAL)), dot(s.get(StillnessDetector.SIGNAL))));
    }

    private void onIncident(Incident incident) {
        waitingForCountdown = true;
        handler.removeCallbacks(ticker);
        trip.addIncident(incident);
        Intent intent = new Intent(this, CountdownActivity.class)
                .putExtra(CountdownActivity.EXTRA_TIME, incident.getTimestampMillis())
                .putExtra(CountdownActivity.EXTRA_LAT, incident.getLatitude())
                .putExtra(CountdownActivity.EXTRA_LNG, incident.getLongitude());
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (waitingForCountdown) {
            // Voltou da contagem: limpa os sinais e volta a pilotar normal.
            waitingForCountdown = false;
            queue.clear();
            manager.resume();
            txtStatus.setText("Back on the road. Guard is watching again.");
        }
        handler.post(ticker);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(ticker);
    }

    private void endTrip() {
        if (!trip.isActive()) return; // evita salvar duas vezes com toque duplo
        source.stop();
        trip.end(System.currentTimeMillis());
        ((GuardApplication) getApplication()).getTripDao().insert(trip);
        Toast.makeText(this, String.format(Locale.US, "Trip ended · max %.0f km/h · %d alert(s)",
                trip.getMaxSpeedKmh(), trip.getIncidents().size()), Toast.LENGTH_LONG).show();
        finish();
    }

    private static int percent(double value, double max) {
        return (int) Math.min(100, Math.round(value / max * 100));
    }

    private static String dot(Boolean active) {
        return Boolean.TRUE.equals(active) ? "●" : "○";
    }
}
