package com.guard.ui.common;

import android.annotation.SuppressLint;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;

/**
 * Resposta ao toque: o botão "afunda" um pouco ao encostar o dedo e volta com um leve quique,
 * e o celular dá uma vibração curta nas ações importantes.
 *
 * Classe utilitária (só métodos static, construtor privado), como a ContactViews.
 */
public final class Press {

    private static final float PRESSED_SCALE = 0.97f;

    private Press() { }

    /**
     * Liga o efeito de "afundar" em uma ou mais Views.
     *
     * "View... views" = VARARGS: dá para passar quantas Views quiser, separadas por vírgula.
     * Dentro do método, "views" vira um array.
     */
    @SuppressLint("ClickableViewAccessibility")   // retornamos false: o clique normal continua funcionando
    public static void attach(View... views) {
        for (View view : views) {
            view.setOnTouchListener((v, event) -> {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:      // encostou o dedo
                        v.animate().scaleX(PRESSED_SCALE).scaleY(PRESSED_SCALE).setDuration(90).start();
                        break;
                    case MotionEvent.ACTION_UP:        // soltou
                    case MotionEvent.ACTION_CANCEL:    // arrastou para fora
                        v.animate().scaleX(1f).scaleY(1f).setDuration(260)
                                .setInterpolator(new OvershootInterpolator(2.5f)).start();
                        break;
                    default:
                        break;
                }
                return false;   // false = "não consumi o toque": o OnClickListener ainda roda
            });
        }
    }

    /** Vibração curtinha de confirmação (escolher veículo, tocar num botão). */
    public static void tick(View view) {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
    }

    /** Vibração mais forte (acidente detectado, alerta enviado). */
    public static void strong(View view) {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
    }
}
