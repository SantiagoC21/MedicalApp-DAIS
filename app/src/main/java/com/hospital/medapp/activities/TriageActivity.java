package com.hospital.medapp.activities;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.hospital.medapp.R;
import com.hospital.medapp.dao.TriageDAO;
import com.hospital.medapp.models.Triage;
import com.hospital.medapp.utils.PPGProcessor;
import com.hospital.medapp.utils.SessionManager;

/**
 * TriageActivity
 *
 * Modo PATIENT_SELF   → el paciente mide sus propios signos vitales con la cámara.
 * Modo DOCTOR_PRESENTIAL → el médico registra los signos del paciente asignado
 *                           (puede usar la cámara del teléfono para el PPG, o
 *                            ingresar valores manualmente).
 *
 * Flujo:
 *  1. Pedir permiso de cámara si no lo tiene.
 *  2. Mostrar instrucciones (dedo sobre cámara trasera).
 *  3. Medir 30 s con PPGProcessor.
 *  4. Mostrar resultados (BPM + SpO₂).
 *  5. Permitir añadir temperatura, presión, notas manualmente.
 *  6. Guardar el triaje en BD y mostrar prioridad calculada.
 */
public class TriageActivity extends AppCompatActivity implements PPGProcessor.PPGListener {

    // ─── Extras de Intent ─────────────────────────────────────────────────────
    public static final String EXTRA_MODE           = "mode";
    public static final String EXTRA_PATIENT_ID     = "patient_id";
    public static final String EXTRA_PATIENT_NAME   = "patient_name";
    public static final String EXTRA_APPOINTMENT_ID = "appointment_id";

    public static final int MODE_PATIENT_SELF       = 0;
    public static final int MODE_DOCTOR_PRESENTIAL  = 1;

    private static final int  CAMERA_PERMISSION_CODE = 200;
    private static final int  MEASUREMENT_SECONDS    = 30;

    // ─── UI ───────────────────────────────────────────────────────────────────
    private TextView    tvTitle, tvInstruction, tvBpm, tvSpo2, tvStatus, tvPriorityResult;
    private ProgressBar progressBar;
    private LinearLayout layoutInstructions, layoutMeasuring, layoutResults, layoutManual;
    private EditText    etTemp, etSysBp, etDiaBp, etNotes;
    private Button      btnStart, btnSave, btnCancel;

    // ─── Datos ────────────────────────────────────────────────────────────────
    private PPGProcessor   ppgProcessor;
    private SessionManager session;
    private TriageDAO       triageDAO;
    private CountDownTimer  countDownTimer;

    private int    mode;
    private int    targetPatientId;
    private String targetPatientName;
    private int    appointmentId;

