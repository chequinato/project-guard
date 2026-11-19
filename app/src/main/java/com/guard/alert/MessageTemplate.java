package com.guard.alert;

import com.guard.model.Incident;
import com.guard.model.User;

/**
 * Monta o TEXTO da mensagem de emergência.
 *
 * CONCEITO: interface funcional (só 1 método abstrato)
 * Por ter um método só, dá para criar uma implementação com uma LAMBDA:
 *     MessageTemplate t = (user, incident) -> "Olá " + user.getName();
 *
 * Por que separar isso do AlertService?
 * - O AlertService cuida da REGRA (quem avisar e quando).
 * - O texto depende do IDIOMA do app, que só a camada de tela conhece (strings.xml).
 * Assim a tela entrega um template em português ou inglês, e o AlertService não muda nada.
 */
@FunctionalInterface
public interface MessageTemplate {

    String build(User user, Incident incident);
}
