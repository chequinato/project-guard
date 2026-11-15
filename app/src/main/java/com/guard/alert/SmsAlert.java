package com.guard.alert;

import android.content.Context;
import android.os.Build;
import android.telephony.SmsManager;

import com.guard.model.EmergencyContact;

import java.util.ArrayList;

/**
 * Canal real: SMS. Precisa da permissão SEND_SMS concedida pelo usuário.
 * Para usar no app, troque "new DemoAlert()" por "new SmsAlert(this)" na AlertActivity.
 */
public class SmsAlert implements AlertChannel {

    private final Context context;

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
            // A forma de pegar o SmsManager mudou no Android 12 (API 31, versão "S").
            SmsManager sms = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    ? context.getSystemService(SmsManager.class)
                    : SmsManager.getDefault();
            // SMS tem limite de 160 caracteres: divideMessage quebra em partes.
            ArrayList<String> parts = sms.divideMessage(message);
            sms.sendMultipartTextMessage(contact.getPhone(), null, parts, null, null);
            return true;
        } catch (RuntimeException e) {
            // Sem permissão, sem chip, número inválido... avisamos que falhou.
            return false;
        }
    }
}
