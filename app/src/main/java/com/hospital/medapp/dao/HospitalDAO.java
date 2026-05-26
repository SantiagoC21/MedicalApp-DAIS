package com.hospital.medapp.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.hospital.medapp.database.DatabaseHelper;
import com.hospital.medapp.models.Hospital;
import com.hospital.medapp.models.Schedule;

import java.util.ArrayList;
import java.util.List;

// ══════════════════════════════════════════════════════════════════════════════
// HospitalDAO
// ══════════════════════════════════════════════════════════════════════════════
public class HospitalDAO {

    private final DatabaseHelper dbHelper;

    public HospitalDAO(Context context) {
        dbHelper = DatabaseHelper.getInstance(context);
    }

    // ─── SELECT ───────────────────────────────────────────────────────────────

    /** Todos los hospitales ordenados por nombre. */
    public List<Hospital> getAllHospitals() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Hospital> list = new ArrayList<>();
        Cursor c = db.query(DatabaseHelper.TABLE_HOSPITALS,
                null, null, null, null, null,
                DatabaseHelper.COL_HOSP_NAME + " ASC");
        while (c.moveToNext()) list.add(cursorToHospital(c));
        c.close();
        return list;
    }

    /** Hospital por ID. */
    public Hospital getHospitalById(int hospitalId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(DatabaseHelper.TABLE_HOSPITALS, null,
                DatabaseHelper.COL_HOSP_ID + "=?",
                new String[]{String.valueOf(hospitalId)},
                null, null, null);
        Hospital h = null;
        if (c.moveToFirst()) h = cursorToHospital(c);
        c.close();
        return h;
    }

    /** Hospitales filtrados por especialidad. */
    public List<Hospital> getBySpecialty(String specialty) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Hospital> list = new ArrayList<>();
        Cursor c = db.query(DatabaseHelper.TABLE_HOSPITALS, null,
                DatabaseHelper.COL_HOSP_SPECIALTY + " LIKE ?",
                new String[]{"%" + specialty + "%"},
                null, null, DatabaseHelper.COL_HOSP_NAME + " ASC");
        while (c.moveToNext()) list.add(cursorToHospital(c));
        c.close();
        return list;
    }

    // ─── INSERT / UPDATE ──────────────────────────────────────────────────────

    public long insertHospital(Hospital h) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.insert(DatabaseHelper.TABLE_HOSPITALS, null, buildCV(h));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private ContentValues buildCV(Hospital h) {
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_HOSP_NAME,      h.getName());
        cv.put(DatabaseHelper.COL_HOSP_ADDRESS,   h.getAddress());
        cv.put(DatabaseHelper.COL_HOSP_PHONE,     h.getPhone());
        cv.put(DatabaseHelper.COL_HOSP_SPECIALTY, h.getSpecialty());
        cv.put(DatabaseHelper.COL_HOSP_LATITUDE,  h.getLatitude());
        cv.put(DatabaseHelper.COL_HOSP_LONGITUDE, h.getLongitude());
        return cv;
    }

    private Hospital cursorToHospital(Cursor c) {
        Hospital h = new Hospital();
        h.setHospitalId(c.getInt   (c.getColumnIndexOrThrow(DatabaseHelper.COL_HOSP_ID)));
        h.setName      (c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_HOSP_NAME)));
        h.setAddress   (c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_HOSP_ADDRESS)));
        h.setPhone     (c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_HOSP_PHONE)));
        h.setSpecialty (c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_HOSP_SPECIALTY)));
        h.setLatitude  (c.getDouble(c.getColumnIndexOrThrow(DatabaseHelper.COL_HOSP_LATITUDE)));
        h.setLongitude (c.getDouble(c.getColumnIndexOrThrow(DatabaseHelper.COL_HOSP_LONGITUDE)));
        return h;
    }
}

