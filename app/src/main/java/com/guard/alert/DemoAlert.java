package com.guard.alert;

import com.guard.model.EmergencyContact;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Canal de DEMONSTRAÇÃO: não envia nada de verdade, só guarda o que "teria enviado".
 * Usado nos testes e na apresentação (para ninguém receber SMS de mentira no meio da aula).
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
        return true;
    }

    public List<String> getSentMessages() {
        return Collections.unmodifiableList(sent);
    }
}
