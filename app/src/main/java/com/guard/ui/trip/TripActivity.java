package com.guard.ui.trip;

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
import com.guard.detection.RolloverDetector;
import com.guard.detection.StillnessDetector;
import com.guard.model.Incident;
import com.guard.model.SensorReading;
import com.guard.model.Trip;
import com.guard.model.vehicle.Vehicle;
import com.guard.sensor.MockSensorSource;
import com.guard.sensor.Scenario;
import com.guard.session.TripSession;
import com.guard.ui.alert.AlertActivity;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

/**
 * Tela 2 · "Trip mode" (a tela escura; layout: activity_trip.xml).
 *
 * Esta tela só DESENHA e TOCA a simulação. Quem coordena sensor, detecção e viagem
 * é a TripSession (por isso a Activity implementa TripSession.Listener: ela "ouve" a sessão).
 *
 * Fonte das leituras: MockSensorSource (dado simulado, como no site).
 * Para usar os sensores reais, troque por AndroidSensorSource em onCreate()
 * (e peça a permissão de localização antes). O resto não muda.
 */
public class TripActivity extends AppCompatActivity implements TripSession.Listener {

    /**
     * Cada leitura simulada representa 500 ms. Tocamos uma a cada 250 ms
     * (2x mais rápido) para a demonstração não ficar arrastada.
     */
    private static final long TICK_MILLIS = 250;

    // HANDLER: agenda tarefas para rodar mais tarde na "thread principal" (a que mexe na tela).
    private final Handler handler = new Handler(Looper.getMainLooper());

    // Fila de leituras que ainda vão ser tocadas.
    private final Deque<SensorReading> queue = new ArrayDeque<>();

    private GuardApplication app;
    private TripSession session;
    private MockSensorSource source;
    private long nextTimestamp;          // horário simulado da próxima leitura
    private boolean waitingForAlert;     // true enquanto a tela "Are you OK?" está aberta
    private boolean ended;               // evita encerrar (e salvar) a viagem duas vezes

    // Várias variáveis do MESMO tipo podem ser declaradas numa linha só, separadas por vírgula.
    private TextView txtSpeed, txtImpact, txtRotation, txtSignals, txtStatus;
    private LinearProgressIndicator barImpact, barRotation;

    /**
     * O "relógio" da simulação, a cada 250 ms entrega uma leitura.
     *
     * CLASSE ANÔNIMA: "new Runnable() { ... }" cria, ali mesmo, um objeto de uma classe
     * sem nome que implementa a interface Runnable (que só tem o método run()).
     */
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            // Acabaram as leituras do cenário? Volta para a pilotagem normal.
            if (queue.isEmpty()) {
                queue.addAll(MockSensorSource.generate(Scenario.NORMAL_RIDE, nextTimestamp));
            }
            SensorReading reading = queue.poll();   // tira a primeira da fila
            nextTimestamp = reading.getTimestampMillis() + MockSensorSource.STEP_MILLIS;
            source.push(reading);                   // entrega para os ouvintes (Observer)

            // Agenda a si mesmo de novo, a não ser que um acidente tenha sido detectado.
            if (!waitingForAlert) handler.postDelayed(this, TICK_MILLIS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip);
        app = (GuardApplication) getApplication();

        txtSpeed = findViewById(R.id.txt_speed);
        txtImpact = findViewById(R.id.txt_impact);
        txtRotation = findViewById(R.id.txt_rotation);
        txtSignals = findViewById(R.id.txt_signals);
        txtStatus = findViewById(R.id.txt_status);
        barImpact = findViewById(R.id.bar_impact);
        barRotation = findViewById(R.id.bar_rotation);

        // Começa a viagem com o sensor SIMULADO. "this" = esta tela recebe os avisos da sessão.
        source = new MockSensorSource();
        session = app.startTrip(source);
        session.setListener(this);
        session.start();

        ((TextView) findViewById(R.id.txt_vehicle)).setText(session.getVehicle().getDisplayName());
        buildScenarioChips();

        findViewById(R.id.btn_end).setOnClickListener(v -> endTrip());

