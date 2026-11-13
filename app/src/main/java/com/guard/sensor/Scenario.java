package com.guard.sensor;

/**
 * Situações que o simulador sabe reproduzir. São as mesmas do site ("Try a situation").
 * Cada uma tem a resposta esperada do Guard, que os testes usam como gabarito.
 */
public enum Scenario {
    NORMAL_RIDE("Normal ride", false),
    CRASH("Real crash", true),
    PHONE_DROPPED("Phone dropped", false),
    HARD_BRAKE("Hard brake", false),
    SPEED_BUMP("Speed bump", false),
    PARKED("Parked, phone handled", false);

    private final String label;
    private final boolean shouldAlert;

    Scenario(String label, boolean shouldAlert) {
        this.label = label;
        this.shouldAlert = shouldAlert;
    }

    public String getLabel() { return label; }

    /** Gabarito: esse cenário deveria iniciar a contagem regressiva? */
    public boolean shouldAlert() { return shouldAlert; }
}
