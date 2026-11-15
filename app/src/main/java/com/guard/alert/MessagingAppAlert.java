package com.guard.alert;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.guard.model.EmergencyContact;

/**
 * Canal: app de mensagens (WhatsApp, via link wa.me).
 *
 * Limitação importante: o Android não deixa um app enviar mensagem POR OUTRO app sozinho.
 * Isto só abre a conversa com o texto pronto; alguém precisa tocar em "enviar".
 * Funciona na demo, mas não ajudaria se a pessoa estivesse desacordada.
 * (Bom assunto para a parte de "Risks" da apresentação.)
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
        // Uri.encode troca espaços e acentos por códigos que podem ir dentro de um link.
        Uri uri = Uri.parse("https://wa.me/" + phone + "?text=" + Uri.encode(message));
        // INTENT = um "pedido" para o Android: "abra este link com o app que souber abrir".
        Intent intent = new Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            return false;   // nenhum app instalado sabe abrir o link
        }
    }
}
