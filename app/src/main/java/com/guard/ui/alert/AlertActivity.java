package com.guard.ui.alert;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.guard.GuardApplication;
import com.guard.R;
import com.guard.alert.AlertChannel;
import com.guard.alert.AlertService;
import com.guard.alert.DemoAlert;
import com.guard.alert.MessageTemplate;
import com.guard.alert.SmsAlert;
import com.guard.model.BrazilianPhone;
import com.guard.model.EmergencyContact;
import com.guard.model.Incident;
import com.guard.model.IncidentStatus;
import com.guard.model.User;
import com.guard.sensor.PhoneLocation;
import com.guard.session.TripSession;
import com.guard.ui.common.Press;
import com.guard.ui.common.VehicleUi;

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
 * Canal de envio (POLIMORFISMO): se o usuário deu a permissão de SMS, usamos SmsAlert (SMS de verdade);
 * senão, DemoAlert (não envia nada). O AlertService não sabe qual dos dois recebeu.
 */
public class AlertActivity extends AppCompatActivity {

    /** Na demo o escalonamento espera 10 s (na versão real: AlertService.ESCALATION_TIMEOUT_MILLIS, 1 min). */
    private static final long DEMO_ESCALATION_MILLIS = 10_000;

    private User user;
    private Incident incident;
    private AlertService alertService;
    private CountDownTimer countdown;    // relógio dos 15 s
    private CountDownTimer escalation;   // relógio de cada contato
    private ObjectAnimator haloAnim;     // o halo que pulsa atrás do anel
    private Vibrator vibrator;

    private View panelCount, panelAlert, panelEnd;
    private CircularProgressIndicator ring;
    private TextView txtSeconds, txtAlertTitle, txtAlertDetail, txtSmsStatus;
    private boolean realSms;             // true = SmsAlert (SMS de verdade); false = DemoAlert
    private SmsAlert smsChannel;         // guardado para ler o motivo de um erro de envio
    private View btnSmsApp;              // plano B: abrir o app de mensagens do celular
    private boolean listeningReplies;    // true = estamos lendo os SMS que chegam (respostas)
    private PhoneLocation phoneLocation; // GPS de verdade do celular (vai no link do mapa)
    private TextView txtLocation;        // selo "Localização ao vivo (±15 m)"
    private TextView txtMessage;         // prévia do SMS (atualiza quando a posição melhora)

