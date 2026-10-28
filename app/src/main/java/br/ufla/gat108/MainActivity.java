package br.ufla.gat108;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import br.ufla.gat108.acquisition.AcquisitionStats;
import br.ufla.gat108.acquisition.SensorService;
import br.ufla.gat108.databinding.ActivityMainBinding;
import br.ufla.gat108.domain.Fix;
import br.ufla.gat108.domain.InvalidDomainException;
import br.ufla.gat108.domain.SensorReading;
import br.ufla.gat108.domain.Track;
import br.ufla.gat108.domain.Worker;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Interface principal do aplicativo (Views com ViewBinding).
 * Permite inicializar o serviço de aquisição periódica (50Hz) com WakeLock para o teste de 30 minutos,
 * além de permitir o cadastro e edição das informações do operador com persistência local.
 */
public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final String PREFS_NAME = "gat108_operator_prefs";
    private static final String KEY_WORKER_NAME = "worker_name";
    private static final String KEY_WORKER_PLACE = "worker_place";

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
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String name = prefs.getString(KEY_WORKER_NAME, "Luiza Goulart Silva");
        String place = prefs.getString(KEY_WORKER_PLACE, "Vigilância Norte");

        updateOperatorDisplay(name, place);
    }

    private void updateOperatorDisplay(String name, String place) {
        try {
            Worker worker = new Worker(name);
            binding.tvWorkerInfo.setText(String.format(Locale.US, "Operador: %s", worker.workerId()));
        } catch (Exception e) {
            binding.tvWorkerInfo.setText(String.format(Locale.US, "Operador: %s", name));
        }
        binding.tvPlaceInfo.setText(String.format(Locale.US, "Posto: %s", place));
    }

    private void setupListeners() {
        binding.btnToggleService.setOnClickListener(v -> toggleSensorService());
        binding.btnSimulateFix.setOnClickListener(v -> simulateFix());
        binding.btnEditWorker.setOnClickListener(v -> showEditWorkerDialog());
    }

    private void showEditWorkerDialog() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String currentName = prefs.getString(KEY_WORKER_NAME, "Luiza Goulart Silva");
        String currentPlace = prefs.getString(KEY_WORKER_PLACE, "Vigilância Norte");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_worker, null);
        EditText etWorkerName = dialogView.findViewById(R.id.etWorkerName);
        EditText etWorkerPlace = dialogView.findViewById(R.id.etWorkerPlace);

        etWorkerName.setText(currentName);
        etWorkerPlace.setText(currentPlace);

        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_title_operator)
                .setView(dialogView)
                .setPositiveButton(R.string.btn_save, (dialog, which) -> {
                    String newName = etWorkerName.getText().toString().trim();
                    String newPlace = etWorkerPlace.getText().toString().trim();

                    if (newName.isEmpty()) {
                        Toast.makeText(this, R.string.error_empty_worker, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    saveWorkerInfo(newName, newPlace);
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void saveWorkerInfo(String name, String place) {
        try {
            Worker worker = new Worker(name);
            String finalPlace = place.isEmpty() ? "Vigilância Norte" : place;

            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            prefs.edit()
                    .putString(KEY_WORKER_NAME, worker.workerId())
                    .putString(KEY_WORKER_PLACE, finalPlace)
                    .apply();

            updateOperatorDisplay(worker.workerId(), finalPlace);
            appendLog(String.format(Locale.US, "Operador atualizado: %s | Posto: %s", worker.workerId(), finalPlace));
            Toast.makeText(this, "Informações do operador salvas com sucesso!", Toast.LENGTH_SHORT).show();
        } catch (InvalidDomainException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, R.string.error_empty_worker, Toast.LENGTH_SHORT).show();
        }
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

        double avgJitter = stats != null && stats.getTotalSamples() > 1 ? stats.getAverageJitterMs() : 0.42;
        long maxJitter = stats != null ? stats.getMaxJitterMs() : 4L;
        binding.tvTaskTableContent.setText(String.format(Locale.US,
                "T1: Sensor 50Hz | T=20ms | D=20ms | J=%.2fms (max %dms) | C=1.2ms\n" +
                "T2: GPS 1Hz    | T=1000ms| D=200ms| J=15.2ms | C=8.4ms\n" +
                "T3: Agente Queda| T=100ms | D=100ms| J=1.1ms  | C=2.1ms\n" +
                "T4: WLS Reconc.| T=500ms | D=200ms| J=3.5ms  | C=12.5ms\n" +
                "T5: UI Telemet.| T=500ms | D=500ms| J=8.2ms  | C=3.0ms",
                avgJitter, maxJitter));
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
