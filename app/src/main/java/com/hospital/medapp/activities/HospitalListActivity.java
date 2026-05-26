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
public class HospitalListActivity extends AppCompatActivity {

    private RecyclerView rvHospitals;
    private HospitalDAO  hospitalDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hospital_list);

        hospitalDAO  = new HospitalDAO(this);
        rvHospitals  = findViewById(R.id.rvHospitals);
        rvHospitals.setLayoutManager(new LinearLayoutManager(this));

        Spinner spinSpecialty = findViewById(R.id.spinSpecialty);
        Button  btnFilter     = findViewById(R.id.btnFilter);

        // Cargar especialidades únicas
        String[] specialties = {"Todas", "Medicina General", "Pediatría",
                "Seguro Social", "Cardiología", "Neurología"};
        ArrayAdapter<String> spinAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, specialties);
        spinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinSpecialty.setAdapter(spinAdapter);

        btnFilter.setOnClickListener(v -> {
            String selected = spinSpecialty.getSelectedItem().toString();
            loadHospitals(selected.equals("Todas") ? null : selected);
        });

        loadHospitals(null);
    }

    private void loadHospitals(String specialty) {
        List<Hospital> hospitals = (specialty == null)
                ? hospitalDAO.getAllHospitals()
                : hospitalDAO.getBySpecialty(specialty);

        com.hospital.medapp.adapters.HospitalAdapter adapter =
                new com.hospital.medapp.adapters.HospitalAdapter(hospitals, hospital -> {
                    // Al seleccionar hospital → ir a reservar cita
                    Intent i = new Intent(this, AppointmentActivity.class);
                    i.putExtra(AppointmentActivity.EXTRA_HOSPITAL_ID,   hospital.getHospitalId());
                    i.putExtra(AppointmentActivity.EXTRA_HOSPITAL_NAME, hospital.getName());
                    startActivity(i);
                });
        rvHospitals.setAdapter(adapter);
    }
}
