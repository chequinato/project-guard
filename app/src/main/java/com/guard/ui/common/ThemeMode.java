package com.guard.ui.common;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Modo claro / escuro (botão da lua na tela inicial).
 *
 * Como funciona:
 * - O tema do app é "DayNight": no escuro, o Android usa as cores de res/values-night/
 *   no lugar das de res/values/. Nenhuma tela precisa saber disso.
 * - AppCompatDelegate.setDefaultNightMode liga/desliga e recria as telas abertas.
 * - A escolha fica salva (SharedPreferences) e é aplicada toda vez que o app abre.
 */
public final class ThemeMode {

    private static final String PREFS = "guard_theme";
    private static final String KEY_DARK = "dark";

    private ThemeMode() { }

    /** Chamado na abertura do app (GuardApplication): aplica o que foi escolhido da última vez. */
    public static void apply(Context context) {
        boolean dark = prefs(context).getBoolean(KEY_DARK, false);   // padrão: claro
        AppCompatDelegate.setDefaultNightMode(dark
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO);
    }

    /** A tela está no modo escuro agora? */
    public static boolean isDark(Context context) {
        int mode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return mode == Configuration.UI_MODE_NIGHT_YES;
    }

    /** Claro vira escuro e vice-versa (salva e aplica na hora). */
    public static void toggle(Context context) {
        prefs(context).edit().putBoolean(KEY_DARK, !isDark(context)).apply();
        apply(context);
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
