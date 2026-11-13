package com.guard.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.guard.GuardApplication;
import com.guard.R;
import com.guard.model.EmergencyContact;
import com.guard.model.User;
import com.guard.model.Vehicle;
import com.guard.model.VehicleType;

import java.util.Locale;

/** Tela 1 · New trip: escolhe o veículo, confere a ordem de alerta e começa a viagem. */
public class MainActivity extends AppCompatActivity {

    private GuardApplication app;
    private MaterialButtonToggleGroup groupVehicle;
    private LinearLayout listContacts;
    private TextView txtNoContacts;
    private TextView txtLimits;
    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        app = (GuardApplication) getApplication();

        groupVehicle = findViewById(R.id.group_vehicle);
        listContacts = findViewById(R.id.list_contacts);
        txtNoContacts = findViewById(R.id.txt_no_contacts);
        txtLimits = findViewById(R.id.txt_limits);

        groupVehicle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            VehicleType type = checkedId == R.id.btn_car ? VehicleType.CAR
                    : checkedId == R.id.btn_bike ? VehicleType.BICYCLE
                    : VehicleType.MOTORCYCLE;
            app.getSettings().setVehicleType(type);
            showLimits(Vehicle.of(type));
        });

        findViewById(R.id.btn_edit_contacts).setOnClickListener(v ->
                startActivity(new Intent(this, ContactsActivity.class)));
        findViewById(R.id.btn_history).setOnClickListener(v ->
                startActivity(new Intent(this, HistoryActivity.class)));
        findViewById(R.id.btn_start).setOnClickListener(v -> startTrip());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recarrega sempre: o usuário pode ter mudado os contatos na outra tela.
        user = app.loadUser();
        selectVehicle(user.getSelectedVehicle().getType());
        renderContacts();
    }

    private void selectVehicle(VehicleType type) {
        int id = type == VehicleType.CAR ? R.id.btn_car
                : type == VehicleType.BICYCLE ? R.id.btn_bike
                : R.id.btn_moto;
        groupVehicle.check(id);
        showLimits(Vehicle.of(type));
    }

    private void showLimits(Vehicle v) {
        txtLimits.setText(String.format(Locale.US, "Limits: %.1f g · %.0f km/h in 2 s · %.0f°/s",
                v.getImpactThresholdG(), v.getSpeedDropThresholdKmh(), v.getRotationThresholdDegPerSec()));
    }

    private void renderContacts() {
        listContacts.removeAllViews();
        for (EmergencyContact c : user.getContactsByPriority()) {
            listContacts.addView(ContactViews.inflate(listContacts, c));
        }
        txtNoContacts.setVisibility(user.hasContacts() ? View.GONE : View.VISIBLE);
    }

    private void startTrip() {
        if (!user.hasContacts()) {
            Toast.makeText(this, R.string.no_contacts, Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, ContactsActivity.class));
            return;
        }
        startActivity(new Intent(this, TripActivity.class));
    }
}
