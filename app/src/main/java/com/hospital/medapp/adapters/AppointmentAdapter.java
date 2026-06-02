package com.hospital.medapp.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hospital.medapp.R;
import com.hospital.medapp.models.Appointment;
import com.hospital.medapp.models.Hospital;
import com.hospital.medapp.models.Schedule;

import java.util.List;

// ══════════════════════════════════════════════════════════════════════════════
// AppointmentAdapter  – lista de citas (vista paciente o médico)
// ══════════════════════════════════════════════════════════════════════════════
public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.VH> {

    public interface OnActionClick { void onAction(Appointment appt); }

    private final List<Appointment> appointments;
    private final OnActionClick     actionListener;
    private final boolean           isDoctorView;

    public AppointmentAdapter(List<Appointment> appointments,
                       OnActionClick actionListener,
                       boolean isDoctorView) {
        this.appointments   = appointments;
        this.actionListener = actionListener;
        this.isDoctorView   = isDoctorView;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_appointment, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Appointment a = appointments.get(pos);

        h.tvPerson  .setText(isDoctorView
                ? "Paciente: "  + a.getPatientName()
                : "Médico: Dr./Dra. " + a.getDoctorName());
        h.tvHospital.setText("🏥 " + a.getHospitalName());
        h.tvDateTime.setText("📅 " + a.getScheduleDate() + "  ⏰ " + a.getScheduleTime());
        h.tvStatus  .setText(a.getStatus().name());

        // Color y fondo del estado (Badge adaptativo moderno)
        boolean isDarkMode = (h.itemView.getContext().getResources().getConfiguration().uiMode 
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK) 
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;

        int textColor;
        int bgColor;
        
        if (isDarkMode) {
            switch (a.getStatus()) {
                case CONFIRMED:
                    textColor = Color.parseColor("#A5D6A7");
                    bgColor = Color.parseColor("#1B5E20");
                    break;
                case CANCELLED:
                    textColor = Color.parseColor("#EF9A9A");
                    bgColor = Color.parseColor("#C62828");
                    break;
                case COMPLETED:
                    textColor = Color.parseColor("#90CAF9");
                    bgColor = Color.parseColor("#1565C0");
                    break;
                default:
                    textColor = Color.parseColor("#CFD8DC");
                    bgColor = Color.parseColor("#37474F");
                    break;
            }
        } else {
            switch (a.getStatus()) {
                case CONFIRMED:
                    textColor = Color.parseColor("#1B5E20");
                    bgColor = Color.parseColor("#E8F5E9");
                    break;
                case CANCELLED:
                    textColor = Color.parseColor("#C62828");
                    bgColor = Color.parseColor("#FFEBEE");
                    break;
                case COMPLETED:
                    textColor = Color.parseColor("#1565C0");
                    bgColor = Color.parseColor("#E3F2FD");
                    break;
                default:
                    textColor = Color.parseColor("#37474F");
                    bgColor = Color.parseColor("#ECEFF1");
                    break;
            }
        }
        
        h.tvStatus.setTextColor(textColor);
        h.tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(bgColor));

        // Botón de acción contextual
        if (isDoctorView) {
            if (a.getStatus() == Appointment.Status.CONFIRMED) {
                h.btnAction.setText("Iniciar triaje");
                h.btnAction.setVisibility(View.VISIBLE);
                h.btnAction.setOnClickListener(v -> actionListener.onAction(a));
            } else {
                h.btnAction.setVisibility(View.GONE);
            }
        } else {
            if (a.getStatus() == Appointment.Status.CONFIRMED
                    || a.getStatus() == Appointment.Status.PENDING) {
                h.btnAction.setText("Cancelar cita");
                h.btnAction.setVisibility(View.VISIBLE);
                h.btnAction.setOnClickListener(v -> actionListener.onAction(a));
            } else {
                h.btnAction.setVisibility(View.GONE);
            }
        }
    }

    @Override public int getItemCount() { return appointments.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvPerson, tvHospital, tvDateTime, tvStatus;
        Button   btnAction;
        VH(View v) {
            super(v);
            tvPerson   = v.findViewById(R.id.tvApptPerson);
            tvHospital = v.findViewById(R.id.tvApptHospital);
            tvDateTime = v.findViewById(R.id.tvApptDateTime);
            tvStatus   = v.findViewById(R.id.tvApptStatus);
            btnAction  = v.findViewById(R.id.btnApptAction);
        }
    }
}
