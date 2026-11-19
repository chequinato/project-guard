package com.guard.ui.common;

import com.guard.R;
import com.guard.model.vehicle.VehicleType;

/**
 * Liga cada tipo de veículo ao seu ícone e nome na tela.
 * Fica na camada de tela (ui) porque o modelo não deve saber nada de ícones nem de R.
 */
public final class VehicleUi {

    private VehicleUi() { }

    /** Ícone desenhado no site (res/drawable/ic_vehicle_*.xml). */
    public static int icon(VehicleType type) {
        switch (type) {
            case CAR:     return R.drawable.ic_vehicle_car;
            case BICYCLE: return R.drawable.ic_vehicle_bike;
            default:      return R.drawable.ic_vehicle_moto;
        }
    }

    /** Nome do veículo (res/values/strings.xml). */
    public static int label(VehicleType type) {
        switch (type) {
            case CAR:     return R.string.car;
            case BICYCLE: return R.string.bicycle;
            default:      return R.string.motorcycle;
        }
    }
}
