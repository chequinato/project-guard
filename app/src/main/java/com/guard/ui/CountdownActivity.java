package com.guard.ui;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.guard.GuardApplication;
import com.guard.R;
import com.guard.alert.AlertService;
import com.guard.alert.DemoAlert;
import com.guard.model.EmergencyContact;
import com.guard.model.Incident;
import com.guard.model.User;

import java.util.Locale;

/**
 * Tela 3 · "Are you OK?" → alerta enviado → escalonamento → fim.
 *
 * Canal de envio: DemoAlert (não manda SMS de verdade). Para enviar SMS real,
 * troque por new SmsAlert(this) e peça a permissão SEND_SMS. Polimorfismo: nada mais muda.
 */
public class CountdownActivity extends AppCompatActivity {

    public static final String EXTRA_TIME = "time";
    public static final String EXTRA_LAT = "lat";
    public static final String EXTRA_LNG = "lng";

    /** Na demo o escalonamento espera 20 s em vez de 2 min (AlertService.ESCALATION_TIMEOUT_MILLIS). */
    private static final long DEMO_ESCALATION_MILLIS = 20_000;

    private User user;
    private Incident incident;
    private AlertService alertService;
    private CountDownTimer countdown;
    private CountDownTimer escalation;

    private View panelCount, panelAlert, panelEnd;
    private CircularProgressIndicator ring;
    private TextView txtSeconds, txtAlertTitle, txtAlertDetail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_Guard_Alert);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_countdown);

        user = ((GuardApplication) getApplication()).loadUser();
        incident = new Incident(
                getIntent().getLongExtra(EXTRA_TIME, System.currentTimeMillis()),
                getIntent().getDoubleExtra(EXTRA_LAT, 0),
                getIntent().getDoubleExtra(EXTRA_LNG, 0),
                user.getSelectedVehicle().getType());
        alertService = new AlertService(new DemoAlert());

        panelCount = findViewById(R.id.panel_count);
        panelAlert = findViewById(R.id.panel_alert);
        panelEnd = findViewById(R.id.panel_end);
        ring = findViewById(R.id.ring);
        txtSeconds = findViewById(R.id.txt_seconds);
        txtAlertTitle = findViewById(R.id.txt_alert_title);
        txtAlertDetail = findViewById(R.id.txt_alert_detail);

        EmergencyContact first = user.hasContacts() ? user.getContactsByPriority().get(0) : null;
        ((TextView) findViewById(R.id.txt_who)).setText(first == null
                ? "No contacts registered."
                : first.getName() + " will get your location when the timer ends.");

        findViewById(R.id.btn_ok).setOnClickListener(v -> userIsOk());
        findViewById(R.id.btn_cancel_alert).setOnClickListener(v -> userIsOk());
        findViewById(R.id.btn_confirmed).setOnClickListener(v -> contactConfirmed());
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        startCountdown();
    }

    // ---------- 1. Contagem regressiva ----------

    private void startCountdown() {
        countdown = new CountDownTimer(AlertService.COUNTDOWN_MILLIS, 50) {
            @Override
            public void onTick(long left) {
                txtSeconds.setText(String.valueOf((left + 999) / 1000));
                ring.setProgress((int) (left * 1000 / AlertService.COUNTDOWN_MILLIS));
            }

            @Override
            public void onFinish() {
                ring.setProgress(0);
                sendAlert();
            }
        }.start();
    }

    // ---------- 2. Alerta + escalonamento ----------

    private void sendAlert() {
        if (!user.hasContacts()) {
            showEnd(false, "No contacts", "Nobody to alert",
                    "Add emergency contacts before your next trip.");
            return;
        }
        alertService.start(user, incident);
        ((TextView) findViewById(R.id.txt_message)).setText(AlertService.buildMessage(user, incident));
        panelCount.setVisibility(View.GONE);
        panelAlert.setVisibility(View.VISIBLE);
        showCurrentContact();
    }

    private void showCurrentContact() {
        EmergencyContact current = alertService.getCurrentContact();
        EmergencyContact next = alertService.getNextContact();
        txtAlertTitle.setText(current.getName() + " has your location");

        if (escalation != null) escalation.cancel();
        escalation = new CountDownTimer(DEMO_ESCALATION_MILLIS, 1000) {
            @Override
            public void onTick(long left) {
                String nextText = next == null
                        ? "Last contact on your list."
                        : String.format(Locale.US, "No reply in %ds? %s is next.", (left + 999) / 1000, next.getName());
                txtAlertDetail.setText("Sent by " + alertService.getChannelName() + " to "
                        + current.getPhone() + ".\n" + nextText);
            }

            @Override
            public void onFinish() {
                if (alertService.escalate()) {
                    showCurrentContact();
                } else {
                    showEnd(false, "No reply", "Nobody answered",
                            "Guard is a concept project. In a real emergency in Brazil, call 192 (SAMU).");
                }
            }
        }.start();
    }

    private void contactConfirmed() {
        cancelTimers();
        EmergencyContact who = alertService.getCurrentContact();
        alertService.confirm();
        showEnd(true, getString(R.string.help_coming), who.getName() + " is on the way",
                "Your location keeps updating for " + who.getName() + ".");
    }

    // ---------- 3. Fim ----------

    private void userIsOk() {
        cancelTimers();
        incident.cancelByUser();
        showEnd(false, "Alert cancelled", getString(R.string.glad_ok), getString(R.string.no_message_sent));
    }

    private void showEnd(boolean helpComing, String header, String title, String text) {
        panelCount.setVisibility(View.GONE);
        panelAlert.setVisibility(View.GONE);
        panelEnd.setVisibility(View.VISIBLE);

        int bg = ContextCompat.getColor(this, helpComing ? R.color.guard_ok_dark : R.color.guard_page);
        int fg = ContextCompat.getColor(this, helpComing ? R.color.white : R.color.guard_ink);
        findViewById(R.id.root).setBackgroundColor(bg);
        getWindow().getDecorView().setBackgroundColor(bg);
        getWindow().setStatusBarColor(bg);
        // Tela clara (cancelado) precisa de ícones escuros na barra de status.
        new androidx.core.view.WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(!helpComing);

        TextView h = findViewById(R.id.txt_end_header);
        TextView t = findViewById(R.id.txt_end_title);
        TextView x = findViewById(R.id.txt_end_text);
        h.setText(header);
        t.setText(title);
        x.setText(text);
        h.setTextColor(fg);
        t.setTextColor(fg);
        x.setTextColor(fg);
    }

    private void cancelTimers() {
        if (countdown != null) countdown.cancel();
        if (escalation != null) escalation.cancel();
    }

    @Override
    protected void onDestroy() {
        cancelTimers();
        super.onDestroy();
    }
}
