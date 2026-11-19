package com.guard.ui.splash;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Intent;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.guard.R;
import com.guard.ui.home.MainActivity;

/**
 * Tela 0 · Abertura animada do Guard (layout: activity_splash.xml).
 *
 * Linha do tempo (em milissegundos):
 *     0 –  450  o logo "salta" para dentro (cresce com um leve exagero e aparece)
 *   350 – 1050  a linha de batimento se desenha no escudo (animação do próprio desenho)
 *   350 – 1250  um pulso de "radar" sai do logo, duas vezes
 *   500 –  900  o nome "Guard" sobe e aparece
 *   850 – 1250  o slogan e o rodapé aparecem
 *          2200  abre a tela inicial (MainActivity)
 *
 * Tocar em qualquer lugar pula a animação.
 */
public class SplashActivity extends AppCompatActivity {

    /** Duração total da abertura antes de ir para o app. */
    private static final long TOTAL_MILLIS = 2200;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean opened;   // evita abrir a tela inicial duas vezes (timer + toque)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView logo = findViewById(R.id.logo);
        View pulse = findViewById(R.id.pulse);
        View brand = findViewById(R.id.txt_brand);
        View slogan = findViewById(R.id.txt_slogan);
        View footer = findViewById(R.id.txt_footer);

        // dp -> pixels: as animações trabalham em pixels, então convertemos pela densidade da tela.
        float dp = getResources().getDisplayMetrics().density;

        // 1) Logo: começa menor e transparente, cresce até o tamanho normal.
        //    view.animate() é o jeito mais simples de animar uma View (encadeando os métodos).
        //    OvershootInterpolator passa um pouco do tamanho final e volta: dá o efeito de "salto".
        logo.setScaleX(0.6f);
        logo.setScaleY(0.6f);
        logo.animate().scaleX(1f).scaleY(1f).alpha(1f)
                .setDuration(450)
                .setInterpolator(new OvershootInterpolator(1.6f))
                .start();

        // 2) Linha de batimento: o desenho é um AnimatedVectorDrawable (avd_guard_logo.xml).
        //    "instanceof" confere o tipo antes do CAST, para não quebrar se o desenho mudar.
        Drawable drawable = logo.getDrawable();
        if (drawable instanceof Animatable) {
            ((Animatable) drawable).start();
        }

        // 3) Pulso de radar: cresce e some, e repete uma vez.
        //    ObjectAnimator anima várias propriedades juntas (PropertyValuesHolder) e permite repetir,
        //    coisa que o view.animate() não faz.
        ObjectAnimator pulseAnim = ObjectAnimator.ofPropertyValuesHolder(pulse,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.6f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.6f),
                PropertyValuesHolder.ofFloat(View.ALPHA, 0.9f, 0f));
        pulseAnim.setDuration(450);
        pulseAnim.setStartDelay(350);
        pulseAnim.setRepeatCount(1);   // toca 2 vezes no total
        pulseAnim.start();

        // 4) Nome "Guard": começa 24 dp mais baixo e sobe enquanto aparece.
        brand.setTranslationY(24 * dp);
        brand.animate().translationY(0f).alpha(1f)
                .setStartDelay(500).setDuration(400)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        // 5) Slogan e rodapé: só aparecem.
        slogan.animate().alpha(1f).setStartDelay(850).setDuration(400).start();
        footer.animate().alpha(1f).setStartDelay(850).setDuration(400).start();

        // Abre o app quando o tempo acabar... ou antes, se tocarem na tela.
        // "this::openApp" é um method reference: "quando chegar a hora, chame openApp()".
        handler.postDelayed(this::openApp, TOTAL_MILLIS);
        findViewById(R.id.root).setOnClickListener(v -> openApp());
    }

    /** Vai para a tela inicial com um fade suave e fecha a abertura (o "voltar" não volta para cá). */
    private void openApp() {
        if (opened) return;
        opened = true;
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    /** Se a tela fechar antes do tempo, cancela o timer pendente. */
    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);   // null = remove TODAS as tarefas agendadas
        super.onDestroy();
    }
}
