package com.guard.alert;

import com.guard.model.EmergencyContact;

/**
 * ABSTRAÇÃO + POLIMORFISMO: um jeito de mandar a mensagem de emergência.
 * Para criar um canal novo (e-mail, Telegram...) basta implementar esta interface;
 * o AlertService não muda nada.
 */
public interface AlertChannel {

    String getName();

    /** @return true se a mensagem saiu (ou foi entregue ao app responsável). */
    boolean send(EmergencyContact contact, String message);
}
