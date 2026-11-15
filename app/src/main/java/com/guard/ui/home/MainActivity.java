package com.guard.ui.home;

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
import com.guard.model.vehicle.Vehicle;
import com.guard.model.vehicle.VehicleType;
import com.guard.ui.common.ContactViews;
import com.guard.ui.contacts.ContactsActivity;
import com.guard.ui.history.HistoryActivity;
import com.guard.ui.trip.TripActivity;

/**
 * Tela 1 · "New trip" (a primeira tela do app; layout: activity_main.xml).
 * Escolhe o veículo, mostra a ordem de alerta e começa a viagem.
 *
 * CONCEITO ANDROID: Activity
 * Cada tela do app é uma Activity. O Android chama métodos dela em momentos certos
 * (o "ciclo de vida"):
 *   onCreate() -> a tela foi criada: montamos os componentes (roda uma vez)
 *   onResume() -> a tela voltou a aparecer (roda toda vez, inclusive ao voltar de outra tela)
 *   onPause()  -> a tela vai sair de vista
 */
public class MainActivity extends AppCompatActivity {

    private GuardApplication app;
    private MaterialButtonToggleGroup groupVehicle;   // os 3 botões Car / Motorcycle / Bicycle
    private LinearLayout listContacts;
    private TextView txtNoContacts;
    private TextView txtLimits;
    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);   // "desenha" o XML na tela
        app = (GuardApplication) getApplication();

        // Pegamos os componentes do XML pelo id.
        groupVehicle = findViewById(R.id.group_vehicle);
        listContacts = findViewById(R.id.list_contacts);
        txtNoContacts = findViewById(R.id.txt_no_contacts);
        txtLimits = findViewById(R.id.txt_limits);

        // LAMBDA: "(group, checkedId, isChecked) -> { ... }" é uma função sem nome,
        // passada como parâmetro. Ela roda toda vez que um botão de veículo muda.
        groupVehicle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;   // ignoramos o botão que foi DESmarcado
            VehicleType type = vehicleForButton(checkedId);
            app.getSettings().setVehicleType(type);   // lembra a escolha para a próxima vez
            showLimits(Vehicle.of(type));
        });

        // "v -> ..." = lambda que recebe a View clicada (v), que aqui não precisamos usar.
        // Intent = "pedido para abrir outra tela".
        findViewById(R.id.btn_edit_contacts).setOnClickListener(v ->
                startActivity(new Intent(this, ContactsActivity.class)));
        findViewById(R.id.btn_history).setOnClickListener(v ->
                startActivity(new Intent(this, HistoryActivity.class)));
        findViewById(R.id.btn_start).setOnClickListener(v -> startTrip());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recarregamos SEMPRE: o usuário pode ter mudado os contatos na outra tela.
        user = app.loadUser();
        groupVehicle.check(buttonForVehicle(user.getSelectedVehicle().getType()));
        showLimits(user.getSelectedVehicle());
        renderContacts();
    }

    /** Converte o id do botão clicado no tipo de veículo. */
    private VehicleType vehicleForButton(int buttonId) {
        if (buttonId == R.id.btn_car) return VehicleType.CAR;
        if (buttonId == R.id.btn_bike) return VehicleType.BICYCLE;
        return VehicleType.MOTORCYCLE;
    }

    /** O caminho inverso: tipo de veículo -> id do botão. */
    private int buttonForVehicle(VehicleType type) {
        switch (type) {
            case CAR:     return R.id.btn_car;
            case BICYCLE: return R.id.btn_bike;
            default:      return R.id.btn_moto;
        }
    }

    /** Mostra os limites do veículo, ex.: "Limits: 3.0 g · 20 km/h in 2 s · 150°/s". */
    private void showLimits(Vehicle v) {
        txtLimits.setText(getString(R.string.vehicle_limits,
                v.getImpactThresholdG(), v.getSpeedDropThresholdKmh(), v.getRotationThresholdDegPerSec()));
    }

    /** Desenha uma linha para cada contato, na ordem de prioridade. */
    private void renderContacts() {
        listContacts.removeAllViews();   // limpa o que estava antes
        for (EmergencyContact c : user.getContactsByPriority()) {
            listContacts.addView(ContactViews.inflate(listContacts, c));
        }
        // View.GONE = some (e não ocupa espaço); View.VISIBLE = aparece.
        txtNoContacts.setVisibility(user.hasContacts() ? View.GONE : View.VISIBLE);
    }

    private void startTrip() {
        // Sem contatos não há quem avisar: mandamos o usuário cadastrar antes.
        if (!user.hasContacts()) {
            // Toast = aquela mensagem rápida que aparece e some sozinha.
            Toast.makeText(this, R.string.no_contacts, Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, ContactsActivity.class));
            return;
        }
        startActivity(new Intent(this, TripActivity.class));
    }
}
