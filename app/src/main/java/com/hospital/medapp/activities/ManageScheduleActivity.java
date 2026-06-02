package com.hospital.medapp.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hospital.medapp.R;
import com.hospital.medapp.adapters.DoctorScheduleAdapter;
import com.hospital.medapp.dao.HospitalDAO;
import com.hospital.medapp.dao.ScheduleDAO;
import com.hospital.medapp.models.Hospital;
import com.hospital.medapp.models.Schedule;
import com.hospital.medapp.utils.SessionManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ManageScheduleActivity extends AppCompatActivity {

    private Spinner spHospitals;
    private TextView tvSelectedDate, tvSelectedTime, tvNoSchedules;
    private RecyclerView rvMySchedules;

    private HospitalDAO hospitalDAO;
    private ScheduleDAO scheduleDAO;
    private SessionManager session;

    private List<Hospital> hospitals;
    private String selectedDate = "";
    private String selectedTime = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_schedule);

        session = new SessionManager(this);
        hospitalDAO = new HospitalDAO(this);
        scheduleDAO = new ScheduleDAO(this);

        spHospitals = findViewById(R.id.spHospitals);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvSelectedTime = findViewById(R.id.tvSelectedTime);
        tvNoSchedules = findViewById(R.id.tvNoSchedules);
        rvMySchedules = findViewById(R.id.rvMySchedules);
        rvMySchedules.setLayoutManager(new LinearLayoutManager(this));

        Button btnPickDate = findViewById(R.id.btnPickDate);
        Button btnPickTime = findViewById(R.id.btnPickTime);
        Button btnAddSlot = findViewById(R.id.btnAddSlot);
        Button btnBack = findViewById(R.id.btnBack);

        btnPickDate.setOnClickListener(v -> showDatePicker());
        btnPickTime.setOnClickListener(v -> showTimePicker());
        btnAddSlot.setOnClickListener(v -> addScheduleSlot());
        btnBack.setOnClickListener(v -> finish());

        loadHospitals();
        loadSchedules();
    }

    private void loadHospitals() {
        hospitals = hospitalDAO.getAllHospitals();
        List<String> names = new ArrayList<>();
        for (Hospital h : hospitals) {
            names.add(h.getName() + " (" + h.getSpecialty() + ")");
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spHospitals.setAdapter(adapter);
    }

    private void loadSchedules() {
        int doctorId = session.getUserId();
        List<Schedule> mySlots = scheduleDAO.getSchedulesByDoctor(doctorId);
        if (mySlots.isEmpty()) {
            tvNoSchedules.setVisibility(View.VISIBLE);
            rvMySchedules.setVisibility(View.GONE);
        } else {
            tvNoSchedules.setVisibility(View.GONE);
            rvMySchedules.setVisibility(View.VISIBLE);

            DoctorScheduleAdapter adapter = new DoctorScheduleAdapter(mySlots, this::confirmDeleteSlot);
            rvMySchedules.setAdapter(adapter);
        }
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dpd = new DatePickerDialog(this, (view, y, m, d) -> {
            selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d);
            tvSelectedDate.setText(selectedDate);
        }, year, month, day);

        // Prevenir fechas pasadas
        dpd.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        dpd.show();
    }

    private void showTimePicker() {
        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        TimePickerDialog tpd = new TimePickerDialog(this, (view, h, m) -> {
            selectedTime = String.format(Locale.getDefault(), "%02d:%02d", h, m);
            tvSelectedTime.setText(selectedTime);
        }, hour, minute, true);
        tpd.show();
    }

    private void addScheduleSlot() {
        if (hospitals == null || hospitals.isEmpty()) {
            Toast.makeText(this, "No hay hospitales disponibles.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedDate.isEmpty()) {
            Toast.makeText(this, "Debe seleccionar una fecha.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedTime.isEmpty()) {
            Toast.makeText(this, "Debe seleccionar una hora.", Toast.LENGTH_SHORT).show();
            return;
        }

        int hospitalId = hospitals.get(spHospitals.getSelectedItemPosition()).getHospitalId();
        int doctorId = session.getUserId();

        Schedule s = new Schedule();
        s.setDoctorId(doctorId);
        s.setHospitalId(hospitalId);
        s.setDate(selectedDate);
        s.setTime(selectedTime);
        s.setAvailable(true);

        long result = scheduleDAO.insertSchedule(s);
        if (result > 0) {
            Toast.makeText(this, "✅ Horario agregado con éxito.", Toast.LENGTH_SHORT).show();
            // Resetear selección
            selectedDate = "";
            selectedTime = "";
            tvSelectedDate.setText("Sin seleccionar");
            tvSelectedTime.setText("Sin seleccionar");
            loadSchedules();
        } else {
            Toast.makeText(this, "Error al agregar horario.", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmDeleteSlot(Schedule slot) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar horario")
                .setMessage(String.format("¿Está seguro de que desea eliminar el horario del %s a las %s?",
                        slot.getDate(), slot.getTime()))
                .setPositiveButton("Sí, eliminar", (d, w) -> {
                    boolean ok = scheduleDAO.deleteSchedule(slot.getScheduleId());
                    if (ok) {
                        Toast.makeText(this, "Horario eliminado.", Toast.LENGTH_SHORT).show();
                        loadSchedules();
                    } else {
                        Toast.makeText(this, "No se pudo eliminar (puede que ya esté reservado).", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
