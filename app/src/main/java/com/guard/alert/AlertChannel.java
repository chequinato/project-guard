package com.guard.alert;

import com.guard.model.EmergencyContact;

/**
 * Um jeito de mandar a mensagem de emergência (SMS, WhatsApp, demonstração...).
 *
 * CONCEITOS: abstração + polimorfismo
 * O AlertService só conhece esta interface. Para criar um canal novo (e-mail, Telegram...)
 * basta escrever uma classe que implementa AlertChannel: o AlertService não muda nada.
 */
public interface AlertChannel {

    /** Nome do canal, mostrado na tela ("SMS", "Demo"...). */
    String getName();

    /** @return true se a mensagem saiu (ou foi entregue ao app responsável). */
    boolean send(EmergencyContact contact, String message);
}
