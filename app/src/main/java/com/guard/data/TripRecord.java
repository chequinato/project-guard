package com.guard.data;

import com.guard.model.vehicle.VehicleType;

/**
 * Uma linha da tabela "trips", pronta para a tela de histórico.
 *
 * Por que não usar a classe Trip? A Trip é uma viagem EM ANDAMENTO (recebe leituras, tem regras).
 * No histórico só precisamos de um resumo para LER. Então este é um objeto simples e imutável.
 */
public final class TripRecord {

    private final long id;
    private final VehicleType vehicleType;
    private final long startMillis;
    private final long endMillis;
    private final double maxSpeedKmh;
    private final int alertCount;

    public TripRecord(long id, VehicleType vehicleType, long startMillis, long endMillis,
                      double maxSpeedKmh, int alertCount) {
        this.id = id;
        this.vehicleType = vehicleType;
        this.startMillis = startMillis;
        this.endMillis = endMillis;
        this.maxSpeedKmh = maxSpeedKmh;
        this.alertCount = alertCount;
    }

    public long getId() { return id; }
    public VehicleType getVehicleType() { return vehicleType; }
    public long getStartMillis() { return startMillis; }
    public long getEndMillis() { return endMillis; }
    public long getDurationMillis() { return endMillis - startMillis; }
    public double getMaxSpeedKmh() { return maxSpeedKmh; }
    public int getAlertCount() { return alertCount; }
}
