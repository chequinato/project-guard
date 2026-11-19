package com.guard.ui.history;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.guard.GuardApplication;
import com.guard.R;
import com.guard.data.TripRecord;
import com.guard.ui.common.Press;
import com.guard.ui.common.VehicleUi;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Tela 5 · "Your trips" (layout: activity_history.xml).
 * Lista as viagens salvas na tabela trips, da mais recente para a mais antiga.
 */
public class HistoryActivity extends AppCompatActivity {

    /**
     * Formato da data, ex.: "Fri, 13 Nov, 15:34".
     * EEE = dia da semana | d = dia | MMM = mês abreviado | HH:mm = hora:minuto (24 h)
     * Criado no onCreate, com o idioma DA TELA: em português sai "sex., 13 nov., 15:34".
     */
    private SimpleDateFormat dateFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Locale screenLocale = getResources().getConfiguration().getLocales().get(0);
        dateFormat = new SimpleDateFormat("EEE, d MMM, HH:mm", screenLocale);
        setContentView(R.layout.activity_history);

        View btnBack = findViewById(R.id.btn_back);
        Press.attach(btnBack);
        btnBack.setOnClickListener(v -> finish());

        List<TripRecord> trips = ((GuardApplication) getApplication()).getTripDao().findAllNewestFirst();
        LinearLayout list = findViewById(R.id.list_trips);
        int totalAlerts = 0;

        for (int i = 0; i < trips.size(); i++) {
            TripRecord trip = trips.get(i);
            View row = LayoutInflater.from(this).inflate(R.layout.item_trip, list, false);

            ((ImageView) row.findViewById(R.id.img_vehicle)).setImageResource(VehicleUi.icon(trip.getVehicleType()));
            ((TextView) row.findViewById(R.id.txt_date)).setText(dateFormat.format(new Date(trip.getStartMillis())));
            ((TextView) row.findViewById(R.id.txt_details)).setText(getString(R.string.trip_details,
                    getString(VehicleUi.label(trip.getVehicleType())),
                    formatDuration(trip.getDurationMillis()),
                    trip.getMaxSpeedKmh()));

            // Etiqueta da direita: laranja com o número de alertas, ou verde "Safe".
            TextView badge = row.findViewById(R.id.txt_alerts);
            boolean hadAlert = trip.getAlertCount() > 0;
            badge.setText(hadAlert ? getString(R.string.trip_alerts, trip.getAlertCount()) : getString(R.string.trip_safe));
            badge.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, hadAlert ? R.color.guard_alert_dark : R.color.guard_ok_dark)));

            row.findViewById(R.id.divider).setVisibility(i == trips.size() - 1 ? View.GONE : View.VISIBLE);
            list.addView(row);
            totalAlerts += trip.getAlertCount();   // "+=" soma e guarda
        }

        ((TextView) findViewById(R.id.txt_summary)).setText(getString(R.string.history_summary, trips.size(), totalAlerts));
        list.setVisibility(trips.isEmpty() ? View.GONE : View.VISIBLE);
        findViewById(R.id.txt_empty).setVisibility(trips.isEmpty() ? View.VISIBLE : View.GONE);
    }

    /** 125000 ms -> "2 min"; 45000 ms -> "45 s". */
    private String formatDuration(long millis) {
        long minutes = millis / 60_000;          // divisão inteira: descarta o resto
        long seconds = (millis / 1000) % 60;     // "%" = resto da divisão
        return minutes > 0
                ? getString(R.string.duration_minutes, (int) minutes)
                : getString(R.string.duration_seconds, (int) seconds);
    }
}
