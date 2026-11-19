package com.guard.model;

/**
 * Em que etapa está um possível acidente. Usado pela classe Incident.
 */
public enum IncidentStatus {
    COUNTDOWN,            // tela "Are you OK?" com a contagem de 15 s
    CANCELLED_BY_USER,    // a pessoa apertou "I'm OK"
    ALERT_SENT,           // a contagem zerou e os contatos estão sendo avisados
    CONFIRMED_BY_CONTACT, // um contato respondeu que está indo
    NO_RESPONSE           // ninguém da lista respondeu
}
