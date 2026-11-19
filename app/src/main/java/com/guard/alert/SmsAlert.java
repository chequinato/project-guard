package com.guard.alert;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.telephony.SmsManager;

import com.guard.model.EmergencyContact;

import java.util.ArrayList;

/**
 * Canal REAL: manda SMS pelo chip do celular. Precisa da permissão SEND_SMS concedida.
 * A AlertActivity usa este canal quando a permissão foi dada; senão, usa o DemoAlert.
 *
 * Retorno do envio: o SMS não sai na hora. O Android avisa depois se deu certo, mandando
 * um "broadcast" com a ação ACTION_SENT. Quem quiser saber o resultado (a tela de alerta)
 * escuta essa ação. O nome do contato vai junto no extra EXTRA_CONTACT.
 */
public class SmsAlert implements AlertChannel {

    /** Ação do aviso "o SMS saiu (ou falhou)". */
    public static final String ACTION_SENT = "com.guard.SMS_SENT";
    /** Extra com o nome do contato que recebeu. */
    public static final String EXTRA_CONTACT = "contact";

    private final Context context;
    private int requestCode = 0;   // um número diferente por envio, para os avisos não se misturarem

    public SmsAlert(Context context) {
        // getApplicationContext(): guardamos o contexto do APP, não o da tela.
        // Se guardássemos a tela (Activity), ela não poderia ser liberada da memória ao fechar.
        this.context = context.getApplicationContext();
    }

    @Override
    public String getName() {
        return "SMS";
    }

    @Override
    public boolean send(EmergencyContact contact, String message) {
        // TRY/CATCH: "tenta fazer; se der erro, trata aqui em vez de fechar o app".
        try {
            SmsManager sms = smsManager();
            // SMS tem limite de 160 caracteres: divideMessage quebra em partes.
            ArrayList<String> parts = sms.divideMessage(message);

            // PendingIntent = um Intent "guardado" que o SISTEMA dispara depois, em nome do app.
            // setPackage: só o nosso app recebe esse aviso.
            Intent sent = new Intent(ACTION_SENT)
                    .setPackage(context.getPackageName())
                    .putExtra(EXTRA_CONTACT, contact.getName());
            PendingIntent onSent = PendingIntent.getBroadcast(context, requestCode++, sent,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            // Um aviso por parte da mensagem (a lista precisa ter o mesmo tamanho de "parts").
            ArrayList<PendingIntent> sentIntents = new ArrayList<>();
            for (int i = 0; i < parts.size(); i++) sentIntents.add(onSent);

            sms.sendMultipartTextMessage(contact.getPhone(), null, parts, sentIntents, null);
            return true;
        } catch (RuntimeException e) {
            // Sem permissão, sem chip, número inválido... avisamos que falhou.
            return false;
        }
    }

    /** A forma de pegar o SmsManager mudou no Android 12 (API 31, versão "S"). */
    @SuppressWarnings("deprecation")   // getDefault() é antigo, mas é o certo antes do Android 12
    private SmsManager smsManager() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SmsManager manager = context.getSystemService(SmsManager.class);
            if (manager != null) return manager;
        }
        return SmsManager.getDefault();
    }
}
