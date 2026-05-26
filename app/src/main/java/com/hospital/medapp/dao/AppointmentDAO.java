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
// AppointmentDAO
// ══════════════════════════════════════════════════════════════════════════════
public class AppointmentDAO {

    private final DatabaseHelper dbHelper;
    private final ScheduleDAO    scheduleDAO;

    public AppointmentDAO(Context context) {
        dbHelper    = DatabaseHelper.getInstance(context);
        scheduleDAO = new ScheduleDAO(context);
    }

    // ─── INSERT ───────────────────────────────────────────────────────────────

    /**
     * Reserva una cita y marca la franja como no disponible de forma atómica.
     * @return el ID de la cita, o -1 si falló.
     */
    public long bookAppointment(Appointment appt) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long id = -1;
        db.beginTransaction();
        try {
            ContentValues cv = new ContentValues();
            cv.put(DatabaseHelper.COL_APPT_PATIENT_ID,  appt.getPatientId());
            cv.put(DatabaseHelper.COL_APPT_DOCTOR_ID,   appt.getDoctorId());
            cv.put(DatabaseHelper.COL_APPT_HOSPITAL_ID, appt.getHospitalId());
            cv.put(DatabaseHelper.COL_APPT_SCHEDULE_ID, appt.getScheduleId());
            cv.put(DatabaseHelper.COL_APPT_STATUS,      Appointment.Status.CONFIRMED.name());
            cv.put(DatabaseHelper.COL_APPT_REASON,      appt.getReason());
            id = db.insert(DatabaseHelper.TABLE_APPOINTMENTS, null, cv);
            if (id > 0) {
                scheduleDAO.markUnavailable(appt.getScheduleId());
                db.setTransactionSuccessful();
            }
        } finally {
            db.endTransaction();
        }
        return id;
    }

    // ─── SELECT ───────────────────────────────────────────────────────────────

    /** Citas de un paciente con datos completos (JOIN). */
    public List<Appointment> getAppointmentsByPatient(int patientId) {
        return queryWithJoin(DatabaseHelper.COL_APPT_PATIENT_ID, patientId);
    }

    /** Citas asignadas a un médico con datos completos (JOIN). */
    public List<Appointment> getAppointmentsByDoctor(int doctorId) {
        return queryWithJoin(DatabaseHelper.COL_APPT_DOCTOR_ID, doctorId);
    }

    /** Citas confirmadas de un médico que aún no tienen triaje (para selección presencial). */
    public List<Appointment> getPendingTriageForDoctor(int doctorId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Appointment> list = new ArrayList<>();
        // Citas CONFIRMED que no tienen entrada en triage
        String sql =
            "SELECT a.*, "
            + "  p." + DatabaseHelper.COL_USER_NAME + " AS patient_name, "
            + "  h." + DatabaseHelper.COL_HOSP_NAME + " AS hospital_name, "
            + "  s." + DatabaseHelper.COL_SCHED_DATE + " AS sched_date, "
            + "  s." + DatabaseHelper.COL_SCHED_TIME + " AS sched_time "
            + "FROM " + DatabaseHelper.TABLE_APPOINTMENTS + " a "
            + "JOIN " + DatabaseHelper.TABLE_USERS         + " p ON a." + DatabaseHelper.COL_APPT_PATIENT_ID + " = p." + DatabaseHelper.COL_USER_ID
            + " JOIN " + DatabaseHelper.TABLE_HOSPITALS    + " h ON a." + DatabaseHelper.COL_APPT_HOSPITAL_ID + " = h." + DatabaseHelper.COL_HOSP_ID
            + " JOIN " + DatabaseHelper.TABLE_SCHEDULES    + " s ON a." + DatabaseHelper.COL_APPT_SCHEDULE_ID + " = s." + DatabaseHelper.COL_SCHED_ID
            + " WHERE a." + DatabaseHelper.COL_APPT_DOCTOR_ID + " = ?"
            + "   AND a." + DatabaseHelper.COL_APPT_STATUS    + " = 'CONFIRMED'"
            + "   AND a." + DatabaseHelper.COL_APPT_ID        + " NOT IN ("
            + "       SELECT DISTINCT " + DatabaseHelper.COL_TRIAGE_APPT_ID
            + "       FROM " + DatabaseHelper.TABLE_TRIAGE
            + "       WHERE " + DatabaseHelper.COL_TRIAGE_APPT_ID + " IS NOT NULL)"
            + " ORDER BY s." + DatabaseHelper.COL_SCHED_DATE + ", s." + DatabaseHelper.COL_SCHED_TIME;

        Cursor c = db.rawQuery(sql, new String[]{String.valueOf(doctorId)});
        while (c.moveToNext()) list.add(buildAppointmentFromCursor(c));
        c.close();
        return list;
    }

    /** Obtiene una cita por su ID. */
    public Appointment getAppointmentById(int appointmentId) {
        List<Appointment> list = queryWithJoin(DatabaseHelper.COL_APPT_ID, appointmentId);
        return list.isEmpty() ? null : list.get(0);
    }

    // ─── UPDATE ───────────────────────────────────────────────────────────────

    /**
     * Cancela una cita y libera la franja horaria de forma atómica.
     */
    public boolean cancelAppointment(int appointmentId) {
        Appointment appt = getAppointmentById(appointmentId);
        if (appt == null) return false;

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        boolean ok = false;
        db.beginTransaction();
        try {
            ContentValues cv = new ContentValues();
            cv.put(DatabaseHelper.COL_APPT_STATUS, Appointment.Status.CANCELLED.name());
            int rows = db.update(DatabaseHelper.TABLE_APPOINTMENTS, cv,
                    DatabaseHelper.COL_APPT_ID + "=?",
                    new String[]{String.valueOf(appointmentId)});
            if (rows > 0) {
                scheduleDAO.markAvailable(appt.getScheduleId());
                db.setTransactionSuccessful();
                ok = true;
            }
        } finally {
            db.endTransaction();
        }
        return ok;
    }

    public boolean completeAppointment(int appointmentId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_APPT_STATUS, Appointment.Status.COMPLETED.name());
        return db.update(DatabaseHelper.TABLE_APPOINTMENTS, cv,
                DatabaseHelper.COL_APPT_ID + "=?",
                new String[]{String.valueOf(appointmentId)}) > 0;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private List<Appointment> queryWithJoin(String filterCol, int filterValue) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Appointment> list = new ArrayList<>();
        String sql =
            "SELECT a.*, "
            + "  p." + DatabaseHelper.COL_USER_NAME + " AS patient_name, "
            + "  d." + DatabaseHelper.COL_USER_NAME + " AS doctor_name, "
            + "  h." + DatabaseHelper.COL_HOSP_NAME + " AS hospital_name, "
            + "  s." + DatabaseHelper.COL_SCHED_DATE + " AS sched_date, "
            + "  s." + DatabaseHelper.COL_SCHED_TIME + " AS sched_time "
            + "FROM " + DatabaseHelper.TABLE_APPOINTMENTS + " a "
            + "JOIN "  + DatabaseHelper.TABLE_USERS       + " p ON a." + DatabaseHelper.COL_APPT_PATIENT_ID  + " = p." + DatabaseHelper.COL_USER_ID
            + " JOIN " + DatabaseHelper.TABLE_USERS       + " d ON a." + DatabaseHelper.COL_APPT_DOCTOR_ID   + " = d." + DatabaseHelper.COL_USER_ID
            + " JOIN " + DatabaseHelper.TABLE_HOSPITALS   + " h ON a." + DatabaseHelper.COL_APPT_HOSPITAL_ID + " = h." + DatabaseHelper.COL_HOSP_ID
            + " JOIN " + DatabaseHelper.TABLE_SCHEDULES   + " s ON a." + DatabaseHelper.COL_APPT_SCHEDULE_ID + " = s." + DatabaseHelper.COL_SCHED_ID
            + " WHERE a." + filterCol + " = ?"
            + " ORDER BY s." + DatabaseHelper.COL_SCHED_DATE + " DESC, s." + DatabaseHelper.COL_SCHED_TIME;

        Cursor c = db.rawQuery(sql, new String[]{String.valueOf(filterValue)});
        while (c.moveToNext()) list.add(buildAppointmentFromCursor(c));
        c.close();
        return list;
    }

    private Appointment buildAppointmentFromCursor(Cursor c) {
        Appointment a = new Appointment();
        a.setAppointmentId(c.getInt   (c.getColumnIndexOrThrow(DatabaseHelper.COL_APPT_ID)));
        a.setPatientId    (c.getInt   (c.getColumnIndexOrThrow(DatabaseHelper.COL_APPT_PATIENT_ID)));
        a.setDoctorId     (c.getInt   (c.getColumnIndexOrThrow(DatabaseHelper.COL_APPT_DOCTOR_ID)));
        a.setHospitalId   (c.getInt   (c.getColumnIndexOrThrow(DatabaseHelper.COL_APPT_HOSPITAL_ID)));
        a.setScheduleId   (c.getInt   (c.getColumnIndexOrThrow(DatabaseHelper.COL_APPT_SCHEDULE_ID)));
        a.setStatus       (Appointment.Status.valueOf(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_APPT_STATUS))));
        a.setReason       (c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_APPT_REASON)));
        a.setCreatedAt    (c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_APPT_CREATED_AT)));
        // Joined fields
        int piIdx = c.getColumnIndex("patient_name");
        int diIdx = c.getColumnIndex("doctor_name");
        int hiIdx = c.getColumnIndex("hospital_name");
        int sdIdx = c.getColumnIndex("sched_date");
        int stIdx = c.getColumnIndex("sched_time");
        if (piIdx >= 0) a.setPatientName (c.getString(piIdx));
        if (diIdx >= 0) a.setDoctorName  (c.getString(diIdx));
        if (hiIdx >= 0) a.setHospitalName(c.getString(hiIdx));
        if (sdIdx >= 0) a.setScheduleDate(c.getString(sdIdx));
        if (stIdx >= 0) a.setScheduleTime(c.getString(stIdx));
        return a;
    }
}