        // O botão "voltar" do celular também encerra (e salva) a viagem.
        // Aqui a classe anônima estende uma CLASSE abstrata (OnBackPressedCallback), não uma interface.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                endTrip();
            }
        });
    }

    /** Cria um botão ("chip") para cada cenário do simulador, menos a pilotagem normal. */
    private void buildScenarioChips() {
        ChipGroup group = findViewById(R.id.group_scenarios);
        // Scenario.values() devolve TODOS os valores do enum, em ordem.
        for (Scenario scenario : Scenario.values()) {
            if (scenario == Scenario.NORMAL_RIDE) continue;   // "continue" pula para o próximo
            Chip chip = new Chip(this);
            chip.setText(scenario.getLabel());
            chip.setOnClickListener(v -> play(scenario));
            group.addView(chip);
        }
    }

    /** Troca as próximas leituras pelas do cenário escolhido. */
    private void play(Scenario scenario) {
        queue.clear();
        queue.addAll(MockSensorSource.generate(scenario, nextTimestamp));
        String expected = getString(scenario.shouldAlert() ? R.string.expected_countdown : R.string.expected_watching);
        txtStatus.setText(getString(R.string.trip_status_simulating, scenario.getLabel(), expected));
    }

    // ---------- Avisos da TripSession (contrato TripSession.Listener) ----------

    /** Atualiza velocidade, barras e os 4 sinais a cada leitura. */
    @Override
    public void onReading(SensorReading r) {
        Vehicle v = session.getVehicle();
        txtSpeed.setText(getString(R.string.speed_value, r.getSpeedKmh()));
        txtImpact.setText(getString(R.string.impact_value, r.getAccelerationG()));
        txtRotation.setText(getString(R.string.rotation_value, r.getRotationDegPerSec()));

        // Barra cheia = 2x o limite do veículo. Ou seja: o MEIO da barra é o limite.
        barImpact.setProgress(percent(r.getAccelerationG(), v.getImpactThresholdG() * 2));
        barRotation.setProgress(percent(r.getRotationDegPerSec(), v.getRotationThresholdDegPerSec() * 2));

        Map<String, Boolean> s = session.activeSignals(r.getTimestampMillis());
        txtSignals.setText(getString(R.string.signals,
                dot(s.get(ImpactDetector.SIGNAL)), dot(s.get(DecelerationDetector.SIGNAL)),
                dot(s.get(RolloverDetector.SIGNAL)), dot(s.get(StillnessDetector.SIGNAL))));
    }

    /** Possível acidente: pausa a simulação e abre a tela "Are you OK?". */
    @Override
    public void onIncident(Incident incident) {
        waitingForAlert = true;
        handler.removeCallbacks(ticker);
        // Não precisamos mandar dados pela Intent: a AlertActivity pega o incidente ativo na sessão.
        startActivity(new Intent(this, AlertActivity.class));
    }

    // ---------- Ciclo de vida ----------

    @Override
    protected void onResume() {
        super.onResume();
        if (waitingForAlert) {
            // Voltou da tela de alerta: limpa os sinais e volta a pilotar normalmente.
            waitingForAlert = false;
            queue.clear();
            session.resumeAfterIncident();
            txtStatus.setText(R.string.trip_status_back);
        }
        handler.post(ticker);   // liga o relógio da simulação
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(ticker);   // desliga o relógio quando a tela sai de vista
    }

    /** Encerra a viagem, salva no banco e volta para a tela inicial. */
    private void endTrip() {
        if (ended) return;
        ended = true;
        Trip trip = app.endTrip();
        Toast.makeText(this, getString(R.string.trip_ended, trip.getMaxSpeedKmh(), trip.getIncidents().size()),
                Toast.LENGTH_LONG).show();
        finish();   // fecha esta tela
    }

    // ---------- Pequenos ajudantes ----------

    /** Converte um valor numa porcentagem de 0 a 100 (para as barras). */
    private static int percent(double value, double max) {
        // Math.round arredonda; (int) é um CAST de long para int; Math.min trava em 100.
        return (int) Math.min(100, Math.round(value / max * 100));
    }

    /** ● se o sinal está ligado, ○ se não. */
    private static String dot(Boolean active) {
        // Boolean.TRUE.equals(x) não quebra se x for null (diferente de x.equals(...)).
        return Boolean.TRUE.equals(active) ? "●" : "○";
    }
}