    /**
     * Escuta os SMS que CHEGAM no celular enquanto o alerta está na tela.
     * Se o remetente é um contato que já foi avisado, isso é a resposta dele:
     * quem confirma é o CONTATO, não a pessoa que sofreu o acidente.
     */
    private final BroadcastReceiver smsReply = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Só vale depois que o alerta foi enviado (e antes de terminar).
            if (incident.getStatus() != IncidentStatus.ALERT_SENT) return;
            // Um SMS comprido chega em pedaços: juntamos o texto de todos.
            SmsMessage[] parts = Telephony.Sms.Intents.getMessagesFromIntent(intent);
            if (parts == null || parts.length == 0) return;
            StringBuilder text = new StringBuilder();
            for (SmsMessage part : parts) text.append(part.getMessageBody());
            EmergencyContact who = alertService.findAlertedContact(parts[0].getOriginatingAddress());
            if (who != null) contactReplied(who, text.toString().trim());
        }
    };

    /**
     * Escuta o aviso do Android "o SMS saiu ou falhou" (ver SmsAlert.ACTION_SENT).
     * getResultCode() == RESULT_OK quer dizer que a operadora aceitou a mensagem.
     */
    private final BroadcastReceiver smsResult = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String who = intent.getStringExtra(SmsAlert.EXTRA_CONTACT);
            String phone = BrazilianPhone.format(intent.getStringExtra(SmsAlert.EXTRA_PHONE));
            int code = getResultCode();
            if (code == Activity.RESULT_OK) {
                setStatus(getString(R.string.sms_sent, who), R.drawable.ic_check, R.color.guard_ok_text);
                Press.tick(txtSmsStatus);
            } else {
                // Mostra o MOTIVO que o Android devolveu, para dar para resolver na hora.
                setStatus(getString(R.string.sms_failed, who, phone, failureReason(code)),
                        R.drawable.ic_alert, R.color.guard_alert_text);
                btnSmsApp.setVisibility(View.VISIBLE);
            }
        }
    };
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
        // Tem permissão de SMS? (pedida na tela inicial, ao tocar em "Start trip")
        realSms = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED;
        smsChannel = realSms ? new SmsAlert(this) : null;
        AlertChannel channel = realSms ? smsChannel : new DemoAlert();
        // O texto do SMS vem do strings.xml, então sai no idioma do app. A lambda É o MessageTemplate.
        MessageTemplate template = (u, i) -> getString(R.string.sms_message, u.getName(),
                getString(VehicleUi.label(u.getSelectedVehicle().getType())).toLowerCase(), i.getMapLink());
        alertService = new AlertService(channel, template);
        if (realSms) {
            // RECEIVER_NOT_EXPORTED: só o nosso próprio app pode mandar esse aviso para cá.
            ContextCompat.registerReceiver(this, smsResult, new IntentFilter(SmsAlert.ACTION_SENT),
                    ContextCompat.RECEIVER_NOT_EXPORTED);
        }
        // Escutar as RESPOSTAS: precisa da permissão de receber SMS (pedida junto com as outras).
        listeningReplies = realSms && ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
                == PackageManager.PERMISSION_GRANTED;
        if (listeningReplies) {
            // SMS_RECEIVED vem do sistema. Exigir BROADCAST_SMS de quem envia garante que é
            // o Android de verdade (nenhum outro app consegue fingir que chegou um SMS).
            ContextCompat.registerReceiver(this, smsReply,
                    new IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION),
                    Manifest.permission.BROADCAST_SMS, null, ContextCompat.RECEIVER_EXPORTED);
        }
        vibrator = getSystemService(Vibrator.class);

        panelCount = findViewById(R.id.panel_count);
        panelAlert = findViewById(R.id.panel_alert);
        panelEnd = findViewById(R.id.panel_end);
        ring = findViewById(R.id.ring);
        txtSeconds = findViewById(R.id.txt_seconds);
        txtAlertTitle = findViewById(R.id.txt_alert_title);
        txtAlertDetail = findViewById(R.id.txt_alert_detail);
        listQueue = findViewById(R.id.list_queue);
        txtSmsStatus = findViewById(R.id.txt_sms_status);
        txtLocation = findViewById(R.id.txt_location);
        txtMessage = findViewById(R.id.txt_message);
        startRealLocation();
        btnSmsApp = findViewById(R.id.btn_sms_app);
        Press.attach(btnSmsApp);
        btnSmsApp.setOnClickListener(v -> openSmsApp());
        ((TextView) findViewById(R.id.txt_channel)).setText(realSms ? R.string.channel_sms : R.string.channel_demo);

        TextView txtWho = findViewById(R.id.txt_who);
        if (user.hasContacts()) {
            txtWho.setText(getString(R.string.countdown_who, user.getContactsByPriority().get(0).getName()));
        } else {
            txtWho.setText(R.string.countdown_no_contacts);
        }

        View btnOk = findViewById(R.id.btn_ok);
        View btnCancel = findViewById(R.id.btn_cancel_alert);
        TextView btnSimulate = findViewById(R.id.btn_confirmed);
        View btnBack = findViewById(R.id.btn_back);
        Press.attach(btnOk, btnCancel, btnSimulate, btnBack);
        btnOk.setOnClickListener(v -> userIsOk());
        btnCancel.setOnClickListener(v -> userIsOk());
        // Com SMS de verdade, a confirmação vem da RESPOSTA do contato (smsReply): não há botão.
        // Sem SMS (modo demonstração), deixamos um botão para SIMULAR a resposta na apresentação.
        btnSimulate.setVisibility(listeningReplies ? View.GONE : View.VISIBLE);
        btnSimulate.setText(R.string.simulate_reply);
        btnSimulate.setOnClickListener(v -> simulateReply());
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
        txtMessage.setText(alertService.getMessage());
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
        showSendStatus(current);

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

    /** Linha de status do envio, embaixo da prévia da mensagem. */
    private void showSendStatus(EmergencyContact current) {
        // O plano B começa escondido; aparece no modo demo ou se o envio falhar.
        btnSmsApp.setVisibility(realSms && alertService.wasLastSendOk() ? View.GONE : View.VISIBLE);
        if (!realSms) {
            setStatus(getString(R.string.sms_demo_status), R.drawable.ic_shield, R.color.guard_muted);
        } else if (alertService.wasLastSendOk()) {
            // "Enviando..." até o Android confirmar (o smsResult troca o texto depois).
            setStatus(getString(R.string.sms_sending, current.getName(), BrazilianPhone.format(current.getPhone())),
                    R.drawable.ic_send, R.color.guard_muted);
        } else {
            // O envio nem saiu do celular (sem chip, SMS bloqueado para o app...).
            String detail = smsChannel.getLastError();
            String reason;
            if (detail == null) {
                reason = getString(R.string.sms_reason_not_sent);
            } else if (detail.startsWith("SecurityException")) {
                // O Android barrou por permissão (normalmente a "Telefone"): dizemos onde liberar.
                reason = getString(R.string.sms_reason_permission);
            } else {
                reason = getString(R.string.sms_reason_exception, detail);
            }
            setStatus(getString(R.string.sms_failed, current.getName(), BrazilianPhone.format(current.getPhone()),
                    reason), R.drawable.ic_alert, R.color.guard_alert_text);
        }
    }

    /**
     * Plano B: abre o app de SMS do celular com o número e o texto já preenchidos.
     * "smsto:" é o endereço que todo app de mensagens entende; "sms_body" é o texto.
     * A pessoa só toca em enviar. (Útil na apresentação se o chip recusar o envio automático.)
     */
    private void openSmsApp() {
        EmergencyContact current = alertService.getCurrentContact();
        if (current == null) return;
        String to = BrazilianPhone.isValid(current.getPhone())
                ? BrazilianPhone.normalize(current.getPhone()) : current.getPhone();
        Intent sms = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + to))
                .putExtra("sms_body", alertService.getMessage());
        try {
            startActivity(sms);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.no_sms_app, Toast.LENGTH_SHORT).show();
        }
    }

    /** Traduz o código de erro do Android (SmsManager.RESULT_ERROR_...) para uma frase. */
    private String failureReason(int code) {
        switch (code) {
            case SmsManager.RESULT_ERROR_GENERIC_FAILURE: return getString(R.string.sms_reason_generic);
            case SmsManager.RESULT_ERROR_RADIO_OFF:       return getString(R.string.sms_reason_radio_off);
            case SmsManager.RESULT_ERROR_NO_SERVICE:      return getString(R.string.sms_reason_no_service);
            case SmsManager.RESULT_ERROR_LIMIT_EXCEEDED:  return getString(R.string.sms_reason_limit);
            default:                                      return getString(R.string.sms_reason_code, code);
        }
    }

    /** Texto do status com um ícone pequeno (16 dp) na frente, pintado na cor pedida. */
    private void setStatus(String text, int iconRes, int colorRes) {
        int size = Math.round(16 * getResources().getDisplayMetrics().density);
        // mutate(): pinta só ESTA cópia do ícone (sem mudar o mesmo ícone nas outras telas).
        Drawable icon = ContextCompat.getDrawable(this, iconRes).mutate();
        icon.setBounds(0, 0, size, size);
        icon.setTint(ContextCompat.getColor(this, colorRes));
        txtSmsStatus.setCompoundDrawablesRelative(icon, null, null, null);   // ícone à esquerda
        txtSmsStatus.setText(text);
        // Entrada suave a cada mudança, para o olho perceber que o status mudou.
        txtSmsStatus.setAlpha(0f);
        txtSmsStatus.animate().alpha(1f).setDuration(220).start();
    }

    // ========== Localização real ==========

    /**
     * Liga o GPS assim que a tela abre: os 15 s da contagem dão tempo de achar uma boa posição
     * antes do primeiro SMS. Cada posição nova substitui a do sensor simulado no incidente,
     * então o link do mapa (e cada SMS seguinte) sai com o lugar de verdade.
     */
    private void startRealLocation() {
        phoneLocation = new PhoneLocation(this);
        boolean on = phoneLocation.start(location -> {
            incident.updateLocation(location.getLatitude(), location.getLongitude());
            int meters = Math.max(1, Math.round(location.getAccuracy()));
            txtLocation.setText(getString(R.string.location_live, meters));
            // Se a prévia do SMS já está na tela, mostra o link novo.
            if (incident.getStatus() != IncidentStatus.COUNTDOWN) txtMessage.setText(alertService.getMessage());
        });
        // Sem permissão ou com a localização desligada: avisamos que o link usa a posição simulada.
        if (!on) txtLocation.setText(R.string.location_simulated);
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

    /** Modo demonstração: finge que o contato atual respondeu (sem texto). */
    private void simulateReply() {
        contactReplied(alertService.getCurrentContact(), "");
    }

    /** O contato respondeu: mostra quem foi e o que ele escreveu. */
    private void contactReplied(EmergencyContact who, String reply) {
        cancelTimers();
        alertService.confirmBy(who);
        vibrate(new long[]{0, 80, 60, 80, 60, 200});   // vibração "boa notícia"
        String text = reply.isEmpty()
                ? getString(R.string.location_updating, who.getName())
                : getString(R.string.contact_replied_text, reply, who.getName());
        showEnd(EndStyle.HELP_COMING, getString(R.string.help_coming),
                getString(R.string.contact_replied_title, who.getName()), text);
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
        //          fundo da tela          ícone               cor do ícone           círculo do ícone
        HELP_COMING(R.color.guard_ok_dark, R.drawable.ic_check, R.color.guard_ok_dark, R.color.white),
        SAFE(R.color.guard_page, R.drawable.ic_shield, R.color.guard_accent, R.color.guard_surface),
        NEUTRAL(R.color.guard_page, R.drawable.ic_alert, R.color.guard_alert_text, R.color.guard_surface);

        final int background, icon, iconColor, circle;

        EndStyle(int background, int icon, int iconColor, int circle) {
            this.background = background;
            this.icon = icon;
            this.iconColor = iconColor;
            this.circle = circle;
        }
    }

    private void showEnd(EndStyle style, String header, String title, String text) {
        cancelTimers();
        View from = panelAlert.getVisibility() == View.VISIBLE ? panelAlert : panelCount;
        switchPanel(from, panelEnd);

        // "dark" = fundo escuro (verde do socorro, ou a página no modo escuro): texto e ícones claros.
        boolean dark = style == EndStyle.HELP_COMING || !getResources().getBoolean(R.bool.guard_light_bars);
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
        icon.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, style.circle)));
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
        if (realSms) unregisterReceiver(smsResult);          // parar de escutar quando a tela fecha
        if (listeningReplies) unregisterReceiver(smsReply);
        if (phoneLocation != null) phoneLocation.stop();     // desliga o GPS (bateria)
        super.onDestroy();
    }
}
