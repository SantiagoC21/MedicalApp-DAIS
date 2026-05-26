package com.hospital.medapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hospital.medapp.R;
import com.hospital.medapp.models.Hospital;

import java.util.List;

// ══════════════════════════════════════════════════════════════════════════════
// HospitalAdapter
// ══════════════════════════════════════════════════════════════════════════════
public class HospitalAdapter extends RecyclerView.Adapter<HospitalAdapter.VH> {

    public interface OnHospitalClick { void onClick(Hospital hospital); }

    private final List<Hospital>   hospitals;
    private final OnHospitalClick  listener;

    public HospitalAdapter(List<Hospital> hospitals, OnHospitalClick listener) {
        this.hospitals = hospitals;
        this.listener  = listener;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_hospital, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Hospital hosp = hospitals.get(pos);
        h.tvName     .setText(hosp.getName());
        h.tvSpecialty.setText(hosp.getSpecialty());
        h.tvAddress  .setText(hosp.getAddress());
        h.itemView.setOnClickListener(v -> listener.onClick(hosp));
    }

    @Override public int getItemCount() { return hospitals.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvSpecialty, tvAddress;
        VH(View v) {
            super(v);
            tvName      = v.findViewById(R.id.tvHospName);
            tvSpecialty = v.findViewById(R.id.tvSpecialty);
            tvAddress   = v.findViewById(R.id.tvAddress);
        }
    }
}
