package com.guard.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.guard.model.VehicleType;

/**
 * Preferências simples (chave/valor) que não precisam de tabela: nome do usuário e último veículo.
 */
public class SettingsStore {

    private static final String FILE = "guard_settings";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_VEHICLE = "vehicle";

    private final SharedPreferences prefs;

    public SettingsStore(Context context) {
        this.prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, "Lucas");
    }

    public void setUserName(String name) {
        prefs.edit().putString(KEY_USER_NAME, name).apply();
    }

    public VehicleType getVehicleType() {
        return VehicleType.valueOf(prefs.getString(KEY_VEHICLE, VehicleType.MOTORCYCLE.name()));
    }

    public void setVehicleType(VehicleType type) {
        prefs.edit().putString(KEY_VEHICLE, type.name()).apply();
    }
}
