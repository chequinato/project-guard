package com.guard.ui.common;

import com.guard.R;
import com.guard.sensor.Scenario;

/**
 * Liga cada cenário do simulador ao seu nome na tela (strings.xml, então traduz sozinho).
 * Mesma ideia do VehicleUi: o enum Scenario (camada de sensores) não sabe nada de R nem de idioma.
 */
public final class ScenarioUi {

    private ScenarioUi() { }

    public static int label(Scenario scenario) {
        switch (scenario) {
            case CRASH:         return R.string.scenario_crash;
            case PHONE_DROPPED: return R.string.scenario_phone_dropped;
            case HARD_BRAKE:    return R.string.scenario_hard_brake;
            case SPEED_BUMP:    return R.string.scenario_speed_bump;
            case PARKED:        return R.string.scenario_parked;
            default:            return R.string.scenario_normal;
        }
    }
}
