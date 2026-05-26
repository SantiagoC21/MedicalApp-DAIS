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
// ScheduleAdapter – franjas horarias disponibles
// ══════════════════════════════════════════════════════════════════════════════
public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.VH> {

    public interface OnSlotClick { void onClick(Schedule slot); }

    private final List<Schedule> slots;
    private final OnSlotClick    listener;

    public ScheduleAdapter(List<Schedule> slots, OnSlotClick listener) {
        this.slots    = slots;
        this.listener = listener;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_schedule, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Schedule s = slots.get(pos);
        h.tvTime  .setText(s.getTime());
        h.tvDoctor.setText("Dr./Dra. " + s.getDoctorName());
        h.btnBook .setOnClickListener(v -> listener.onClick(s));
    }

    @Override public int getItemCount() { return slots.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTime, tvDoctor;
        Button   btnBook;
        VH(View v) {
            super(v);
            tvTime  = v.findViewById(R.id.tvSlotTime);
            tvDoctor= v.findViewById(R.id.tvSlotDoctor);
            btnBook = v.findViewById(R.id.btnBookSlot);
        }
    }
}
