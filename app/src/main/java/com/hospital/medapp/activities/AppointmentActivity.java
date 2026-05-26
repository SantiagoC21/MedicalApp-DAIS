package com.hospital.medapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hospital.medapp.R;
import com.hospital.medapp.dao.AppointmentDAO;
import com.hospital.medapp.dao.HospitalDAO;
import com.hospital.medapp.dao.ScheduleDAO;
import com.hospital.medapp.models.Appointment;
import com.hospital.medapp.models.Hospital;
import com.hospital.medapp.models.Schedule;
import com.hospital.medapp.utils.SessionManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

// ══════════════════════════════════════════════════════════════════════════════
// HospitalListActivity  – el paciente elige hospital y fecha
// ══════════════════════════════════════════════════════════════════════════════


// ══════════════════════════════════════════════════════════════════════════════
// AppointmentActivity  – elige fecha, ve horarios disponibles y reserva
// ══════════════════════════════════════════════════════════════════════════════
public class AppointmentActivity extends AppCompatActivity {

    public static final String EXTRA_HOSPITAL_ID   = "hospital_id";
    public static final String EXTRA_HOSPITAL_NAME = "hospital_name";

    private CalendarView  calendarView;
    private RecyclerView  rvSlots;
    private TextView      tvHospitalName, tvNoSlots;
    private ScheduleDAO   scheduleDAO;
    private AppointmentDAO appointmentDAO;
    private SessionManager session;

    private int    hospitalId;
    private String selectedDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointment);

        session        = new SessionManager(this);
        scheduleDAO    = new ScheduleDAO(this);
        appointmentDAO = new AppointmentDAO(this);

        hospitalId   = getIntent().getIntExtra(EXTRA_HOSPITAL_ID, -1);
        String hName = getIntent().getStringExtra(EXTRA_HOSPITAL_NAME);

        tvHospitalName = findViewById(R.id.tvHospitalName);
        tvNoSlots      = findViewById(R.id.tvNoSlots);
        calendarView   = findViewById(R.id.calendarView);
        rvSlots        = findViewById(R.id.rvSlots);
        rvSlots.setLayoutManager(new LinearLayoutManager(this));

        tvHospitalName.setText(hName != null ? hName : "Hospital");

        // Solo permitir fechas futuras
        calendarView.setMinDate(System.currentTimeMillis());

        // Fecha seleccionada por defecto = hoy
        selectedDate = todayString();
        loadSlots(selectedDate);

        calendarView.setOnDateChangeListener((view, year, month, day) -> {
            selectedDate = String.format(Locale.getDefault(),
                    "%04d-%02d-%02d", year, month + 1, day);
            loadSlots(selectedDate);
        });
    }

    private void loadSlots(String date) {
        List<Schedule> slots = scheduleDAO.getAvailableSlots(hospitalId, date);
        if (slots.isEmpty()) {
            tvNoSlots.setVisibility(android.view.View.VISIBLE);
            rvSlots  .setVisibility(android.view.View.GONE);
            tvNoSlots.setText("No hay horarios disponibles para " + date);
        } else {
            tvNoSlots.setVisibility(android.view.View.GONE);
            rvSlots  .setVisibility(android.view.View.VISIBLE);

            com.hospital.medapp.adapters.ScheduleAdapter adapter =
                    new com.hospital.medapp.adapters.ScheduleAdapter(slots, slot ->
                            confirmBooking(slot));
            rvSlots.setAdapter(adapter);
        }
    }

    private void confirmBooking(Schedule slot) {
        String msg = String.format(
                "¿Confirmar cita?\n\nFecha: %s %s\nMédico: %s",
                slot.getDate(), slot.getTime(), slot.getDoctorName());

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Reservar cita")
                .setMessage(msg)
                .setPositiveButton("Confirmar", (d, w) -> bookSlot(slot))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void bookSlot(Schedule slot) {
        Appointment appt = new Appointment();
        appt.setPatientId  (session.getUserId());
        appt.setDoctorId   (slot.getDoctorId());
        appt.setHospitalId (hospitalId);
        appt.setScheduleId (slot.getScheduleId());
        appt.setReason     ("Consulta general");

        long id = appointmentDAO.bookAppointment(appt);

        if (id > 0) {
            Toast.makeText(this,
                    "✅ Cita reservada para " + slot.getDate() + " a las " + slot.getTime(),
                    Toast.LENGTH_LONG).show();
            finish();
        } else {
            Toast.makeText(this,
                    "Error al reservar. El horario puede haberse ocupado.",
                    Toast.LENGTH_LONG).show();
            loadSlots(selectedDate); // Refrescar
        }
    }

    private String todayString() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(Calendar.getInstance().getTime());
    }
}
