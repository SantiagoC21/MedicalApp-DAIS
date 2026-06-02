package com.hospital.medapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hospital.medapp.R;
import com.hospital.medapp.models.Schedule;

import java.util.List;

public class DoctorScheduleAdapter extends RecyclerView.Adapter<DoctorScheduleAdapter.VH> {

    public interface OnDeleteClick { void onDelete(Schedule slot); }

    private final List<Schedule> slots;
    private final OnDeleteClick  listener;

    public DoctorScheduleAdapter(List<Schedule> slots, OnDeleteClick listener) {
        this.slots    = slots;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_doctor_schedule, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Schedule s = slots.get(pos);
        h.tvHospitalName.setText(s.getHospitalName() != null ? s.getHospitalName() : "Hospital");
        h.tvDateTime.setText(s.getDate() + "  -  " + s.getTime());

        if (s.isAvailable()) {
            h.tvStatusBooked.setVisibility(View.GONE);
            h.btnDeleteSlot.setVisibility(View.VISIBLE);
            h.btnDeleteSlot.setOnClickListener(v -> listener.onDelete(s));
        } else {
            h.tvStatusBooked.setVisibility(View.VISIBLE);
            h.btnDeleteSlot.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return slots.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvHospitalName, tvDateTime, tvStatusBooked;
        Button btnDeleteSlot;

        VH(View v) {
            super(v);
            tvHospitalName = v.findViewById(R.id.tvHospitalName);
            tvDateTime     = v.findViewById(R.id.tvDateTime);
            tvStatusBooked = v.findViewById(R.id.tvStatusBooked);
            btnDeleteSlot  = v.findViewById(R.id.btnDeleteSlot);
        }
    }
}
