package com.guard.data;

import android.content.ContentValues;
import android.database.Cursor;

import com.guard.model.Trip;
import com.guard.model.vehicle.VehicleType;

import java.util.ArrayList;
import java.util.List;

/**
 * DAO das viagens: salva o resumo de uma Trip e lista o histórico.
 * Mesma ideia do ContactDao.
 */
public class TripDao {

    private final GuardDatabaseHelper helper;

    public TripDao(GuardDatabaseHelper helper) {
        this.helper = helper;
    }

    /** Salva o resumo de uma viagem que acabou. */
    public long insert(Trip trip) {
        ContentValues v = new ContentValues();
        v.put("vehicle_type", trip.getVehicleType().name());   // enum -> texto ("CAR")
        v.put("start_millis", trip.getStartMillis());
        v.put("end_millis", trip.getEndMillis());
        v.put("max_speed_kmh", trip.getMaxSpeedKmh());
        v.put("alert_count", trip.getIncidents().size());
        long id = helper.getWritableDatabase().insertOrThrow(GuardDatabaseHelper.TABLE_TRIPS, null, v);
        trip.setId(id);
        return id;
    }

    /** Todas as viagens, da mais recente para a mais antiga. */
    public List<TripRecord> findAllNewestFirst() {
        List<TripRecord> list = new ArrayList<>();
        try (Cursor c = helper.getReadableDatabase().rawQuery(
                "SELECT id, vehicle_type, start_millis, end_millis, max_speed_kmh, alert_count FROM "
                        + GuardDatabaseHelper.TABLE_TRIPS + " ORDER BY start_millis DESC", null)) {
            while (c.moveToNext()) {
                list.add(new TripRecord(
                        c.getLong(0),
                        VehicleType.valueOf(c.getString(1)),   // texto -> enum
                        c.getLong(2),
                        c.getLong(3),
                        c.getDouble(4),
                        c.getInt(5)));
            }
        }
        return list;
    }
}
