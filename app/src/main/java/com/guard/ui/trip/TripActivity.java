package com.guard.ui.trip;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

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
import com.guard.ui.common.Press;
import com.guard.ui.common.ScenarioUi;
import com.guard.ui.common.VehicleUi;

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

    /** Cada leitura simulada vale 500 ms; tocamos a cada 250 ms (2x mais rápido) para a demo não arrastar. */
    private static final long TICK_MILLIS = 250;

    // HANDLER: agenda tarefas para rodar mais tarde na "thread principal" (a que mexe na tela).
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Deque<SensorReading> queue = new ArrayDeque<>();   // leituras que ainda vão tocar

    private GuardApplication app;
    private TripSession session;
    private MockSensorSource source;
    private long nextTimestamp;          // horário simulado da próxima leitura
    private boolean waitingForAlert;     // true enquanto a tela "Are you OK?" está aberta
    private boolean ended;               // evita encerrar (e salvar) a viagem duas vezes
    private float shownSpeed;            // velocidade que está desenhada agora (para animar até a nova)
    private ValueAnimator speedAnim;

    private TextView txtSpeed, txtImpact, txtRotation, txtStatus;
    private TextView sigImpact, sigDrop, sigRotation, sigStill;
    private LinearProgressIndicator barImpact, barRotation;

    /**
     * O "relógio" da simulação: a cada 250 ms entrega uma leitura.
     * CLASSE ANÔNIMA: "new Runnable() { ... }" cria ali mesmo um objeto que implementa Runnable.
     */
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (queue.isEmpty()) {   // acabou o cenário? volta para a pilotagem normal
                queue.addAll(MockSensorSource.generate(Scenario.NORMAL_RIDE, nextTimestamp));
            }
            SensorReading reading = queue.poll();
            nextTimestamp = reading.getTimestampMillis() + MockSensorSource.STEP_MILLIS;
            source.push(reading);    // entrega para os ouvintes (Observer)
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
        txtStatus = findViewById(R.id.txt_status);
        barImpact = findViewById(R.id.bar_impact);
        barRotation = findViewById(R.id.bar_rotation);
        sigImpact = findViewById(R.id.sig_impact);
        sigDrop = findViewById(R.id.sig_drop);
        sigRotation = findViewById(R.id.sig_rotation);
        sigStill = findViewById(R.id.sig_still);

        // Começa a viagem com o sensor SIMULADO. "this" = esta tela recebe os avisos da sessão.
        source = new MockSensorSource();
        session = app.startTrip(source);
        session.setListener(this);
        session.start();

        // Etiqueta do veículo com o ícone na frente do nome.
        TextView txtVehicle = findViewById(R.id.txt_vehicle);
        txtVehicle.setText(VehicleUi.label(session.getVehicle().getType()));
        txtVehicle.setCompoundDrawablesRelativeWithIntrinsicBounds(VehicleUi.icon(session.getVehicle().getType()), 0, 0, 0);

        startLiveDot();
        setupScenarioButtons();

        View btnEnd = findViewById(R.id.btn_end);
        Press.attach(btnEnd);
        btnEnd.setOnClickListener(v -> endTrip());

        // O botão "voltar" do celular também encerra (e salva) a viagem.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                endTrip();
            }
        });
    }

    /** A bolinha verde "ao vivo" pisca devagar, sem parar. */
    private void startLiveDot() {
        ObjectAnimator blink = ObjectAnimator.ofFloat(findViewById(R.id.live_dot), View.ALPHA, 1f, 0.25f);
        blink.setDuration(900);
        blink.setRepeatMode(ValueAnimator.REVERSE);
        blink.setRepeatCount(ValueAnimator.INFINITE);
        blink.start();
    }

    /**
     * Liga os botões do simulador aos cenários.
     * "Real crash" fica sozinho em destaque (é o único que deve disparar o alerta);
     * os outros 4 são alarmes falsos, em grade 2x2 (layout: activity_trip.xml).
     */
    private void setupScenarioButtons() {
        bindScenario(R.id.scn_crash, Scenario.CRASH);
        bindScenario(R.id.scn_dropped, Scenario.PHONE_DROPPED);
        bindScenario(R.id.scn_brake, Scenario.HARD_BRAKE);
        bindScenario(R.id.scn_bump, Scenario.SPEED_BUMP);
        bindScenario(R.id.scn_parked, Scenario.PARKED);
    }

    /** Põe o nome traduzido do cenário no botão e o que acontece ao tocar. */
    private void bindScenario(int viewId, Scenario scenario) {
        TextView button = findViewById(viewId);
        button.setText(ScenarioUi.label(scenario));
        Press.attach(button);   // afunda ao tocar, como os outros botões do app
        button.setOnClickListener(v -> {
            Press.tick(v);
            play(scenario);
        });
    }

    /** Troca as próximas leituras pelas do cenário escolhido. */
    private void play(Scenario scenario) {
        queue.clear();
        queue.addAll(MockSensorSource.generate(scenario, nextTimestamp));
        String expected = getString(scenario.shouldAlert() ? R.string.expected_countdown : R.string.expected_watching);
        showStatus(getString(R.string.trip_status_simulating, getString(ScenarioUi.label(scenario)).toLowerCase(), expected));
    }

    /** Troca o texto do balão com uma entrada rápida, para o olho perceber a resposta. */
    private void showStatus(String text) {
        txtStatus.setText(text);
        txtStatus.setAlpha(0f);
        txtStatus.setTranslationY(8f);
        txtStatus.animate().alpha(1f).translationY(0f).setDuration(220).start();
    }

    // ---------- Avisos da TripSession (contrato TripSession.Listener) ----------

    /** Atualiza velocidade, barras e os 4 sinais a cada leitura. */
    @Override
    public void onReading(SensorReading r) {
        Vehicle v = session.getVehicle();
        animateSpeed((float) r.getSpeedKmh());
        txtImpact.setText(getString(R.string.impact_value, r.getAccelerationG()));
        txtRotation.setText(getString(R.string.rotation_value, r.getRotationDegPerSec()));

        // Barra cheia = 2x o limite do veículo. Ou seja: o MEIO da barra é o limite.
        // O "true" no fim anima a barra até o novo valor.
        barImpact.setProgressCompat(percent(r.getAccelerationG(), v.getImpactThresholdG() * 2), true);
        barRotation.setProgressCompat(percent(r.getRotationDegPerSec(), v.getRotationThresholdDegPerSec() * 2), true);

        Map<String, Boolean> s = session.activeSignals(r.getTimestampMillis());
        setSignal(sigImpact, s.get(ImpactDetector.SIGNAL));
        setSignal(sigDrop, s.get(DecelerationDetector.SIGNAL));
        setSignal(sigRotation, s.get(RolloverDetector.SIGNAL));
        setSignal(sigStill, s.get(StillnessDetector.SIGNAL));
    }

    /** Possível acidente: vibra, pausa a simulação e abre a tela "Are you OK?". */
    @Override
    public void onIncident(Incident incident) {
        waitingForAlert = true;
        handler.removeCallbacks(ticker);
        Press.strong(txtSpeed);
        // A AlertActivity pega o incidente ativo direto na sessão: não precisa mandar nada pela Intent.
        startActivity(new Intent(this, AlertActivity.class));
    }

    // ---------- Pequenas animações ----------

    /** O número da velocidade "rola" até o novo valor em vez de pular. */
    private void animateSpeed(float target) {
        if (speedAnim != null) speedAnim.cancel();
        speedAnim = ValueAnimator.ofFloat(shownSpeed, target);
        speedAnim.setDuration(TICK_MILLIS);
        speedAnim.addUpdateListener(a -> {
            shownSpeed = (float) a.getAnimatedValue();
            txtSpeed.setText(getString(R.string.speed_value, shownSpeed));
        });
        speedAnim.start();
    }

    /** Liga/desliga um sinal. Quando ele ACENDE, dá um pulinho para chamar atenção. */
    private void setSignal(TextView chip, Boolean active) {
        boolean on = Boolean.TRUE.equals(active);   // não quebra se vier null
        if (on && !chip.isActivated()) {
            chip.setScaleX(0.9f);
            chip.setScaleY(0.9f);
            chip.animate().scaleX(1f).scaleY(1f).setDuration(240).start();
        }
        chip.setActivated(on);   // o fundo e a cor mudam sozinhos (bg_signal.xml e color/signal_fg.xml)
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
            showStatus(getString(R.string.trip_status_back));
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
        finish();
    }

    /** Converte um valor numa porcentagem de 0 a 100 (para as barras). */
    private static int percent(double value, double max) {
        return (int) Math.min(100, Math.round(value / max * 100));
    }
}
