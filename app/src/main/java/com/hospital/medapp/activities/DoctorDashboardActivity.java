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
// DoctorDashboardActivity
// ══════════════════════════════════════════════════════════════════════════════
public class DoctorDashboardActivity extends AppCompatActivity {

    private SessionManager  session;
    private AppointmentDAO  appointmentDAO;
    private TriageDAO        triageDAO;
    private RecyclerView    rvAppointments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_dashboard);

        session        = new SessionManager(this);
        appointmentDAO = new AppointmentDAO(this);
        triageDAO      = new TriageDAO(this);

        TextView tvWelcome = findViewById(R.id.tvWelcome);
        tvWelcome.setText("Dr./Dra. " + session.getUserName());

        rvAppointments = findViewById(R.id.rvAppointments);
        rvAppointments.setLayoutManager(new LinearLayoutManager(this));

        Button btnPresentialTriage = findViewById(R.id.btnPresentialTriage);
        Button btnMyPatients       = findViewById(R.id.btnMyPatients);
        Button btnManageSchedules  = findViewById(R.id.btnManageSchedules);
        Button btnLogout           = findViewById(R.id.btnLogout);

        // Triaje presencial: el médico elige una cita pendiente
        btnPresentialTriage.setOnClickListener(v -> selectPatientForTriage());

        // Ver todos los pacientes con citas asignadas
        btnMyPatients.setOnClickListener(v -> loadAppointments());

        // Gestionar mis horarios
        btnManageSchedules.setOnClickListener(v ->
                startActivity(new Intent(this, ManageScheduleActivity.class)));

        btnLogout.setOnClickListener(v -> logout());

        loadAppointments();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAppointments();
    }

    private void loadAppointments() {
        int doctorId = session.getUserId();
        List<Appointment> appointments = appointmentDAO.getAppointmentsByDoctor(doctorId);
        com.hospital.medapp.adapters.AppointmentAdapter adapter =
                new com.hospital.medapp.adapters.AppointmentAdapter(appointments,
                        appt -> openTriageForPatient(appt, Triage.Type.PRESENTIAL),
                        true /* vista de médico */);
        rvAppointments.setAdapter(adapter);
    }

    /**
     * Muestra diálogo para que el médico seleccione qué paciente triajear
     * en caso de no venir de una cita programada.
     */
    private void selectPatientForTriage() {
        int doctorId = session.getUserId();
        List<Appointment> pending = appointmentDAO.getPendingTriageForDoctor(doctorId);

        if (pending.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("Sin triajes pendientes")
                    .setMessage("No tiene pacientes con citas pendientes de triaje.")
                    .setPositiveButton("OK", null).show();
            return;
        }

        String[] names = new String[pending.size()];
        for (int i = 0; i < pending.size(); i++) {
            Appointment a = pending.get(i);
            names[i] = a.getPatientName() + " – " + a.getScheduleDate() + " " + a.getScheduleTime();
        }

        new AlertDialog.Builder(this)
                .setTitle("Seleccione paciente para triaje")
                .setItems(names, (dialog, which) ->
                        openTriageForPatient(pending.get(which), Triage.Type.PRESENTIAL))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void openTriageForPatient(Appointment appt, Triage.Type type) {
        Intent intent = new Intent(this, TriageActivity.class);
        intent.putExtra(TriageActivity.EXTRA_MODE,           TriageActivity.MODE_DOCTOR_PRESENTIAL);
        intent.putExtra(TriageActivity.EXTRA_PATIENT_ID,     appt.getPatientId());
        intent.putExtra(TriageActivity.EXTRA_PATIENT_NAME,   appt.getPatientName());
        intent.putExtra(TriageActivity.EXTRA_APPOINTMENT_ID, appt.getAppointmentId());
        startActivity(intent);
    }

    private void logout() {
        session.logout();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
