package com.hospital.medapp.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "MedApp.db";
    private static final int DATABASE_VERSION = 1;
    private static DatabaseHelper instance;

    // ─── Tabla: users ────────────────────────────────────────────────────────
    public static final String TABLE_USERS           = "users";
    public static final String COL_USER_ID           = "user_id";
    public static final String COL_USER_NAME         = "name";
    public static final String COL_USER_EMAIL        = "email";
    public static final String COL_USER_PASSWORD     = "password";
    public static final String COL_USER_DNI          = "dni";
    public static final String COL_USER_PHONE        = "phone";
    public static final String COL_USER_ROLE         = "role";        // PATIENT | DOCTOR | ADMIN
    public static final String COL_USER_STATUS       = "status";      // ACTIVE | PENDING | SUSPENDED
    public static final String COL_USER_CREATED_AT   = "created_at";

    // ─── Tabla: hospitals ────────────────────────────────────────────────────
    public static final String TABLE_HOSPITALS       = "hospitals";
    public static final String COL_HOSP_ID           = "hospital_id";
    public static final String COL_HOSP_NAME         = "name";
    public static final String COL_HOSP_ADDRESS      = "address";
    public static final String COL_HOSP_PHONE        = "phone";
    public static final String COL_HOSP_SPECIALTY    = "specialty";
    public static final String COL_HOSP_LATITUDE     = "latitude";
    public static final String COL_HOSP_LONGITUDE    = "longitude";

    // ─── Tabla: schedules (horarios disponibles por médico/hospital) ─────────
    public static final String TABLE_SCHEDULES       = "schedules";
    public static final String COL_SCHED_ID          = "schedule_id";
    public static final String COL_SCHED_DOCTOR_ID   = "doctor_id";
    public static final String COL_SCHED_HOSPITAL_ID = "hospital_id";
    public static final String COL_SCHED_DATE        = "date";         // YYYY-MM-DD
    public static final String COL_SCHED_TIME        = "time";         // HH:MM
    public static final String COL_SCHED_AVAILABLE   = "available";    // 1 | 0

    // ─── Tabla: appointments ─────────────────────────────────────────────────
    public static final String TABLE_APPOINTMENTS    = "appointments";
    public static final String COL_APPT_ID           = "appointment_id";
    public static final String COL_APPT_PATIENT_ID   = "patient_id";
    public static final String COL_APPT_DOCTOR_ID    = "doctor_id";
    public static final String COL_APPT_HOSPITAL_ID  = "hospital_id";
    public static final String COL_APPT_SCHEDULE_ID  = "schedule_id";
    public static final String COL_APPT_STATUS       = "status";       // PENDING | CONFIRMED | CANCELLED | COMPLETED
    public static final String COL_APPT_REASON       = "reason";
    public static final String COL_APPT_CREATED_AT   = "created_at";

    // ─── Tabla: triage ───────────────────────────────────────────────────────
    public static final String TABLE_TRIAGE          = "triage";
    public static final String COL_TRIAGE_ID         = "triage_id";
    public static final String COL_TRIAGE_PATIENT_ID = "patient_id";
    public static final String COL_TRIAGE_DOCTOR_ID  = "doctor_id";
    public static final String COL_TRIAGE_APPT_ID    = "appointment_id";
    public static final String COL_TRIAGE_BPM        = "heart_rate_bpm";
    public static final String COL_TRIAGE_SPO2       = "spo2_percent";
    public static final String COL_TRIAGE_TEMP       = "temperature_c";
    public static final String COL_TRIAGE_SYS_BP     = "systolic_bp";
    public static final String COL_TRIAGE_DIA_BP     = "diastolic_bp";
    public static final String COL_TRIAGE_NOTES      = "notes";
    public static final String COL_TRIAGE_PRIORITY   = "priority";     // GREEN | YELLOW | ORANGE | RED
    public static final String COL_TRIAGE_TYPE       = "triage_type";  // REMOTE | PRESENTIAL
    public static final String COL_TRIAGE_DATE       = "triage_date";

    // ─── Tabla: doctor_documents ─────────────────────────────────────────────
    public static final String TABLE_DOCTOR_DOCS     = "doctor_documents";
    public static final String COL_DOC_ID            = "doc_id";
    public static final String COL_DOC_DOCTOR_ID     = "doctor_id";
    public static final String COL_DOC_TYPE          = "doc_type";     // TITLE | LICENSE | ID
    public static final String COL_DOC_PATH          = "file_path";
    public static final String COL_DOC_STATUS        = "validation_status"; // PENDING | APPROVED | REJECTED
    public static final String COL_DOC_UPLOADED_AT   = "uploaded_at";

    // ─── CREATE statements ───────────────────────────────────────────────────
    private static final String CREATE_USERS = "CREATE TABLE " + TABLE_USERS + " ("
            + COL_USER_ID        + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COL_USER_NAME      + " TEXT NOT NULL, "
            + COL_USER_EMAIL     + " TEXT NOT NULL UNIQUE, "
            + COL_USER_PASSWORD  + " TEXT NOT NULL, "
            + COL_USER_DNI       + " TEXT NOT NULL UNIQUE, "
            + COL_USER_PHONE     + " TEXT, "
            + COL_USER_ROLE      + " TEXT NOT NULL DEFAULT 'PATIENT', "
            + COL_USER_STATUS    + " TEXT NOT NULL DEFAULT 'ACTIVE', "
            + COL_USER_CREATED_AT+ " TEXT DEFAULT (datetime('now'))"
            + ");";

    private static final String CREATE_HOSPITALS = "CREATE TABLE " + TABLE_HOSPITALS + " ("
            + COL_HOSP_ID        + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COL_HOSP_NAME      + " TEXT NOT NULL, "
            + COL_HOSP_ADDRESS   + " TEXT, "
            + COL_HOSP_PHONE     + " TEXT, "
            + COL_HOSP_SPECIALTY + " TEXT, "
            + COL_HOSP_LATITUDE  + " REAL, "
            + COL_HOSP_LONGITUDE + " REAL"
            + ");";

    private static final String CREATE_SCHEDULES = "CREATE TABLE " + TABLE_SCHEDULES + " ("
            + COL_SCHED_ID          + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COL_SCHED_DOCTOR_ID   + " INTEGER NOT NULL, "
            + COL_SCHED_HOSPITAL_ID + " INTEGER NOT NULL, "
            + COL_SCHED_DATE        + " TEXT NOT NULL, "
            + COL_SCHED_TIME        + " TEXT NOT NULL, "
            + COL_SCHED_AVAILABLE   + " INTEGER NOT NULL DEFAULT 1, "
            + "FOREIGN KEY(" + COL_SCHED_DOCTOR_ID   + ") REFERENCES " + TABLE_USERS      + "(" + COL_USER_ID  + "), "
            + "FOREIGN KEY(" + COL_SCHED_HOSPITAL_ID + ") REFERENCES " + TABLE_HOSPITALS  + "(" + COL_HOSP_ID  + ")"
            + ");";

    private static final String CREATE_APPOINTMENTS = "CREATE TABLE " + TABLE_APPOINTMENTS + " ("
            + COL_APPT_ID           + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COL_APPT_PATIENT_ID   + " INTEGER NOT NULL, "
            + COL_APPT_DOCTOR_ID    + " INTEGER NOT NULL, "
            + COL_APPT_HOSPITAL_ID  + " INTEGER NOT NULL, "
            + COL_APPT_SCHEDULE_ID  + " INTEGER NOT NULL, "
            + COL_APPT_STATUS       + " TEXT NOT NULL DEFAULT 'PENDING', "
            + COL_APPT_REASON       + " TEXT, "
            + COL_APPT_CREATED_AT   + " TEXT DEFAULT (datetime('now')), "
            + "FOREIGN KEY(" + COL_APPT_PATIENT_ID   + ") REFERENCES " + TABLE_USERS     + "(" + COL_USER_ID  + "), "
            + "FOREIGN KEY(" + COL_APPT_DOCTOR_ID    + ") REFERENCES " + TABLE_USERS     + "(" + COL_USER_ID  + "), "
            + "FOREIGN KEY(" + COL_APPT_HOSPITAL_ID  + ") REFERENCES " + TABLE_HOSPITALS + "(" + COL_HOSP_ID  + "), "
            + "FOREIGN KEY(" + COL_APPT_SCHEDULE_ID  + ") REFERENCES " + TABLE_SCHEDULES + "(" + COL_SCHED_ID + ")"
            + ");";

    private static final String CREATE_TRIAGE = "CREATE TABLE " + TABLE_TRIAGE + " ("
            + COL_TRIAGE_ID          + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COL_TRIAGE_PATIENT_ID  + " INTEGER NOT NULL, "
            + COL_TRIAGE_DOCTOR_ID   + " INTEGER, "
            + COL_TRIAGE_APPT_ID     + " INTEGER, "
            + COL_TRIAGE_BPM         + " REAL, "
            + COL_TRIAGE_SPO2        + " REAL, "
            + COL_TRIAGE_TEMP        + " REAL, "
            + COL_TRIAGE_SYS_BP      + " INTEGER, "
            + COL_TRIAGE_DIA_BP      + " INTEGER, "
            + COL_TRIAGE_NOTES       + " TEXT, "
            + COL_TRIAGE_PRIORITY    + " TEXT DEFAULT 'GREEN', "
            + COL_TRIAGE_TYPE        + " TEXT DEFAULT 'REMOTE', "
            + COL_TRIAGE_DATE        + " TEXT DEFAULT (datetime('now')), "
            + "FOREIGN KEY(" + COL_TRIAGE_PATIENT_ID + ") REFERENCES " + TABLE_USERS        + "(" + COL_USER_ID  + "), "
            + "FOREIGN KEY(" + COL_TRIAGE_DOCTOR_ID  + ") REFERENCES " + TABLE_USERS        + "(" + COL_USER_ID  + "), "
            + "FOREIGN KEY(" + COL_TRIAGE_APPT_ID    + ") REFERENCES " + TABLE_APPOINTMENTS + "(" + COL_APPT_ID  + ")"
            + ");";

    private static final String CREATE_DOCTOR_DOCS = "CREATE TABLE " + TABLE_DOCTOR_DOCS + " ("
            + COL_DOC_ID          + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COL_DOC_DOCTOR_ID   + " INTEGER NOT NULL, "
            + COL_DOC_TYPE        + " TEXT NOT NULL, "
            + COL_DOC_PATH        + " TEXT NOT NULL, "
            + COL_DOC_STATUS      + " TEXT NOT NULL DEFAULT 'PENDING', "
            + COL_DOC_UPLOADED_AT + " TEXT DEFAULT (datetime('now')), "
            + "FOREIGN KEY(" + COL_DOC_DOCTOR_ID + ") REFERENCES " + TABLE_USERS + "(" + COL_USER_ID + ")"
            + ");";

    // ─── Singleton ────────────────────────────────────────────────────────────
    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("PRAGMA foreign_keys = ON;");
        db.execSQL(CREATE_USERS);
        db.execSQL(CREATE_HOSPITALS);
        db.execSQL(CREATE_SCHEDULES);
        db.execSQL(CREATE_APPOINTMENTS);
        db.execSQL(CREATE_TRIAGE);
        db.execSQL(CREATE_DOCTOR_DOCS);
        seedHospitals(db);
        seedDoctorsAndSchedules(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DOCTOR_DOCS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRIAGE);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_APPOINTMENTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SCHEDULES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HOSPITALS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        db.execSQL("PRAGMA foreign_keys = ON;");
    }

    /** Carga hospitales de ejemplo al crear la base de datos */
    private void seedHospitals(SQLiteDatabase db) {
        String[] hospitals = {
            "('Hospital Nacional Dos de Mayo','Av. Grau 13, Lima','0015130','Medicina General',-12.0566,-77.0186)",
            "('Hospital Rebagliati','Av. Rebagliati 490, Lima','0016152','Seguro Social',-12.0697,-77.0355)",
            "('Instituto Nacional de Salud del Niño','Av. Brasil 600, Lima','0016151','Pediatría',-12.0858,-77.0530)",
            "('Hospital Guillermo Almenara','Av. Grau 800, Lima','0016146','Medicina General',-12.0594,-77.0283)",
            "('Hospital María Auxiliadora','Av. Miguel Iglesias, Lima','0015208','Medicina General',-12.1574,-76.9724)"
        };
        for (String h : hospitals) {
            db.execSQL("INSERT INTO " + TABLE_HOSPITALS
                    + " (name, address, phone, specialty, latitude, longitude) VALUES " + h);
        }
    }

    /** Carga un doctor de prueba activo y genera horarios disponibles para los siguientes 6 días */
    private void seedDoctorsAndSchedules(SQLiteDatabase db) {
        db.execSQL("INSERT INTO " + TABLE_USERS + " (name, email, password, dni, phone, role, status) VALUES "
                + "('Dr. Carlos Mendoza', 'carlos.mendoza@medapp.com', 'password123', '12345678', '987654321', 'DOCTOR', 'ACTIVE')");

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        java.util.Calendar cal = java.util.Calendar.getInstance();

        String[] times = {"09:00", "10:00", "11:00", "14:00", "15:00", "16:00"};

        for (int i = 0; i < 6; i++) {
            String dateStr = sdf.format(cal.getTime());
            for (int hospId = 1; hospId <= 5; hospId++) {
                for (String time : times) {
                    db.execSQL("INSERT INTO " + TABLE_SCHEDULES
                            + " (doctor_id, hospital_id, date, time, available) VALUES "
                            + "(1, " + hospId + ", '" + dateStr + "', '" + time + "', 1)");
                }
            }
            cal.add(java.util.Calendar.DAY_OF_YEAR, 1);
        }
    }
}
