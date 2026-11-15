package com.guard.alert;

import com.guard.model.EmergencyContact;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Canal de DEMONSTRAÇÃO: não envia nada de verdade, só anota o que "teria enviado".
 * Usado nos testes e na apresentação, para ninguém receber SMS falso no meio da aula.
 */
public class DemoAlert implements AlertChannel {

    private final List<String> sent = new ArrayList<>();

    @Override
    public String getName() {
        return "Demo";
    }

    @Override
    public boolean send(EmergencyContact contact, String message) {
        sent.add(contact.getName() + " <- " + message);
        return true;   // na demo, o envio "sempre funciona"
    }

    /** Tudo o que foi "enviado" até agora (só leitura). Os testes conferem isto. */
    public List<String> getSentMessages() {
        return Collections.unmodifiableList(sent);
    }
}
