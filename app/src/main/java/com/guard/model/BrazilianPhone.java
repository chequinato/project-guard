package com.guard.model;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Regras de um telefone CELULAR do Brasil (o Guard manda SMS, então precisa ser celular).
 *
 * Formato aceito (com ou sem espaços, parênteses e traço):
 *     (11) 91234-5678      11912345678      +55 11 91234-5678      011 91234-5678
 * Formato salvo (padrão internacional, sem nada além de números):
 *     +5511912345678
 *
 * Regras:
 *   1. Só números (e os símbolos + ( ) - . e espaço).
 *   2. Se tiver código do país, tem que ser +55 (Brasil).
 *   3. DDD (2 dígitos) + 9 dígitos do celular = 11 dígitos.
 *   4. O DDD precisa existir (lista da Anatel).
 *   5. Celular começa com 9 logo depois do DDD.
 *
 * CONCEITO: classe utilitária (final + construtor privado + métodos static).
 * Não guarda estado: recebe um texto e responde. Java puro, então tem teste no computador.
 */
public final class BrazilianPhone {

    /** O que pode estar errado no número. A tela escolhe a mensagem de erro (e o idioma) por aqui. */
    public enum Problem {
        EMPTY,              // nada digitado
        INVALID_CHARACTERS, // letras ou símbolos estranhos
        NOT_BRAZIL,         // código de país diferente de +55
        WRONG_LENGTH,       // quantidade de dígitos errada
        INVALID_DDD,        // DDD que não existe
        NOT_MOBILE          // telefone fixo (não recebe SMS)
    }

    /** DDDs válidos no Brasil (Anatel). Set = conjunto: responde "contém?" bem rápido. */
    private static final Set<String> DDDS = new HashSet<>(Arrays.asList(
            "11", "12", "13", "14", "15", "16", "17", "18", "19",
            "21", "22", "24", "27", "28",
            "31", "32", "33", "34", "35", "37", "38",
            "41", "42", "43", "44", "45", "46", "47", "48", "49",
            "51", "53", "54", "55",
            "61", "62", "63", "64", "65", "66", "67", "68", "69",
            "71", "73", "74", "75", "77", "79",
            "81", "82", "83", "84", "85", "86", "87", "88", "89",
            "91", "92", "93", "94", "95", "96", "97", "98", "99"));

    private BrazilianPhone() { }

    /**
     * Confere o número.
     * @return null se estiver tudo certo; senão, o problema encontrado.
     */
    public static Problem check(String raw) {
        if (raw == null || raw.trim().isEmpty()) return Problem.EMPTY;
        String text = raw.trim();

        // Regex: só dígitos, espaço, ponto, traço e parênteses; o "+" só pode vir no começo.
        if (!text.matches("\\+?[0-9 ().\\-]+")) return Problem.INVALID_CHARACTERS;

        String digits = text.replaceAll("[^0-9]", "");   // fica só com os números
        String national;                                 // DDD + celular, sem o 55
        if (text.startsWith("+")) {
            if (!digits.startsWith("55")) return Problem.NOT_BRAZIL;
            national = digits.substring(2);
        } else if (digits.length() == 13 && digits.startsWith("55")) {
            national = digits.substring(2);              // digitou 55 sem o "+"
        } else if (digits.length() == 12 && digits.startsWith("0")) {
            national = digits.substring(1);              // "0" de longa distância: 011...
        } else {
            national = digits;
        }

        if (national.length() == 10) return Problem.NOT_MOBILE;   // DDD + 8 dígitos = fixo
        if (national.length() != 11) return Problem.WRONG_LENGTH;
        if (!DDDS.contains(national.substring(0, 2))) return Problem.INVALID_DDD;
        if (national.charAt(2) != '9') return Problem.NOT_MOBILE;
        return null;
    }

    /** O número está certo? */
    public static boolean isValid(String raw) {
        return check(raw) == null;
    }

    /**
     * Converte para o formato salvo: "(11) 91234-5678" -> "+5511912345678".
     * @throws IllegalArgumentException se o número não passar no check().
     */
    public static String normalize(String raw) {
        Problem problem = check(raw);
        if (problem != null) {
            throw new IllegalArgumentException("Invalid phone (" + problem + "): " + raw);
        }
        String digits = raw.replaceAll("[^0-9]", "");
        // Os 11 últimos dígitos são sempre DDD + celular (o que vem antes é 55 ou 0).
        return "+55" + digits.substring(digits.length() - 11);
    }

    /** O DDD digitado (para a mensagem "o DDD 00 não existe"). Vazio se não der para saber. */
    public static String ddd(String raw) {
        if (raw == null) return "";
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.length() >= 11) return digits.substring(digits.length() - 11, digits.length() - 9);
        return digits.length() >= 2 ? digits.substring(0, 2) : "";
    }

    /**
     * Para mostrar na tela: "+5511912345678" -> "(11) 91234-5678".
     * Se o número não estiver no padrão (ex.: um contato antigo), devolve como está.
     */
    public static String format(String phone) {
        if (!isValid(phone)) return phone;
        String n = normalize(phone).substring(3);   // tira o "+55"
        return "(" + n.substring(0, 2) + ") " + n.substring(2, 7) + "-" + n.substring(7);
    }
}
