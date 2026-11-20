package com.guard.ui.splash;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Intent;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.guard.R;
import com.guard.ui.home.MainActivity;

/**
 * Tela 0 · Abertura animada do Guard (layout: activity_splash.xml).
 *
 * Linha do tempo (milissegundos):
 *      0 – 1660  o logo OFICIAL se monta: base do escudo sobe, os corpos sobem e as
 *                três cabeças "pulam" uma de cada vez (animação em avd_guard_splash.xml)
 *   1500 – 2300  um pulso de "radar" sai do logo
 *   1450 – 2070  o nome "guard" sobe e aparece
 *   1900 – 2550  o slogan aparece
 *   2550 – 3400  pausa: a marca inteira fica na tela por quase 1 segundo
 *          3400  um círculo claro cresce a partir do logo e revela a tela inicial
 *
 * Tocar em qualquer lugar pula direto para o final.
 */
public class SplashActivity extends AppCompatActivity {

    private static final long REVEAL_AT_MILLIS = 3400;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean leaving;   // evita começar a saída duas vezes (timer + toque)
    private ImageView logo;
    private View reveal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        logo = findViewById(R.id.logo);
        reveal = findViewById(R.id.reveal);
        View pulse = findViewById(R.id.pulse);
        View brand = findViewById(R.id.txt_brand);
        View slogan = findViewById(R.id.txt_slogan);
        float dp = getResources().getDisplayMetrics().density;   // para converter dp em pixels

        // 1) O logo se monta: o desenho é um AnimatedVectorDrawable.
        //    "instanceof" confere o tipo antes do CAST, para não quebrar se o desenho mudar.
        Drawable drawable = logo.getDrawable();
        if (drawable instanceof Animatable) {
            ((Animatable) drawable).start();
        }

        // 2) Pulso de radar quando a última cabeça aparece.
        ObjectAnimator pulseAnim = ObjectAnimator.ofPropertyValuesHolder(pulse,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 0.8f, 1.7f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.8f, 1.7f),
                PropertyValuesHolder.ofFloat(View.ALPHA, 0.8f, 0f));
        pulseAnim.setDuration(800);
        pulseAnim.setStartDelay(1500);
        pulseAnim.start();

        // 3) Nome "guard": começa 20 dp mais baixo e sobe enquanto aparece.
        brand.setTranslationY(20 * dp);
        brand.animate().translationY(0f).alpha(1f).setStartDelay(1450).setDuration(620)
                .setInterpolator(new DecelerateInterpolator(1.6f)).start();

        // 4) Slogan aparece logo depois.
        slogan.setTranslationY(10 * dp);
        slogan.animate().translationY(0f).alpha(0.92f).setStartDelay(1900).setDuration(650)
                .setInterpolator(new DecelerateInterpolator(1.4f)).start();

        // "this::revealApp" = method reference: "quando chegar a hora, chame revealApp()".
        handler.postDelayed(this::revealApp, REVEAL_AT_MILLIS);
        findViewById(R.id.root).setOnClickListener(v -> revealApp());
    }

    /**
     * Revelação circular: a camada clara cresce em círculo a partir do centro do logo
     * até cobrir a tela; aí abrimos a tela inicial, que tem a mesma cor de fundo.
     */
    private void revealApp() {
        if (leaving) return;
        leaving = true;
        handler.removeCallbacksAndMessages(null);

        // Centro do logo, em coordenadas da tela.
        int[] logoPos = new int[2];
        logo.getLocationInWindow(logoPos);
        int cx = logoPos[0] + logo.getWidth() / 2;
        int cy = logoPos[1] + logo.getHeight() / 2;
        // Raio final: a distância até o canto mais longe (Math.hypot = hipotenusa).
        float radius = (float) Math.hypot(Math.max(cx, reveal.getWidth() - cx), Math.max(cy, reveal.getHeight() - cy));

        Animator anim = ViewAnimationUtils.createCircularReveal(reveal, cx, cy, 0f, radius);
        anim.setDuration(560);
        anim.setInterpolator(new DecelerateInterpolator());
        // AnimatorListenerAdapter: classe com todos os métodos vazios; sobrescrevemos só o que interessa.
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                openHome();
            }
        });
        reveal.setVisibility(View.VISIBLE);
        anim.start();
    }

    /** Abre a tela inicial com um fade rápido e fecha a abertura (o "voltar" não volta para cá). */
    private void openHome() {
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(R.anim.fade_in_fast, 0);
        finish();
    }

    /** Se a tela fechar antes do tempo, cancela o que estava agendado. */
    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);   // null = remove TODAS as tarefas agendadas
        super.onDestroy();
    }
}
