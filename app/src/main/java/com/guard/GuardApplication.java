package com.guard;

import android.app.Application;

import com.guard.data.ContactDao;
import com.guard.data.GuardDatabaseHelper;
import com.guard.data.SettingsStore;
import com.guard.data.TripDao;
import com.guard.model.EmergencyContact;
import com.guard.model.User;
import com.guard.model.Vehicle;

/**
 * Objeto que vive enquanto o app estiver aberto. Guarda o banco e as preferências
 * para todas as telas usarem as mesmas instâncias.
 *
 * Nas telas: GuardApplication app = (GuardApplication) getApplication();
 */
public class GuardApplication extends Application {

    private GuardDatabaseHelper database;
    private ContactDao contactDao;
    private TripDao tripDao;
    private SettingsStore settings;

    @Override
    public void onCreate() {
        super.onCreate();
        database = new GuardDatabaseHelper(this);
        contactDao = new ContactDao(database);
        tripDao = new TripDao(database);
        settings = new SettingsStore(this);
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
}
