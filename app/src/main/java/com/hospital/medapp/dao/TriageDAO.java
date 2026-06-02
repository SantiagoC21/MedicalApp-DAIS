package com.hospital.medapp.dao;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.hospital.medapp.database.DatabaseHelper;
import com.hospital.medapp.models.Appointment;
import com.hospital.medapp.models.Triage;

import java.util.ArrayList;
import java.util.List;

// ══════════════════════════════════════════════════════════════════════════════
// TriageDAO
// ══════════════════════════════════════════════════════════════════════════════
public class TriageDAO {

    private final DatabaseHelper dbHelper;
    private final AppointmentDAO  appointmentDAO;

    public TriageDAO(Context context) {
        dbHelper       = DatabaseHelper.getInstance(context);
        appointmentDAO = new AppointmentDAO(context);
    }

    // ─── INSERT ───────────────────────────────────────────────────────────────

    /**
     * Guarda un registro de triaje.
     * Si tiene appointmentId, también marca la cita como COMPLETED.
     */
    public long saveTriage(Triage t) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long id = -1;
        db.beginTransaction();
        try {
            // Calcular prioridad automáticamente
            t.setPriority(Triage.calculatePriority(
                    t.getHeartRateBpm(), t.getSpo2Percent(), t.getTemperatureC()));

            ContentValues cv = buildCV(t);
            id = db.insert(DatabaseHelper.TABLE_TRIAGE, null, cv);

            if (id > 0 && t.getAppointmentId() > 0) {
                appointmentDAO.completeAppointment(t.getAppointmentId());
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return id;
    }

    // ─── SELECT ───────────────────────────────────────────────────────────────

    /** Historial de triajes de un paciente (más reciente primero). */
    public List<Triage> getTriageByPatient(int patientId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Triage> list = new ArrayList<>();
        String sql =
                "SELECT t.*, "
                        + "  d." + DatabaseHelper.COL_USER_NAME + " AS doctor_name "
                        + "FROM " + DatabaseHelper.TABLE_TRIAGE + " t "
                        + "LEFT JOIN " + DatabaseHelper.TABLE_USERS + " d ON t." + DatabaseHelper.COL_TRIAGE_DOCTOR_ID + " = d." + DatabaseHelper.COL_USER_ID
                        + " WHERE t." + DatabaseHelper.COL_TRIAGE_PATIENT_ID + " = ?"
                        + " ORDER BY t." + DatabaseHelper.COL_TRIAGE_DATE + " DESC";
        Cursor c = db.rawQuery(sql, new String[]{String.valueOf(patientId)});
        while (c.moveToNext()) {
            Triage tr = cursorToTriage(c);
            int dnIdx = c.getColumnIndex("doctor_name");
            if (dnIdx >= 0) tr.setDoctorName(c.getString(dnIdx));
            list.add(tr);
        }
        c.close();
        return list;
    }

    /** Triajes realizados por un médico. */
    public List<Triage> getTriageByDoctor(int doctorId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Triage> list = new ArrayList<>();
        String sql =
                "SELECT t.*, "
                        + "  p." + DatabaseHelper.COL_USER_NAME + " AS patient_name "
                        + "FROM " + DatabaseHelper.TABLE_TRIAGE + " t "
                        + "JOIN " + DatabaseHelper.TABLE_USERS  + " p ON t." + DatabaseHelper.COL_TRIAGE_PATIENT_ID + " = p." + DatabaseHelper.COL_USER_ID
                        + " WHERE t." + DatabaseHelper.COL_TRIAGE_DOCTOR_ID + " = ?"
                        + " ORDER BY t." + DatabaseHelper.COL_TRIAGE_DATE + " DESC";
        Cursor c = db.rawQuery(sql, new String[]{String.valueOf(doctorId)});
        while (c.moveToNext()) {
            Triage tr = cursorToTriage(c);
            int pnIdx = c.getColumnIndex("patient_name");
            if (pnIdx >= 0) tr.setPatientName(c.getString(pnIdx));
            list.add(tr);
        }
        c.close();
        return list;
    }

    /** Último triaje de un paciente. */
    public Triage getLatestTriageByPatient(int patientId) {
        List<Triage> list = getTriageByPatient(patientId);
        return list.isEmpty() ? null : list.get(0);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private ContentValues buildCV(Triage t) {
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_TRIAGE_PATIENT_ID, t.getPatientId());
        cv.put(DatabaseHelper.COL_TRIAGE_DOCTOR_ID,  t.getDoctorId() > 0 ? t.getDoctorId() : null);
        cv.put(DatabaseHelper.COL_TRIAGE_APPT_ID,    t.getAppointmentId() > 0 ? t.getAppointmentId() : null);
        cv.put(DatabaseHelper.COL_TRIAGE_BPM,        t.getHeartRateBpm());
        cv.put(DatabaseHelper.COL_TRIAGE_SPO2,       t.getSpo2Percent());
        cv.put(DatabaseHelper.COL_TRIAGE_TEMP,       t.getTemperatureC());
        cv.put(DatabaseHelper.COL_TRIAGE_SYS_BP,     t.getSystolicBp());
        cv.put(DatabaseHelper.COL_TRIAGE_DIA_BP,     t.getDiastolicBp());
        cv.put(DatabaseHelper.COL_TRIAGE_NOTES,      t.getNotes());
        cv.put(DatabaseHelper.COL_TRIAGE_PRIORITY,   t.getPriority().name());
        cv.put(DatabaseHelper.COL_TRIAGE_TYPE,       t.getTriageType().name());
        return cv;
    }

    private Triage cursorToTriage(Cursor c) {
        Triage t = new Triage();
        t.setTriageId    (c.getInt   (c.getColumnIndexOrThrow(DatabaseHelper.COL_TRIAGE_ID)));
        t.setPatientId   (c.getInt   (c.getColumnIndexOrThrow(DatabaseHelper.COL_TRIAGE_PATIENT_ID)));
        t.setDoctorId    (c.getInt   (c.getColumnIndexOrThrow(DatabaseHelper.COL_TRIAGE_DOCTOR_ID)));
        t.setHeartRateBpm(c.getDouble(c.getColumnIndexOrThrow(DatabaseHelper.COL_TRIAGE_BPM)));
        t.setSpo2Percent (c.getDouble(c.getColumnIndexOrThrow(DatabaseHelper.COL_TRIAGE_SPO2)));
        t.setTemperatureC(c.getDouble(c.getColumnIndexOrThrow(DatabaseHelper.COL_TRIAGE_TEMP)));
        t.setSystolicBp  (c.getInt   (c.getColumnIndexOrThrow(DatabaseHelper.COL_TRIAGE_SYS_BP)));
        t.setDiastolicBp (c.getInt   (c.getColumnIndexOrThrow(DatabaseHelper.COL_TRIAGE_DIA_BP)));
        t.setNotes       (c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TRIAGE_NOTES)));
        t.setPriority    (Triage.Priority.valueOf(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TRIAGE_PRIORITY))));
        t.setTriageType  (Triage.Type.valueOf    (c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TRIAGE_TYPE))));
        t.setTriageDate  (c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TRIAGE_DATE)));
        int apptIdx = c.getColumnIndex(DatabaseHelper.COL_TRIAGE_APPT_ID);
        if (apptIdx >= 0 && !c.isNull(apptIdx)) t.setAppointmentId(c.getInt(apptIdx));
        return t;
    }
}