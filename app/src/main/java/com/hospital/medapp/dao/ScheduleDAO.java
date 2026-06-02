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
// ScheduleDAO  — franjas horarias disponibles
// ══════════════════════════════════════════════════════════════════════════════
public class ScheduleDAO {

    private final DatabaseHelper dbHelper;

    public ScheduleDAO(Context context) {
        dbHelper = DatabaseHelper.getInstance(context);
    }

    // ─── INSERT ───────────────────────────────────────────────────────────────

    public long insertSchedule(Schedule s) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_SCHED_DOCTOR_ID,   s.getDoctorId());
        cv.put(DatabaseHelper.COL_SCHED_HOSPITAL_ID, s.getHospitalId());
        cv.put(DatabaseHelper.COL_SCHED_DATE,        s.getDate());
        cv.put(DatabaseHelper.COL_SCHED_TIME,        s.getTime());
        cv.put(DatabaseHelper.COL_SCHED_AVAILABLE,   s.isAvailable() ? 1 : 0);
        return db.insert(DatabaseHelper.TABLE_SCHEDULES, null, cv);
    }

    // ─── SELECT ───────────────────────────────────────────────────────────────

    /**
     * Franjas DISPONIBLES de un hospital en una fecha dada.
     * Hace JOIN con users para devolver el nombre del médico.
     */
    public List<Schedule> getAvailableSlots(int hospitalId, String date) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Schedule> list = new ArrayList<>();

        String sql = "SELECT s.*, u." + DatabaseHelper.COL_USER_NAME + " AS doctor_name "
                + "FROM "  + DatabaseHelper.TABLE_SCHEDULES + " s "
                + "JOIN "  + DatabaseHelper.TABLE_USERS     + " u "
                + "  ON s." + DatabaseHelper.COL_SCHED_DOCTOR_ID + " = u." + DatabaseHelper.COL_USER_ID
                + " WHERE s." + DatabaseHelper.COL_SCHED_HOSPITAL_ID + " = ?"
                + "   AND s." + DatabaseHelper.COL_SCHED_DATE        + " = ?"
                + "   AND s." + DatabaseHelper.COL_SCHED_AVAILABLE    + " = 1"
                + " ORDER BY s." + DatabaseHelper.COL_SCHED_TIME;

        Cursor c = db.rawQuery(sql, new String[]{
                String.valueOf(hospitalId), date});

        while (c.moveToNext()) {
            Schedule s = cursorToSchedule(c);
            s.setDoctorName(c.getString(c.getColumnIndexOrThrow("doctor_name")));
            list.add(s);
        }
        c.close();
        return list;
    }

    /** Horarios de un médico específico (para que él los vea). */
    public List<Schedule> getSchedulesByDoctor(int doctorId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Schedule> list = new ArrayList<>();
        String sql = "SELECT s.*, h." + DatabaseHelper.COL_HOSP_NAME + " AS hospital_name "
                + "FROM " + DatabaseHelper.TABLE_SCHEDULES + " s "
                + "JOIN " + DatabaseHelper.TABLE_HOSPITALS  + " h "
                + "  ON s." + DatabaseHelper.COL_SCHED_HOSPITAL_ID + " = h." + DatabaseHelper.COL_HOSP_ID
                + " WHERE s." + DatabaseHelper.COL_SCHED_DOCTOR_ID + " = ?"
                + " ORDER BY s." + DatabaseHelper.COL_SCHED_DATE + " DESC, s." + DatabaseHelper.COL_SCHED_TIME + " DESC";

        Cursor c = db.rawQuery(sql, new String[]{String.valueOf(doctorId)});
        while (c.moveToNext()) {
            Schedule s = cursorToSchedule(c);
            int idx = c.getColumnIndex("hospital_name");
            if (idx >= 0) s.setHospitalName(c.getString(idx));
            list.add(s);
        }
        c.close();
        return list;
    }

    /** Marca una franja como NO disponible al reservarla. */
    public boolean markUnavailable(int scheduleId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_SCHED_AVAILABLE, 0);
        int rows = db.update(DatabaseHelper.TABLE_SCHEDULES, cv,
                DatabaseHelper.COL_SCHED_ID + "=?",
                new String[]{String.valueOf(scheduleId)});
        return rows > 0;
    }

    /** Libera la franja si la cita es cancelada. */
    public boolean markAvailable(int scheduleId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_SCHED_AVAILABLE, 1);
        int rows = db.update(DatabaseHelper.TABLE_SCHEDULES, cv,
                DatabaseHelper.COL_SCHED_ID + "=?",
                new String[]{String.valueOf(scheduleId)});
        return rows > 0;
    }

    /** Elimina una franja horaria si no está reservada (mantiene la integridad referencial) */
    public boolean deleteSchedule(int scheduleId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rows = db.delete(DatabaseHelper.TABLE_SCHEDULES,
                DatabaseHelper.COL_SCHED_ID + "=? AND " + DatabaseHelper.COL_SCHED_AVAILABLE + "=1",
                new String[]{String.valueOf(scheduleId)});
        return rows > 0;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Schedule cursorToSchedule(Cursor c) {
        Schedule s = new Schedule();
        s.setScheduleId (c.getInt   (c.getColumnIndexOrThrow(DatabaseHelper.COL_SCHED_ID)));
        s.setDoctorId   (c.getInt   (c.getColumnIndexOrThrow(DatabaseHelper.COL_SCHED_DOCTOR_ID)));
        s.setHospitalId (c.getInt   (c.getColumnIndexOrThrow(DatabaseHelper.COL_SCHED_HOSPITAL_ID)));
        s.setDate       (c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_SCHED_DATE)));
        s.setTime       (c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_SCHED_TIME)));
        s.setAvailable  (c.getInt   (c.getColumnIndexOrThrow(DatabaseHelper.COL_SCHED_AVAILABLE)) == 1);
        return s;
    }
}
