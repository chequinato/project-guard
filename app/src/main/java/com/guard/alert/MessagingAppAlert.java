package com.guard.alert;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.guard.model.EmergencyContact;

/**
 * Canal: app de mensagens (WhatsApp, via link wa.me).
 * Limitação: o Android não deixa um app enviar mensagem por outro sozinho;
 * isto abre a conversa com o texto pronto. Bom para a demo, fraco se a pessoa estiver desacordada.
 * (Ótimo assunto para a parte de "Risks" da apresentação.)
 */
public class MessagingAppAlert implements AlertChannel {

    private final Context context;

    public MessagingAppAlert(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public String getName() {
        return "Messaging app";
    }

    @Override
    public boolean send(EmergencyContact contact, String message) {
        String phone = contact.getPhone().replace("+", "");
        Uri uri = Uri.parse("https://wa.me/" + phone + "?text=" + Uri.encode(message));
        Intent intent = new Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            return false;
        }
    }
}
