package com.hospital.medapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hospital.medapp.R;
import com.hospital.medapp.dao.AppointmentDAO;
import com.hospital.medapp.dao.TriageDAO;
import com.hospital.medapp.models.Appointment;
import com.hospital.medapp.models.Triage;
import com.hospital.medapp.utils.SessionManager;

import java.util.List;

// ══════════════════════════════════════════════════════════════════════════════
// PatientDashboardActivity
// ══════════════════════════════════════════════════════════════════════════════
public class PatientDashboardActivity extends AppCompatActivity {

    private SessionManager  session;
    private AppointmentDAO  appointmentDAO;
    private TriageDAO        triageDAO;
    private RecyclerView    rvAppointments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_dashboard);

        session        = new SessionManager(this);
        appointmentDAO = new AppointmentDAO(this);
        triageDAO      = new TriageDAO(this);

        TextView tvWelcome = findViewById(R.id.tvWelcome);
        tvWelcome.setText("Bienvenido/a, " + session.getUserName());

        rvAppointments = findViewById(R.id.rvAppointments);
        rvAppointments.setLayoutManager(new LinearLayoutManager(this));

        Button btnNewAppt   = findViewById(R.id.btnNewAppointment);
        Button btnTriage    = findViewById(R.id.btnSelfTriage);
        Button btnHistory   = findViewById(R.id.btnTriageHistory);
        Button btnLogout    = findViewById(R.id.btnLogout);

        // Nueva cita → seleccionar hospital
        btnNewAppt.setOnClickListener(v ->
                startActivity(new Intent(this, HospitalListActivity.class)));

        // Autotriaje remoto (PPG con la cámara)
        btnTriage.setOnClickListener(v -> {
            Intent i = new Intent(this, TriageActivity.class);
            i.putExtra(TriageActivity.EXTRA_MODE, TriageActivity.MODE_PATIENT_SELF);
            startActivity(i);
        });

        // Historial de triajes
        btnHistory.setOnClickListener(v -> showTriageHistory());

        btnLogout.setOnClickListener(v -> logout());

        loadAppointments();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAppointments();
        showLatestTriageSummary();
    }

    private void loadAppointments() {
        int userId = session.getUserId();
        List<Appointment> appointments = appointmentDAO.getAppointmentsByPatient(userId);
        // Usar el adapter de citas
        com.hospital.medapp.adapters.AppointmentAdapter adapter =
                new com.hospital.medapp.adapters.AppointmentAdapter(appointments,
                        appt -> confirmCancel(appt.getAppointmentId()),
                        false /* no es vista de médico */);
        rvAppointments.setAdapter(adapter);
    }

    private void showLatestTriageSummary() {
        Triage latest = triageDAO.getLatestTriageByPatient(session.getUserId());
        TextView tvTriage = findViewById(R.id.tvLatestTriage);
        if (latest != null) {
            String summary = String.format(
                    "Último triaje: %s\nFC: %.0f BPM | SpO₂: %.1f%%\nPrioridad: %s",
                    latest.getTriageDate(),
                    latest.getHeartRateBpm(),
                    latest.getSpo2Percent(),
                    latest.getPriority().name());
            tvTriage.setText(summary);
            // Color según prioridad
            int color;
            switch (latest.getPriority()) {
                case RED:    color = 0xFFFF0000; break;
                case ORANGE: color = 0xFFFF6600; break;
                case YELLOW: color = 0xFFFFCC00; break;
                default:     color = 0xFF009900; break;
            }
            tvTriage.setTextColor(color);
        } else {
            tvTriage.setText("Sin triajes registrados.");
        }
    }

    private void showTriageHistory() {
        List<Triage> history = triageDAO.getTriageByPatient(session.getUserId());
        if (history.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("Historial de triajes")
                    .setMessage("No tiene triajes registrados aún.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (Triage t : history) {
            sb.append(String.format("📅 %s\n  FC: %.0f bpm  SpO₂: %.1f%%  Prioridad: %s\n\n",
                    t.getTriageDate(), t.getHeartRateBpm(),
                    t.getSpo2Percent(), t.getPriority().name()));
        }
        new AlertDialog.Builder(this)
                .setTitle("Historial de triajes")
                .setMessage(sb.toString())
                .setPositiveButton("Cerrar", null)
                .show();
    }

    private void confirmCancel(int appointmentId) {
        new AlertDialog.Builder(this)
                .setTitle("Cancelar cita")
                .setMessage("¿Está seguro de que desea cancelar esta cita?")
                .setPositiveButton("Sí, cancelar", (d, w) -> {
                    appointmentDAO.cancelAppointment(appointmentId);
                    loadAppointments();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void logout() {
        session.logout();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
