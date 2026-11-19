package com.guard.ui.alert;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.guard.ui.common.Press;

import java.util.List;

/**
 * Tela 3 · "Are you OK?" -> alerta enviado -> escalonamento -> fim (layout: activity_alert.xml).
 *
 * O layout tem 3 "painéis" e só um aparece por vez:
 *   panel_count (contagem) -> panel_alert (alerta enviado) -> panel_end (final)
 *
 * O incidente vem da TripSession: é o MESMO objeto que a viagem guardou.
 * Quando o status muda aqui (cancelado, confirmado...), a viagem também enxerga.
 *
 * Canal de envio: DemoAlert (não manda SMS de verdade). Para enviar SMS real,
 * troque por new SmsAlert(this) e peça a permissão SEND_SMS. Polimorfismo: nada mais muda.
 */
public class AlertActivity extends AppCompatActivity {

    /** Na demo o escalonamento espera 20 s (na versão real: AlertService.ESCALATION_TIMEOUT_MILLIS, 2 min). */
    private static final long DEMO_ESCALATION_MILLIS = 20_000;

    private User user;
    private Incident incident;
    private AlertService alertService;
    private CountDownTimer countdown;    // relógio dos 30 s
    private CountDownTimer escalation;   // relógio de cada contato
    private ObjectAnimator haloAnim;     // o halo que pulsa atrás do anel
    private Vibrator vibrator;

    private View panelCount, panelAlert, panelEnd;
    private CircularProgressIndicator ring;
    private TextView txtSeconds, txtAlertTitle, txtAlertDetail;
    private LinearLayout listQueue;

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
        vibrator = getSystemService(Vibrator.class);

        panelCount = findViewById(R.id.panel_count);
        panelAlert = findViewById(R.id.panel_alert);
        panelEnd = findViewById(R.id.panel_end);
        ring = findViewById(R.id.ring);
        txtSeconds = findViewById(R.id.txt_seconds);
        txtAlertTitle = findViewById(R.id.txt_alert_title);
        txtAlertDetail = findViewById(R.id.txt_alert_detail);
        listQueue = findViewById(R.id.list_queue);

        TextView txtWho = findViewById(R.id.txt_who);
        if (user.hasContacts()) {
            txtWho.setText(getString(R.string.countdown_who, user.getContactsByPriority().get(0).getName()));
        } else {
            txtWho.setText(R.string.countdown_no_contacts);
        }

        View btnOk = findViewById(R.id.btn_ok);
        View btnCancel = findViewById(R.id.btn_cancel_alert);
        View btnConfirmed = findViewById(R.id.btn_confirmed);
        View btnBack = findViewById(R.id.btn_back);
        Press.attach(btnOk, btnCancel, btnConfirmed, btnBack);
        btnOk.setOnClickListener(v -> userIsOk());
        btnCancel.setOnClickListener(v -> userIsOk());
        btnConfirmed.setOnClickListener(v -> contactConfirmed());
        btnBack.setOnClickListener(v -> finish());

