package com.guard.alert;

import android.content.Context;
import android.os.Build;
import android.telephony.SmsManager;

import com.guard.model.EmergencyContact;

import java.util.ArrayList;

/**
 * Canal real: SMS. Precisa da permissão SEND_SMS concedida pelo usuário.
 * A mensagem pode passar de 160 caracteres, por isso divideMessage().
 */
public class SmsAlert implements AlertChannel {

    private final Context context;

    public SmsAlert(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public String getName() {
        return "SMS";
    }

    @Override
    public boolean send(EmergencyContact contact, String message) {
        try {
            SmsManager sms = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    ? context.getSystemService(SmsManager.class)
                    : SmsManager.getDefault();
            ArrayList<String> parts = sms.divideMessage(message);
            sms.sendMultipartTextMessage(contact.getPhone(), null, parts, null, null);
            return true;
        } catch (RuntimeException e) {
            // Sem permissão, sem chip, número inválido...
            return false;
        }
    }
}
