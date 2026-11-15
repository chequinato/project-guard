package com.guard.sensor;

/**
 * Situações que o simulador sabe reproduzir. São as mesmas do site ("Try a situation").
 *
 * CONCEITO: enum com campos, construtor e métodos
 * Um enum pode ser bem mais que uma lista de nomes: cada valor aqui carrega
 * um texto (label) e a resposta esperada do Guard (shouldAlert), que os testes usam como gabarito.
 */
public enum Scenario {

    //           label                    deveria alertar?
    NORMAL_RIDE("Normal ride",            false),
    CRASH("Real crash",                   true),
    PHONE_DROPPED("Phone dropped",        false),
    HARD_BRAKE("Hard brake",              false),
    SPEED_BUMP("Speed bump",              false),
    PARKED("Parked, phone handled",       false);   // ";" encerra a lista de valores

    private final String label;
    private final boolean shouldAlert;

    // Construtor de enum é sempre privado: só os valores acima podem chamá-lo.
    Scenario(String label, boolean shouldAlert) {
        this.label = label;
        this.shouldAlert = shouldAlert;
    }

    /** Texto do botão na tela. */
    public String getLabel() { return label; }

    /** Gabarito: esse cenário deveria iniciar a contagem regressiva? */
    public boolean shouldAlert() { return shouldAlert; }
}
