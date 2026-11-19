package com.guard;

import android.app.Application;

import com.guard.data.ContactDao;
import com.guard.data.GuardDatabaseHelper;
import com.guard.data.SettingsStore;
import com.guard.data.TripDao;
import com.guard.model.EmergencyContact;
import com.guard.model.Trip;
import com.guard.model.User;
import com.guard.model.vehicle.Vehicle;
import com.guard.sensor.SensorMonitor;
import com.guard.session.TripSession;
import com.guard.ui.common.Language;

/**
 * O objeto do APP inteiro: é criado quando o app abre e vive até ele fechar.
 * (Está registrado no AndroidManifest.xml, em android:name=".GuardApplication".)
 *
 * Guarda o que TODAS as telas compartilham:
 *   - o banco (DAOs) e as preferências;
 *   - a viagem em andamento (TripSession).
 *
 * Nas telas, para pegar este objeto:
 *     GuardApplication app = (GuardApplication) getApplication();
 */
public class GuardApplication extends Application {

    private ContactDao contactDao;
    private TripDao tripDao;
    private SettingsStore settings;
    private TripSession currentSession;   // null = nenhuma viagem em andamento

    /** O Android chama isto uma vez, quando o app é aberto. */
    @Override
    public void onCreate() {
        super.onCreate();   // sempre chamar o método da mãe primeiro
        GuardDatabaseHelper database = new GuardDatabaseHelper(this);
        contactDao = new ContactDao(database);
        tripDao = new TripDao(database);
        settings = new SettingsStore(this);
        Language.applyDefault(this);   // 1ª abertura: app em inglês (a bandeira troca depois)
    }

    public ContactDao getContactDao() { return contactDao; }
    public TripDao getTripDao() { return tripDao; }
    public SettingsStore getSettings() { return settings; }

    /** Monta o User atual a partir do que está salvo (nome, veículo e contatos). */
    public User loadUser() {
        User user = new User(settings.getUserName(), Vehicle.of(settings.getVehicleType()));
        for (EmergencyContact c : contactDao.findAllByPriority()) {
            user.addContact(c);
        }
        return user;
    }

    // ---------- Viagem em andamento ----------

    /** Começa uma viagem nova com a fonte de sensores escolhida (mock ou real). */
    public TripSession startTrip(SensorMonitor source) {
        currentSession = new TripSession(loadUser(), source, System.currentTimeMillis());
        return currentSession;
    }

    /** A viagem em andamento, ou null. */
    public TripSession getCurrentSession() {
        return currentSession;
    }

    /** Termina a viagem, salva no banco e devolve o resumo. */
    public Trip endTrip() {
        Trip trip = currentSession.end(System.currentTimeMillis());
        tripDao.insert(trip);
        currentSession = null;
        return trip;
    }
}
