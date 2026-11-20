package com.guard.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Testes da validação de celular brasileiro (BrazilianPhone). */
public class BrazilianPhoneTest {

    @Test
    public void acceptsCommonWaysOfTypingTheSameNumber() {
        // Todos viram o mesmo número salvo.
        String expected = "+5511939411585";
        assertEquals(expected, BrazilianPhone.normalize("(11) 93941-1585"));
        assertEquals(expected, BrazilianPhone.normalize("11939411585"));
        assertEquals(expected, BrazilianPhone.normalize("+55 11 93941-1585"));
        assertEquals(expected, BrazilianPhone.normalize("5511939411585"));
        assertEquals(expected, BrazilianPhone.normalize("011 93941-1585"));
        assertNull(BrazilianPhone.check("  11 9 3941 1585  "));
    }

    @Test
    public void rejectsWhatIsOutOfTheStandard() {
        assertEquals(BrazilianPhone.Problem.EMPTY, BrazilianPhone.check("   "));
        assertEquals(BrazilianPhone.Problem.INVALID_CHARACTERS, BrazilianPhone.check("11 9abc-1585"));
        assertEquals(BrazilianPhone.Problem.NOT_BRAZIL, BrazilianPhone.check("+1 415 555 0100"));
        assertEquals(BrazilianPhone.Problem.WRONG_LENGTH, BrazilianPhone.check("1193941"));
        assertEquals(BrazilianPhone.Problem.WRONG_LENGTH, BrazilianPhone.check("119394115850"));
        assertEquals(BrazilianPhone.Problem.INVALID_DDD, BrazilianPhone.check("(20) 93941-1585"));
        assertEquals(BrazilianPhone.Problem.NOT_MOBILE, BrazilianPhone.check("(11) 4521-1234"));    // fixo
        assertEquals(BrazilianPhone.Problem.NOT_MOBILE, BrazilianPhone.check("(11) 83941-1585"));   // sem o 9
    }

    @Test
    public void formatsForTheScreen() {
        assertEquals("(11) 94444-5612", BrazilianPhone.format("+5511944445612"));
        assertEquals("abc", BrazilianPhone.format("abc"));   // fora do padrão: mostra como está
        assertEquals("20", BrazilianPhone.ddd("(20) 93941-1585"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void normalizeRefusesInvalidNumbers() {
        BrazilianPhone.normalize("12345");
    }

    @Test
    public void comparesTheSameNumberInDifferentFormats() {
        assertTrue(BrazilianPhone.sameNumber("+5511939411585", "11939411585"));
        assertTrue(BrazilianPhone.sameNumber("+5511939411585", "011 93941-1585"));
        assertFalse(BrazilianPhone.sameNumber("+5511939411585", "+5511944445612"));
        assertFalse(BrazilianPhone.sameNumber("+5511939411585", "1585"));   // curto demais
        assertFalse(BrazilianPhone.sameNumber(null, "11939411585"));
    }
}
