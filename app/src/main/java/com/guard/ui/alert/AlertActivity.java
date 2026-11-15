package com.guard.ui.alert;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.guard.GuardApplication;
import com.guard.R;
import com.guard.alert.AlertService;
import com.guard.alert.DemoAlert;
import com.guard.model.EmergencyContact;
import com.guard.model.Incident;
import com.guard.model.User;
import com.guard.session.TripSession;

/**
 * Tela 3 · "Are you OK?" -> alerta enviado -> escalonamento -> fim (layout: activity_alert.xml).
 *
 * O layout tem 3 "painéis" empilhados e só um aparece por vez:
 *   panel_count (contagem) -> panel_alert (alerta enviado) -> panel_end (final)
 *
 * O incidente vem da TripSession: é o MESMO objeto que a viagem guardou.
 * Quando o status muda aqui (cancelado, confirmado...), a viagem também enxerga.
 *
 * Canal de envio: DemoAlert (não manda SMS de verdade). Para enviar SMS real,
 * troque por new SmsAlert(this) e peça a permissão SEND_SMS. Polimorfismo: nada mais muda.
 */
public class AlertActivity extends AppCompatActivity {

    /**
     * Na demo, o escalonamento espera 20 s. Na versão real seria AlertService.ESCALATION_TIMEOUT_MILLIS (2 min).
     */
    private static final long DEMO_ESCALATION_MILLIS = 20_000;

    private User user;
    private Incident incident;
    private AlertService alertService;
    private CountDownTimer countdown;    // relógio dos 30 s
    private CountDownTimer escalation;   // relógio de cada contato

