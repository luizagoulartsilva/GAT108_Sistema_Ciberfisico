package br.ufla.gat108;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import br.ufla.gat108.acquisition.AcquisitionStats;
import br.ufla.gat108.acquisition.SensorService;
import br.ufla.gat108.databinding.ActivityMainBinding;
import br.ufla.gat108.domain.Fix;
import br.ufla.gat108.domain.SensorReading;
import br.ufla.gat108.domain.Track;
import br.ufla.gat108.domain.Worker;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Interface principal do aplicativo (Views com ViewBinding).
 * Permite inicializar o serviço de aquisição periódica (50Hz) com WakeLock para o teste de 30 minutos.
 */
public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1001;
    private ActivityMainBinding binding;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private Runnable uiUpdaterRunnable;
    private final Track localTrack = new Track(100);
    private long simulatedTimestampOffset = System.currentTimeMillis();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        requestRequiredPermissions();
        setupWorkerInfo();
        setupListeners();
        startUiUpdater();
    }

    private void setupWorkerInfo() {
        try {
            Worker worker = new Worker("Luiza Goulart Silva");
            binding.tvWorkerInfo.setText(String.format(Locale.US, "Operador: %s (ID: TRAB-01)", worker.workerId()));
        } catch (Exception e) {
            binding.tvWorkerInfo.setText(R.string.loading);
        }
    }

    private void setupListeners() {
        binding.btnToggleService.setOnClickListener(v -> toggleSensorService());
        binding.btnSimulateFix.setOnClickListener(v -> simulateFix());
    }

    private void toggleSensorService() {
        Intent intent = new Intent(this, SensorService.class);
        if (SensorService.isRunning()) {
            stopService(intent);
            appendLog("Serviço de aquisição finalizado.");
        } else {
            ContextCompat.startForegroundService(this, intent);
            appendLog("Serviço de aquisição periódica (50Hz - WakeLock) iniciado.");
        }
        updateServiceButtonState();
    }

    private void simulateFix() {
        simulatedTimestampOffset += 20; // 20ms delta
        long now = simulatedTimestampOffset;
        SensorReading reading = new SensorReading(
                now, 0.2, 9.81, 0.5, 1.1, -21.229, -44.978, 0.05, 4.0, 0.1
        );
        Fix fix = new Fix(now, reading);

        Track sharedTrack = SensorService.getSharedTrack();
        if (sharedTrack != null) {
            sharedTrack.append(fix);
        } else {
            localTrack.append(fix);
        }

        appendLog(String.format(Locale.US, "Amostra simulada inserida: t=%d ms, Acc=[0.20, 9.81, 0.50]", now));
    }

    private void startUiUpdater() {
        uiUpdaterRunnable = new Runnable() {
            @Override
            public void run() {
                updateTelemetryUi();
                updateServiceButtonState();
                uiHandler.postDelayed(this, 500); // Atualiza a cada 500ms
            }
        };
        uiHandler.post(uiUpdaterRunnable);
    }

    private void updateTelemetryUi() {
        boolean running = SensorService.isRunning();
        AcquisitionStats stats = SensorService.getStats();
        Track sharedTrack = SensorService.getSharedTrack();
        Track activeTrack = sharedTrack != null ? sharedTrack : localTrack;

        if (running && stats != null) {
            binding.tvSystemStatus.setText(R.string.status_system);
            binding.tvTrackStatus.setText(String.format(Locale.US,
                    "Aquisição Ativa (50Hz) | Buffer: %d/%d\n%s",
                    activeTrack.size(), activeTrack.capacity(), stats.getFormattedSummary()));
        } else {
            binding.tvSystemStatus.setText("Status do Sistema: Repouso (Aguardando início)");
            binding.tvTrackStatus.setText(String.format(Locale.US,
                    "Buffer local (Track): %d/%d amostras",
                    activeTrack.size(), activeTrack.capacity()));
        }

        Fix latestFix = activeTrack.latest();
        if (latestFix != null) {
            SensorReading r = latestFix.reading();
            double accMagnitude = Math.sqrt(
                    r.accelerationX() * r.accelerationX() +
                    r.accelerationY() * r.accelerationY() +
                    r.accelerationZ() * r.accelerationZ()
            );
            binding.tvLastFix.setText(String.format(Locale.US,
                    "Última leitura: t=%d ms | |a|=%.2f m/s² | v=%.1f m/s",
                    latestFix.timestampMillis(), accMagnitude, r.speedMetersPerSecond()));
        } else {
            binding.tvLastFix.setText(R.string.no_readings);
        }
    }

    private void updateServiceButtonState() {
        if (SensorService.isRunning()) {
            binding.btnToggleService.setText(R.string.btn_stop_service);
            binding.btnToggleService.setBackgroundColor(ContextCompat.getColor(this, R.color.status_error));
        } else {
            binding.btnToggleService.setText(R.string.btn_start_service);
            binding.btnToggleService.setBackgroundColor(ContextCompat.getColor(this, R.color.primary));
        }
    }

    private void appendLog(String message) {
        String currentLog = binding.tvLogContent.getText().toString();
        String newEntry = String.format(Locale.US, "[%tT] %s\n", System.currentTimeMillis(), message);
        binding.tvLogContent.setText(newEntry + currentLog);
    }

    private void requestRequiredPermissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        List<String> needed = new ArrayList<>();
        for (String perm : permissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                needed.add(perm);
            }
        }

        if (!needed.isEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            appendLog("Permissões de localização e notificações atualizadas.");
        }
    }

    @Override
    protected void onDestroy() {
        uiHandler.removeCallbacks(uiUpdaterRunnable);
        super.onDestroy();
    }
}
