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
import android.os.IBinder;
import android.os.PowerManager;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import br.ufla.gat108.domain.Fix;
import br.ufla.gat108.domain.Track;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Serviço em primeiro plano para aquisição periódica (Capacidade 4).
 * Mantém a CPU ativa com WakeLock e captura dados a 50Hz mesmo com o aparelho bloqueado por 30 minutos.
 */
public class SensorService extends Service {

    private static final String CHANNEL_ID = "SensorServiceChannel";
    private static final int NOTIFICATION_ID = 101;

    private static volatile SensorService instance;
    private static AcquisitionStats stats = new AcquisitionStats();

    private final AndroidSensorAcquisition acquisition = new AndroidSensorAcquisition();
    private final Track track = new Track(500); // Buffer circular para 10s a 50Hz
    private ScheduledExecutorService scheduler;
    private PowerManager.WakeLock wakeLock;

    public static AcquisitionStats getStats() {
        return stats;
    }

    public static Track getSharedTrack() {
        return instance != null ? instance.track : null;
    }

    public static boolean isRunning() {
        return instance != null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        stats = new AcquisitionStats(); // Reinicia métricas no início do serviço

        // Adquire WakeLock para garantir execução contínua com tela desligada (Capacidade 4)
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GAT108:SensorServiceWakeLock");
            wakeLock.acquire(35 * 60 * 1000L); // Timeout de segurança de 35 minutos para o ensaio de 30 minutos
        }

        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());

        // Registro de sensores inerciais
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accelerometer != null) {
                sensorManager.registerListener(acquisition, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
            }
        }

        // Registro de localização GNSS/GPS
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {
            try {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        acquisition.updateLocation(location);
                    }
                });
            } catch (SecurityException ignored) {
                // Permissão deve ser concedida pela UI
            }
        }

        // Loop de aquisição periódica em tempo real (50Hz / 20ms)
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                Fix fix = acquisition.acquire();
                track.append(fix);
                stats.recordSample(fix.timestampMillis());

                // Atualiza a notificação em primeiro plano periodicamente (~1 segundo)
                if (stats.getTotalSamples() % 50 == 0) {
                    updateNotification();
                }
            } catch (Exception ignored) {
                // Previne queda do thread do executor
            }
        }, 0, 20, TimeUnit.MILLISECONDS);
    }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID, "Monitoramento do Trabalhador Isolado",
                NotificationManager.IMPORTANCE_LOW
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private Notification createNotification() {
        String contentText = stats.getFormattedSummary();
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Segurança Ativa — Monitoramento 50Hz")
                .setContentText(contentText)
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build();
    }

    private void updateNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, createNotification());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        instance = null;
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