    private double measuredBpm  = 0;
    private double measuredSpo2 = 0;

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_triage);

        session   = new SessionManager(this);
        triageDAO = new TriageDAO(this);

        // Leer extras
        mode             = getIntent().getIntExtra(EXTRA_MODE, MODE_PATIENT_SELF);
        targetPatientId  = getIntent().getIntExtra(EXTRA_PATIENT_ID, session.getUserId());
        targetPatientName= getIntent().getStringExtra(EXTRA_PATIENT_NAME);
        appointmentId    = getIntent().getIntExtra(EXTRA_APPOINTMENT_ID, 0);
        if (targetPatientName == null) targetPatientName = session.getUserName();

        initViews();
        setupUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ppgProcessor != null) ppgProcessor.stopMeasurement();
        if (countDownTimer != null) countDownTimer.cancel();
    }

    // ─── Inicialización ───────────────────────────────────────────────────────

    private void initViews() {
        tvTitle          = findViewById(R.id.tvTitle);
        tvInstruction    = findViewById(R.id.tvInstruction);
        tvBpm            = findViewById(R.id.tvBpm);
        tvSpo2           = findViewById(R.id.tvSpo2);
        tvStatus         = findViewById(R.id.tvStatus);
        tvPriorityResult = findViewById(R.id.tvPriorityResult);
        progressBar      = findViewById(R.id.progressMeasure);
        layoutInstructions = findViewById(R.id.layoutInstructions);
        layoutMeasuring  = findViewById(R.id.layoutMeasuring);
        layoutResults    = findViewById(R.id.layoutResults);
        layoutManual     = findViewById(R.id.layoutManual);
        etTemp           = findViewById(R.id.etTemp);
        etSysBp          = findViewById(R.id.etSysBp);
        etDiaBp          = findViewById(R.id.etDiaBp);
        etNotes          = findViewById(R.id.etNotes);
        btnStart         = findViewById(R.id.btnStart);
        btnSave          = findViewById(R.id.btnSave);
        btnCancel        = findViewById(R.id.btnCancel);
    }

    private void setupUI() {
        if (mode == MODE_DOCTOR_PRESENTIAL) {
            tvTitle.setText("Triaje Presencial: " + targetPatientName);
        } else {
            tvTitle.setText("Autotriaje – " + session.getUserName());
        }

        tvInstruction.setText(
                "📱 Coloque el dedo índice sobre la cámara trasera " +
                "cubriendo completamente el lente y el flash.\n\n" +
                "Permanezca quieto durante 30 segundos.");

        showPhase(Phase.INSTRUCTIONS);

        btnStart .setOnClickListener(v -> checkPermissionAndStart());
        btnSave  .setOnClickListener(v -> saveTriage());
        btnCancel.setOnClickListener(v -> finish());
    }

    // ─── Fases de la pantalla ─────────────────────────────────────────────────

    private enum Phase { INSTRUCTIONS, MEASURING, RESULTS }

    private void showPhase(Phase phase) {
        layoutInstructions.setVisibility(phase == Phase.INSTRUCTIONS ? View.VISIBLE : View.GONE);
        layoutMeasuring   .setVisibility(phase == Phase.MEASURING    ? View.VISIBLE : View.GONE);
        layoutResults     .setVisibility(phase == Phase.RESULTS      ? View.VISIBLE : View.GONE);
        layoutManual      .setVisibility(phase == Phase.RESULTS      ? View.VISIBLE : View.GONE);
        btnSave .setVisibility           (phase == Phase.RESULTS      ? View.VISIBLE : View.GONE);
    }

    // ─── Permiso de cámara ────────────────────────────────────────────────────

    private void checkPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_CODE);
        } else {
            startPPGMeasurement();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
            @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startPPGMeasurement();
        } else {
            Toast.makeText(this,
                    "Se necesita permiso de cámara para medir signos vitales.",
                    Toast.LENGTH_LONG).show();
        }
    }

    // ─── Medición PPG ─────────────────────────────────────────────────────────

    private void startPPGMeasurement() {
        showPhase(Phase.MEASURING);
        tvBpm .setText("-- BPM");
        tvSpo2.setText("-- %");
        tvStatus.setText("Iniciando sensor...");

        progressBar.setMax(MEASUREMENT_SECONDS);
        progressBar.setProgress(0);

        // Cuenta regresiva visual
        countDownTimer = new CountDownTimer(MEASUREMENT_SECONDS * 1000L, 1000) {
            int elapsed = 0;
            @Override
            public void onTick(long ms) {
                elapsed++;
                progressBar.setProgress(elapsed);
                tvStatus.setText("Midiendo... " + elapsed + " / " + MEASUREMENT_SECONDS + " s");
            }
            @Override
            public void onFinish() {
                tvStatus.setText("Procesando señal...");
            }
        }.start();

        ppgProcessor = new PPGProcessor(this);
        ppgProcessor.startMeasurement(this);
    }

    // ─── PPGListener callbacks (vienen en el main thread) ────────────────────

    @Override
    public void onHeartRateUpdated(double bpm) {
        if (bpm > 30 && bpm < 220) {
            tvBpm.setText(String.format("%.0f BPM", bpm));
        }
    }

    @Override
    public void onSpO2Updated(double spo2) {
        if (spo2 > 50 && spo2 <= 100) {
            tvSpo2.setText(String.format("%.1f %%", spo2));
        }
    }

    @Override
    public void onMeasurementComplete(double bpm, double spo2) {
        if (countDownTimer != null) countDownTimer.cancel();

        // Valores de seguridad ante lecturas imposibles
        measuredBpm  = (bpm  > 30  && bpm  < 220) ? bpm  : 0;
        measuredSpo2 = (spo2 > 50  && spo2 <= 100) ? spo2 : 0;

        showPhase(Phase.RESULTS);

        tvBpm .setText(measuredBpm  > 0 ? String.format("%.0f BPM",  measuredBpm)  : "No detectado");
        tvSpo2.setText(measuredSpo2 > 0 ? String.format("%.1f %%",   measuredSpo2) : "No detectado");

        // Preview de prioridad en tiempo real (sin temp/PA todavía)
        if (measuredBpm > 0 && measuredSpo2 > 0) {
            Triage.Priority preview = Triage.calculatePriority(measuredBpm, measuredSpo2, 37.0);
            showPriorityBadge(preview, true);
        }

        if (measuredBpm == 0 && measuredSpo2 == 0) {
            Toast.makeText(this,
                    "No se pudieron leer los signos vitales. Verifique que cubrió bien la cámara.",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onError(String message) {
        if (countDownTimer != null) countDownTimer.cancel();
        runOnUiThread(() -> {
            showPhase(Phase.INSTRUCTIONS);
            Toast.makeText(this, "Error: " + message, Toast.LENGTH_LONG).show();
        });
    }

    // ─── Guardar triaje ───────────────────────────────────────────────────────

    private void saveTriage() {
        double temp  = parseDouble(etTemp .getText().toString(), 37.0);
        int    sysBp = parseInt  (etSysBp.getText().toString(), 120);
        int    diaBp = parseInt  (etDiaBp.getText().toString(), 80);
        String notes = etNotes  .getText().toString().trim();

        if (measuredBpm == 0 && measuredSpo2 == 0) {
            new AlertDialog.Builder(this)
                    .setTitle("Sin lecturas de sensor")
                    .setMessage("No se registraron datos del sensor PPG. ¿Desea guardar solo los datos manuales?")
                    .setPositiveButton("Sí, guardar", (d, w) -> doSave(temp, sysBp, diaBp, notes))
                    .setNegativeButton("Cancelar y remedir", (d, w) -> showPhase(Phase.INSTRUCTIONS))
                    .show();
            return;
        }
        doSave(temp, sysBp, diaBp, notes);
    }

    private void doSave(double temp, int sysBp, int diaBp, String notes) {
        Triage triage = new Triage();
        triage.setPatientId   (targetPatientId);
        triage.setDoctorId    (mode == MODE_DOCTOR_PRESENTIAL ? session.getUserId() : 0);
        triage.setAppointmentId(appointmentId);
        triage.setHeartRateBpm(measuredBpm);
        triage.setSpo2Percent (measuredSpo2);
        triage.setTemperatureC(temp);
        triage.setSystolicBp  (sysBp);
        triage.setDiastolicBp (diaBp);
        triage.setNotes       (notes);
        triage.setTriageType  (mode == MODE_DOCTOR_PRESENTIAL
                ? Triage.Type.PRESENTIAL : Triage.Type.REMOTE);

        long id = triageDAO.saveTriage(triage);

        if (id > 0) {
            showPriorityBadge(triage.getPriority(), false);
            new AlertDialog.Builder(this)
                    .setTitle("Triaje guardado")
                    .setMessage(buildResultMessage(triage))
                    .setPositiveButton("Aceptar", (d, w) -> finish())
                    .setCancelable(false)
                    .show();
        } else {
            Toast.makeText(this, "Error al guardar el triaje.", Toast.LENGTH_SHORT).show();
        }
    }

    // ─── Helpers UI ───────────────────────────────────────────────────────────

    private void showPriorityBadge(Triage.Priority priority, boolean isPreview) {
        int color;
        String label;
        switch (priority) {
            case RED:
                color = ContextCompat.getColor(this, R.color.priority_red);
                label = "🔴 CRÍTICO – Con urgencia médica";
                break;
            case ORANGE:
                color = ContextCompat.getColor(this, R.color.priority_orange);
                label = "🟠 URGENTE – Atención < 30 min";
                break;
            case YELLOW:
                color = ContextCompat.getColor(this, R.color.priority_yellow);
                label = "🟡 MODERADO – Atención < 2 horas";
                break;
            default:
                color = ContextCompat.getColor(this, R.color.priority_green);
                label = "🟢 NORMAL – Sin urgencia";
                break;
        }
        tvPriorityResult.setTextColor(color);
        tvPriorityResult.setText(isPreview ? "(Preliminar) " + label : label);
        tvPriorityResult.setVisibility(View.VISIBLE);
        // Animación de pulso para prioridades altas
        if (priority == Triage.Priority.RED || priority == Triage.Priority.ORANGE) {
            ObjectAnimator anim = ObjectAnimator.ofFloat(
                    tvPriorityResult, "alpha", 1f, 0.3f, 1f);
            anim.setDuration(800);
            anim.setRepeatCount(ObjectAnimator.INFINITE);
            anim.start();
        }
    }

    private String buildResultMessage(Triage t) {
        return String.format(
                "Paciente: %s\n\n" +
                "Frecuencia cardíaca: %.0f BPM\n" +
                "SpO₂: %.1f %%\n" +
                "Temperatura: %.1f °C\n" +
                "Presión arterial: %d / %d mmHg\n\n" +
                "Prioridad de triaje: %s",
                targetPatientName,
                t.getHeartRateBpm(), t.getSpo2Percent(),
                t.getTemperatureC(), t.getSystolicBp(), t.getDiastolicBp(),
                t.getPriority().name());
    }

    private double parseDouble(String s, double def) {
        try { return Double.parseDouble(s); } catch (Exception e) { return def; }
    }

    private int parseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }
}