        vibrate(new long[]{0, 400, 200, 400});   // alarme: duas vibrações longas
        startHalo();
        startCountdown();
    }

    // ========== Etapa 1: contagem regressiva ==========

    /** Halo atrás do anel: cresce e some, sem parar, como um sinal de alarme. */
    private void startHalo() {
        haloAnim = ObjectAnimator.ofPropertyValuesHolder(findViewById(R.id.halo),
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.22f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.22f),
                PropertyValuesHolder.ofFloat(View.ALPHA, 0.9f, 0f));
        haloAnim.setDuration(1000);
        haloAnim.setRepeatCount(ValueAnimator.INFINITE);
        haloAnim.start();
    }

    private void startCountdown() {
        // CountDownTimer(duração, intervalo): chama onTick a cada 50 ms e onFinish no fim.
        countdown = new CountDownTimer(AlertService.COUNTDOWN_MILLIS, 50) {
            private long lastSecond = -1;

            @Override
            public void onTick(long left) {
                long second = (left + 999) / 1000;   // arredonda para CIMA: 29,4 s vira "30"
                ring.setProgress((int) (left * 1000 / AlertService.COUNTDOWN_MILLIS));
                if (second != lastSecond) {
                    lastSecond = second;
                    txtSeconds.setText(String.valueOf(second));
                    // Pulinho no número a cada segundo; nos últimos 5, também uma vibração curta.
                    txtSeconds.setScaleX(1.08f);
                    txtSeconds.setScaleY(1.08f);
                    txtSeconds.animate().scaleX(1f).scaleY(1f).setDuration(300).start();
                    if (second <= 5) vibrate(new long[]{0, 60});
                }
            }

            @Override
            public void onFinish() {
                ring.setProgress(0);
                sendAlert();
            }
        }.start();
    }

    // ========== Etapa 2: alerta + escalonamento ==========

    private void sendAlert() {
        if (haloAnim != null) haloAnim.cancel();
        if (!user.hasContacts()) {
            showEnd(EndStyle.NEUTRAL, getString(R.string.no_reply), getString(R.string.no_contacts_title),
                    getString(R.string.no_contacts_text));
            return;
        }
        alertService.start(user, incident);
        vibrate(new long[]{0, 120, 80, 120});
        ((TextView) findViewById(R.id.txt_message)).setText(AlertService.buildMessage(user, incident));
        switchPanel(panelCount, panelAlert);
        showCurrentContact();
    }

    /** Mostra quem foi avisado, atualiza a fila e começa o relógio de espera por resposta. */
    private void showCurrentContact() {
        // "final": a classe anônima abaixo só pode usar variáveis locais que não mudam.
        final EmergencyContact current = alertService.getCurrentContact();
        final EmergencyContact next = alertService.getNextContact();
        txtAlertTitle.setText(getString(R.string.alert_title, current.getName()));
        renderQueue(current);

        if (escalation != null) escalation.cancel();
        escalation = new CountDownTimer(DEMO_ESCALATION_MILLIS, 1000) {
            @Override
            public void onTick(long left) {
                txtAlertDetail.setText(next == null
                        ? getString(R.string.alert_last)
                        : getString(R.string.alert_next, (int) ((left + 999) / 1000), next.getName()));
            }

            @Override
            public void onFinish() {
                // Ninguém respondeu: passa para o próximo. Se acabou a lista, mostra o final.
                if (alertService.escalate()) {
                    Press.strong(txtAlertTitle);
                    showCurrentContact();
                } else {
                    showEnd(EndStyle.NEUTRAL, getString(R.string.no_reply), getString(R.string.nobody_answered),
                            getString(R.string.concept_disclaimer));
                }
            }
        }.start();
    }

    /** Desenha a fila: quem já foi avisado ("Alerted"), o próximo ("Next") e os outros ("Waiting"). */
    private void renderQueue(EmergencyContact current) {
        listQueue.removeAllViews();
        List<EmergencyContact> contacts = user.getContactsByPriority();
        int currentIndex = contacts.indexOf(current);
        for (int i = 0; i < contacts.size(); i++) {
            View row = LayoutInflater.from(this).inflate(R.layout.item_queue, listQueue, false);
            ((TextView) row.findViewById(R.id.txt_priority)).setText(String.valueOf(contacts.get(i).getPriority()));
            ((TextView) row.findViewById(R.id.txt_name)).setText(contacts.get(i).getName());

            TextView state = row.findViewById(R.id.txt_state);
            if (i <= currentIndex) {
                state.setText(R.string.queue_sent);
                state.setTextColor(ContextCompat.getColor(this, R.color.white));
            } else if (i == currentIndex + 1) {
                state.setText(R.string.queue_next);
            } else {
                state.setText(R.string.queue_waiting);
                row.setAlpha(0.6f);
            }
            listQueue.addView(row);
        }
    }

    /** Simula a resposta do contato ("estou indo"). */
    private void contactConfirmed() {
        cancelTimers();
        EmergencyContact who = alertService.getCurrentContact();
        alertService.confirm();
        showEnd(EndStyle.HELP_COMING, getString(R.string.help_coming), getString(R.string.contact_on_way, who.getName()),
                getString(R.string.location_updating, who.getName()));
    }

    // ========== Etapa 3: fim ==========

    /** A pessoa está bem: cancela tudo. */
    private void userIsOk() {
        cancelTimers();
        incident.cancelByUser();
        showEnd(EndStyle.SAFE, getString(R.string.alert_cancelled), getString(R.string.glad_ok),
                getString(R.string.no_message_sent));
    }

    /**
     * ENUM ANINHADO: os 3 jeitos de terminar, cada um com cor de fundo, ícone e cor do ícone.
     * Fica aqui dentro porque só esta tela usa.
     */
    private enum EndStyle {
        HELP_COMING(R.color.guard_ok_dark, R.drawable.ic_check, R.color.guard_ok_dark),
        SAFE(R.color.guard_page, R.drawable.ic_shield, R.color.guard_blue),
        NEUTRAL(R.color.guard_page, R.drawable.ic_alert, R.color.guard_alert_dark);

        final int background, icon, iconColor;

        EndStyle(int background, int icon, int iconColor) {
            this.background = background;
            this.icon = icon;
            this.iconColor = iconColor;
        }
    }

    private void showEnd(EndStyle style, String header, String title, String text) {
        cancelTimers();
        View from = panelAlert.getVisibility() == View.VISIBLE ? panelAlert : panelCount;
        switchPanel(from, panelEnd);

        boolean dark = style == EndStyle.HELP_COMING;
        int bg = ContextCompat.getColor(this, style.background);
        int fg = ContextCompat.getColor(this, dark ? R.color.white : R.color.guard_ink);
        findViewById(R.id.root).setBackgroundColor(bg);
        getWindow().getDecorView().setBackgroundColor(bg);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        // Fundo claro precisa de ícones escuros na barra de status, e vice-versa.
        WindowInsetsControllerCompat bars = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        bars.setAppearanceLightStatusBars(!dark);
        bars.setAppearanceLightNavigationBars(!dark);

        // Ícone dentro do círculo branco, entrando com um "pop".
        ImageView icon = findViewById(R.id.img_end);
        icon.setImageResource(style.icon);
        icon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, style.iconColor)));
        icon.setScaleX(0.4f);
        icon.setScaleY(0.4f);
        icon.animate().scaleX(1f).scaleY(1f).setStartDelay(120).setDuration(420)
                .setInterpolator(new OvershootInterpolator(2.2f)).start();
        Press.tick(icon);

        TextView h = findViewById(R.id.txt_end_header);
        TextView t = findViewById(R.id.txt_end_title);
        TextView x = findViewById(R.id.txt_end_text);
        h.setText(header);
        t.setText(title);
        x.setText(text);
        h.setTextColor(ContextCompat.getColor(this, dark ? R.color.white_80 : R.color.guard_muted));
        t.setTextColor(fg);
        x.setTextColor(ContextCompat.getColor(this, dark ? R.color.white_80 : R.color.guard_muted));
    }

    /** Troca de painel com um fade rápido (o novo entra subindo um pouquinho). */
    private void switchPanel(View from, View to) {
        from.animate().alpha(0f).setDuration(160).withEndAction(() -> from.setVisibility(View.GONE)).start();
        to.setAlpha(0f);
        to.setTranslationY(24f);
        to.setVisibility(View.VISIBLE);
        to.animate().alpha(1f).translationY(0f).setStartDelay(120).setDuration(280).start();
    }

    /** Vibra seguindo um padrão: {espera, vibra, espera, vibra...} em milissegundos. */
    private void vibrate(long[] pattern) {
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));   // -1 = não repetir
        }
    }

    private void cancelTimers() {
        if (countdown != null) countdown.cancel();
        if (escalation != null) escalation.cancel();
        if (haloAnim != null) haloAnim.cancel();
    }

    /** A tela foi fechada: para os relógios, senão eles continuariam rodando "no escuro". */
    @Override
    protected void onDestroy() {
        cancelTimers();
        super.onDestroy();
    }
}
