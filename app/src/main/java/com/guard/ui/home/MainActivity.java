package com.guard.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.guard.GuardApplication;
import com.guard.R;
import com.guard.model.EmergencyContact;
import com.guard.model.User;
import com.guard.model.vehicle.Vehicle;
import com.guard.model.vehicle.VehicleType;
import com.guard.ui.common.ContactViews;
import com.guard.ui.common.Press;
import com.guard.ui.common.VehicleUi;
import com.guard.ui.contacts.ContactsActivity;
import com.guard.ui.history.HistoryActivity;
import com.guard.ui.trip.TripActivity;

import java.util.List;

/**
 * Tela 1 · "New trip" (layout: activity_main.xml).
 * Escolhe o veículo, mostra os limites e a ordem de alerta, e começa a viagem.
 *
 * CONCEITO ANDROID: Activity
 * Cada tela do app é uma Activity. O Android chama métodos dela em momentos certos (ciclo de vida):
 *   onCreate() -> a tela foi criada: montamos os componentes (roda uma vez)
 *   onResume() -> a tela voltou a aparecer (roda toda vez, inclusive ao voltar de outra tela)
 */
public class MainActivity extends AppCompatActivity {

    private GuardApplication app;
    private View tileCar, tileMoto, tileBike;   // os 3 blocos de veículo
    private LinearLayout listContacts;
    private TextView txtNoContacts, txtLimitImpact, txtLimitDrop, txtLimitRotation;
    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        app = (GuardApplication) getApplication();

        listContacts = findViewById(R.id.list_contacts);
        txtNoContacts = findViewById(R.id.txt_no_contacts);
        txtLimitImpact = findViewById(R.id.txt_limit_impact);
        txtLimitDrop = findViewById(R.id.txt_limit_drop);
        txtLimitRotation = findViewById(R.id.txt_limit_rotation);

        // Cada bloco é o mesmo layout (item_vehicle_tile.xml) com ícone e nome diferentes.
        tileCar = setupTile(R.id.btn_car, VehicleType.CAR);
        tileMoto = setupTile(R.id.btn_moto, VehicleType.MOTORCYCLE);
        tileBike = setupTile(R.id.btn_bike, VehicleType.BICYCLE);

        View btnStart = findViewById(R.id.btn_start);
        View btnHistory = findViewById(R.id.btn_history);
        View btnEdit = findViewById(R.id.btn_edit_contacts);
        Press.attach(btnStart, btnHistory, btnEdit);   // efeito de "afundar" ao tocar

        // Intent = "pedido para abrir outra tela".
        btnEdit.setOnClickListener(v -> startActivity(new Intent(this, ContactsActivity.class)));
        btnHistory.setOnClickListener(v -> startActivity(new Intent(this, HistoryActivity.class)));
        btnStart.setOnClickListener(v -> {
            Press.tick(v);
            startTrip();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recarregamos SEMPRE: o usuário pode ter mudado os contatos na outra tela.
        user = app.loadUser();
        selectVehicle(user.getSelectedVehicle().getType(), false);
        renderContacts();
    }

    /** Preenche um bloco de veículo e liga o clique. Devolve o bloco para guardarmos no campo. */
    private View setupTile(int tileId, VehicleType type) {
        View tile = findViewById(tileId);
        ((ImageView) tile.findViewById(R.id.tile_icon)).setImageResource(VehicleUi.icon(type));
        ((TextView) tile.findViewById(R.id.tile_label)).setText(VehicleUi.label(type));
        Press.attach(tile);
        tile.setOnClickListener(v -> {
            Press.tick(v);
            app.getSettings().setVehicleType(type);   // lembra a escolha para a próxima vez
            selectVehicle(type, true);
        });
        return tile;
    }

    /** Marca o bloco escolhido (o fundo fica azul sozinho, pelo bg_tile.xml) e mostra os limites. */
    private void selectVehicle(VehicleType type, boolean animate) {
        tileCar.setSelected(type == VehicleType.CAR);
        tileMoto.setSelected(type == VehicleType.MOTORCYCLE);
        tileBike.setSelected(type == VehicleType.BICYCLE);
        showLimits(Vehicle.of(type), animate);
    }

    /** Mostra os 3 limites do veículo. Ao trocar de veículo, os números "piscam" para chamar atenção. */
    private void showLimits(Vehicle v, boolean animate) {
        txtLimitImpact.setText(getString(R.string.limit_impact_value, v.getImpactThresholdG()));
        txtLimitDrop.setText(getString(R.string.limit_drop_value, v.getSpeedDropThresholdKmh()));
        txtLimitRotation.setText(getString(R.string.limit_rotation_value, v.getRotationThresholdDegPerSec()));
        if (animate) {
            for (View t : new View[]{txtLimitImpact, txtLimitDrop, txtLimitRotation}) {
                t.setAlpha(0.2f);
                t.setTranslationY(6f);
                t.animate().alpha(1f).translationY(0f).setDuration(260).start();
            }
        }
    }

    /** Desenha uma linha para cada contato, na ordem de prioridade. */
    private void renderContacts() {
        listContacts.removeAllViews();
        List<EmergencyContact> contacts = user.getContactsByPriority();
        for (int i = 0; i < contacts.size(); i++) {
            boolean isLast = i == contacts.size() - 1;
            listContacts.addView(ContactViews.inflate(listContacts, contacts.get(i), !isLast, null));
        }
        // View.GONE = some (e não ocupa espaço); View.VISIBLE = aparece.
        listContacts.setVisibility(user.hasContacts() ? View.VISIBLE : View.GONE);
        txtNoContacts.setVisibility(user.hasContacts() ? View.GONE : View.VISIBLE);
    }

    private void startTrip() {
        // Sem contatos não há quem avisar: mandamos o usuário cadastrar antes.
        if (!user.hasContacts()) {
            Toast.makeText(this, R.string.no_contacts, Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, ContactsActivity.class));
            return;
        }
        startActivity(new Intent(this, TripActivity.class));
    }
}
