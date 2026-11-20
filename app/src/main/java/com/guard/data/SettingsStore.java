package com.guard.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.guard.BuildConfig;
import com.guard.model.vehicle.VehicleType;

/**
 * Preferências simples (chave -> valor) que não precisam de tabela:
 * o nome do usuário e o último veículo escolhido.
 *
 * SharedPreferences é um "dicionário" que o Android salva num arquivo pequeno.
 * Ótimo para poucos valores; para listas (contatos, viagens) usamos o SQLite.
 */
public class SettingsStore {

    private static final String FILE = "guard_settings";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_VEHICLE = "vehicle";

    private final SharedPreferences prefs;

    public SettingsStore(Context context) {
        // MODE_PRIVATE = só o nosso app consegue ler este arquivo.
        this.prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    /**
     * Nome do usuário (vai no SMS). O segundo parâmetro é o valor padrão, se ainda não houver
     * nada salvo: o que estiver no local.properties (guard.user.name), ou vazio.
     */
    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, BuildConfig.USER_NAME);
    }

    public void setUserName(String name) {
        // edit() abre a edição, putString() muda, apply() salva (em segundo plano).
        // Isso é "encadeamento de métodos": cada chamada devolve um objeto para a próxima.
        prefs.edit().putString(KEY_USER_NAME, name).apply();
    }

    public VehicleType getVehicleType() {
        return VehicleType.valueOf(prefs.getString(KEY_VEHICLE, VehicleType.MOTORCYCLE.name()));
    }

    public void setVehicleType(VehicleType type) {
        prefs.edit().putString(KEY_VEHICLE, type.name()).apply();
    }
}
