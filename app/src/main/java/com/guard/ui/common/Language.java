package com.guard.ui.common;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

/**
 * Troca o idioma do app (inglês <-> português) pelo botão da bandeira.
 *
 * Como funciona a tradução no Android:
 * - res/values/strings.xml     -> textos em inglês (o padrão)
 * - res/values-pt/strings.xml  -> os MESMOS nomes, com o texto em português
 * O Android escolhe sozinho a pasta certa conforme o idioma ativo.
 *
 * AppCompatDelegate.setApplicationLocales muda o idioma SÓ deste app (o celular continua igual),
 * recria as telas abertas e lembra a escolha quando o app for aberto de novo
 * (isso é o serviço "autoStoreLocales" declarado no AndroidManifest).
 */
public final class Language {

    private static final String PORTUGUESE = "pt-BR";
    private static final String ENGLISH = "en";
    private static final String PREFS = "guard_language";
    private static final String KEY_INITIALIZED = "initialized";

    private Language() { }   // classe utilitária: só métodos estáticos, ninguém cria objeto

    /**
     * Na PRIMEIRA vez que o app abre, fixamos o inglês (a apresentação é em inglês).
     * Sem isso, um celular em português já abriria o app em português.
     * Depois da primeira vez, quem manda é a bandeira.
     */
    public static void applyDefault(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (prefs.getBoolean(KEY_INITIALIZED, false)) return;
        prefs.edit().putBoolean(KEY_INITIALIZED, true).apply();
        if (AppCompatDelegate.getApplicationLocales().isEmpty()) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(ENGLISH));
        }
    }

    /** A tela está em português agora? (olha o idioma que a tela está usando de fato) */
    public static boolean isPortuguese(Context context) {
        return "pt".equals(context.getResources().getConfiguration().getLocales().get(0).getLanguage());
    }

    /** Português vira inglês e vice-versa. */
    public static void toggle(Context context) {
        String next = isPortuguese(context) ? ENGLISH : PORTUGUESE;
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(next));
    }
}