    private View panelCount, panelAlert, panelEnd;
    private CircularProgressIndicator ring;
    private TextView txtSeconds, txtAlertTitle, txtAlertDetail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert);

        TripSession session = ((GuardApplication) getApplication()).getCurrentSession();
        // Proteção: se o Android fechou o app e reabriu direto nesta tela, não há sessão. Saímos.
        if (session == null || session.getActiveIncident() == null) {
            finish();
            return;
        }
        user = session.getUser();
        incident = session.getActiveIncident();
        alertService = new AlertService(new DemoAlert());

        panelCount = findViewById(R.id.panel_count);
        panelAlert = findViewById(R.id.panel_alert);
        panelEnd = findViewById(R.id.panel_end);
        ring = findViewById(R.id.ring);
        txtSeconds = findViewById(R.id.txt_seconds);
        txtAlertTitle = findViewById(R.id.txt_alert_title);
        txtAlertDetail = findViewById(R.id.txt_alert_detail);

        // Quem será avisado primeiro?
        TextView txtWho = findViewById(R.id.txt_who);
        if (user.hasContacts()) {
            EmergencyContact first = user.getContactsByPriority().get(0);
            txtWho.setText(getString(R.string.countdown_who, first.getName()));
        } else {
            txtWho.setText(R.string.countdown_no_contacts);
        }

        // Os dois botões "I'm OK" fazem a mesma coisa.
        findViewById(R.id.btn_ok).setOnClickListener(v -> userIsOk());
        findViewById(R.id.btn_cancel_alert).setOnClickListener(v -> userIsOk());
        findViewById(R.id.btn_confirmed).setOnClickListener(v -> contactConfirmed());
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        startCountdown();
    }

    // ========== Etapa 1: contagem regressiva ==========

    private void startCountdown() {
        // CountDownTimer(duração, intervalo): chama onTick a cada 50 ms e onFinish no fim.
        // Classe anônima estendendo a classe abstrata CountDownTimer.
        countdown = new CountDownTimer(AlertService.COUNTDOWN_MILLIS, 50) {
            @Override
            public void onTick(long left) {
                // (left + 999) / 1000 arredonda para CIMA: 29.4 s aparece como "30".
                // Divisão entre inteiros (long) descarta a parte decimal.
                txtSeconds.setText(String.valueOf((left + 999) / 1000));
                ring.setProgress((int) (left * 1000 / AlertService.COUNTDOWN_MILLIS));
            }

            @Override
            public void onFinish() {
                ring.setProgress(0);
                sendAlert();
            }
        }.start();   // cria E já começa
    }

    // ========== Etapa 2: alerta + escalonamento ==========

    private void sendAlert() {
        if (!user.hasContacts()) {
            showEnd(false, getString(R.string.no_reply), getString(R.string.no_contacts_title),
                    getString(R.string.no_contacts_text));
            return;
        }
        alertService.start(user, incident);
        ((TextView) findViewById(R.id.txt_message)).setText(AlertService.buildMessage(user, incident));
        panelCount.setVisibility(View.GONE);
        panelAlert.setVisibility(View.VISIBLE);
        showCurrentContact();
    }

    /** Mostra quem foi avisado e começa o relógio de espera por resposta. */
    private void showCurrentContact() {
        // "final" numa variável local: ela não muda mais. A classe anônima abaixo só pode
        // usar variáveis locais que sejam final (ou que nunca mudem, "efetivamente final").
        final EmergencyContact current = alertService.getCurrentContact();
        final EmergencyContact next = alertService.getNextContact();
        txtAlertTitle.setText(getString(R.string.alert_title, current.getName()));

        if (escalation != null) escalation.cancel();
        escalation = new CountDownTimer(DEMO_ESCALATION_MILLIS, 1000) {
            @Override
            public void onTick(long left) {
                String sentTo = getString(R.string.alert_sent_to, alertService.getChannelName(), current.getPhone());
                String nextText = next == null
                        ? getString(R.string.alert_last)
                        : getString(R.string.alert_next, (int) ((left + 999) / 1000), next.getName());
                txtAlertDetail.setText(sentTo + "\n" + nextText);   // "\n" = quebra de linha
            }

            @Override
            public void onFinish() {
                // Ninguém respondeu: passa para o próximo. Se acabou a lista, mostra o final.
                if (alertService.escalate()) {
                    showCurrentContact();
                } else {
                    showEnd(false, getString(R.string.no_reply), getString(R.string.nobody_answered),
                            getString(R.string.concept_disclaimer));
                }
            }
        }.start();
    }

    /** Simula a resposta do contato ("estou indo"). */
    private void contactConfirmed() {
        cancelTimers();
        EmergencyContact who = alertService.getCurrentContact();
        alertService.confirm();
        showEnd(true, getString(R.string.help_coming), getString(R.string.contact_on_way, who.getName()),
                getString(R.string.location_updating, who.getName()));
    }

    // ========== Etapa 3: fim ==========

    /** A pessoa está bem: cancela tudo. */
    private void userIsOk() {
        cancelTimers();
        incident.cancelByUser();
        showEnd(false, getString(R.string.alert_cancelled), getString(R.string.glad_ok),
                getString(R.string.no_message_sent));
    }

    /**
     * Mostra o painel final.
     *
     * @param helpComing true = fundo verde (alguém está indo); false = fundo claro
     */
    private void showEnd(boolean helpComing, String header, String title, String text) {
        panelCount.setVisibility(View.GONE);
        panelAlert.setVisibility(View.GONE);
        panelEnd.setVisibility(View.VISIBLE);

        // Cores vêm do colors.xml. ContextCompat.getColor transforma o id na cor de verdade.
        int bg = ContextCompat.getColor(this, helpComing ? R.color.guard_ok_dark : R.color.guard_page);
        int fg = ContextCompat.getColor(this, helpComing ? R.color.white : R.color.guard_ink);
        findViewById(R.id.root).setBackgroundColor(bg);
        getWindow().getDecorView().setBackgroundColor(bg);
        getWindow().setStatusBarColor(bg);   // barra do relógio/bateria com a mesma cor
        // Fundo claro precisa de ícones escuros na barra de status, e vice-versa.
        new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView())
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

    /** A tela foi fechada: para os relógios, senão eles continuariam rodando "no escuro". */
    @Override
    protected void onDestroy() {
        cancelTimers();
        super.onDestroy();
    }
}
