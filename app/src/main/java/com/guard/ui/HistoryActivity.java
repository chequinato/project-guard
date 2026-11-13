package com.guard.ui;

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
import com.guard.model.Vehicle;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Tela 5 · "Your trips": lista as viagens salvas na tabela trips. */
public class HistoryActivity extends AppCompatActivity {

    private static final SimpleDateFormat DATE = new SimpleDateFormat("EEE, d MMM · HH:mm", Locale.US);

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
            ((TextView) row.findViewById(R.id.txt_date)).setText(DATE.format(new Date(trip.getStartMillis())));
            ((TextView) row.findViewById(R.id.txt_details)).setText(String.format(Locale.US, "%s · %s · max %.0f km/h",
                    Vehicle.of(trip.getVehicleType()).getDisplayName(),
                    formatDuration(trip.getDurationMillis()),
                    trip.getMaxSpeedKmh()));

            TextView alerts = row.findViewById(R.id.txt_alerts);
            boolean hadAlert = trip.getAlertCount() > 0;
            alerts.setText(hadAlert ? trip.getAlertCount() + " alert" + (trip.getAlertCount() > 1 ? "s" : "") : "Safe");
            alerts.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, hadAlert ? R.color.guard_alert_dark : R.color.guard_ok_dark)));

            list.addView(row);
            totalAlerts += trip.getAlertCount();
        }

        ((TextView) findViewById(R.id.txt_summary)).setText(
                String.format(Locale.US, "%d trip(s) · %d alert(s)", trips.size(), totalAlerts));
        findViewById(R.id.txt_empty).setVisibility(trips.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private static String formatDuration(long millis) {
        long minutes = millis / 60_000;
        long seconds = (millis / 1000) % 60;
        return minutes > 0 ? minutes + " min" : seconds + " s";
    }
}
