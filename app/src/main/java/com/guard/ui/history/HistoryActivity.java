package com.guard.ui.history;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.guard.GuardApplication;
import com.guard.R;
import com.guard.data.TripRecord;
import com.guard.model.vehicle.Vehicle;

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
     * Formato da data, ex.: "Fri, 13 Nov · 15:34".
     * EEE = dia da semana | d = dia | MMM = mês abreviado | HH:mm = hora:minuto (24 h)
     */
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, d MMM · HH:mm", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        List<TripRecord> trips = ((GuardApplication) getApplication()).getTripDao().findAllNewestFirst();
        LinearLayout list = findViewById(R.id.list_trips);
        int totalAlerts = 0;

        for (TripRecord trip : trips) {
            View row = LayoutInflater.from(this).inflate(R.layout.item_trip, list, false);

            ((TextView) row.findViewById(R.id.txt_date)).setText(dateFormat.format(new Date(trip.getStartMillis())));
            ((TextView) row.findViewById(R.id.txt_details)).setText(getString(R.string.trip_details,
                    Vehicle.of(trip.getVehicleType()).getDisplayName(),
                    formatDuration(trip.getDurationMillis()),
                    trip.getMaxSpeedKmh()));

            // Etiqueta da direita: laranja "1 alert(s)" ou verde "Safe".
            TextView badge = row.findViewById(R.id.txt_alerts);
            boolean hadAlert = trip.getAlertCount() > 0;
            badge.setText(hadAlert ? getString(R.string.trip_alerts, trip.getAlertCount()) : getString(R.string.trip_safe));
            badge.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, hadAlert ? R.color.guard_alert_dark : R.color.guard_ok_dark)));

            list.addView(row);
            totalAlerts += trip.getAlertCount();   // "+=" soma e guarda: totalAlerts = totalAlerts + ...
        }

        ((TextView) findViewById(R.id.txt_summary)).setText(getString(R.string.history_summary, trips.size(), totalAlerts));
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
