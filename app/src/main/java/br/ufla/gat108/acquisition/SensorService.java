package br.ufla.gat108.acquisition;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import br.ufla.gat108.domain.Track;
import br.ufla.gat108.reconciliation.KinematicReconciler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Serviço em primeiro plano para aquisição periódica (Capacidade 4).
 * Mantém o sistema vivo e coletando dados a 50Hz.
 */
public class SensorService extends Service {

    private static final String CHANNEL_ID = "SensorServiceChannel";
    private final AndroidSensorAcquisition acquisition = new AndroidSensorAcquisition();
    private final Track track = new Track(500); // Buffer para ~10 segundos a 50Hz
    private final KinematicReconciler reconciler = new KinematicReconciler();
    private ScheduledExecutorService scheduler;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(1, createNotification());

        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            sensorManager.registerListener(acquisition, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        }

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    acquisition.updateLocation(location);
                }
            });
        } catch (SecurityException e) {
            // Log de falta de permissão
        }

        // Loop de aquisição periódica (Capacidade 4) - 50Hz (20ms)
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleWithFixedDelay(() -> {
            track.append(acquisition.acquire());
        }, 0, 20, TimeUnit.MILLISECONDS);
    }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID, "Monitoramento do Trabalhador",
                NotificationManager.IMPORTANCE_LOW
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(serviceChannel);
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Segurança Ativa")
                .setContentText("Monitorando quedas e imobilidade...")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        scheduler.shutdown();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
