package br.ufla.gat108.acquisition;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.Location;
import br.ufla.gat108.contracts.AcquisitionContract;
import br.ufla.gat108.domain.Fix;
import br.ufla.gat108.domain.SensorReading;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementação real do contrato de aquisição para Android.
 * Captura dados do acelerômetro e localização em tempo real.
 */
public class AndroidSensorAcquisition implements AcquisitionContract, SensorEventListener {

    private final AtomicReference<SensorEvent> lastAccelerometerEvent = new AtomicReference<>();
    private final AtomicReference<Location> lastLocation = new AtomicReference<>();

    public void updateLocation(Location location) {
        lastLocation.set(location);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            lastAccelerometerEvent.set(event);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Reservado para Capacidade 2 (Incerteza dinâmica)
    }

    @Override
    public Fix acquire() {
        SensorEvent acc = lastAccelerometerEvent.get();
        Location loc = lastLocation.get();

        long now = System.currentTimeMillis();
        
        double ax = 0, ay = 0, az = 0, a_acc = 1.0;
        if (acc != null) {
            ax = acc.values[0];
            ay = acc.values[1];
            az = acc.values[2];
            // Acurácia do acelerômetro Android é qualitativa (int), 
            // mapeamos para um desvio padrão aproximado em m/s^2
            a_acc = mapSensorAccuracy(acc.accuracy);
        }

        double v = 0, lat = 0, lon = 0, p_acc = 100, v_acc = 10;
        if (loc != null) {
            v = loc.getSpeed();
            lat = loc.getLatitude();
            lon = loc.getLongitude();
            p_acc = loc.getAccuracy(); // Incerteza da posição
            v_acc = loc.getSpeedAccuracyMetersPerSecond(); // Incerteza da velocidade (API 26+)
        }

        SensorReading reading = new SensorReading(
            now, ax, ay, az, v, lat, lon, a_acc, p_acc, v_acc
        );

        return new Fix(now, reading);
    }

    private double mapSensorAccuracy(int accuracy) {
        // Mapeamento simples: quanto maior a precisão, menor o desvio padrão (m/s^2)
        return switch (accuracy) {
            case 3 -> 0.05; // High
            case 2 -> 0.15; // Medium
            case 1 -> 0.5;  // Low
            default -> 1.0; // Unreliable
        };
    }
}
