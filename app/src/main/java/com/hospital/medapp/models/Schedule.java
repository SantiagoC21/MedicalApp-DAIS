package com.hospital.medapp.models;

/** Franja horaria de un médico en un hospital. */
public class Schedule {

    private int     scheduleId;
    private int     doctorId;
    private int     hospitalId;
    private String  date;           // YYYY-MM-DD
    private String  time;           // HH:MM
    private boolean available;

    // Campos poblados por JOINs
    private String  doctorName;
    private String  hospitalName;

    public Schedule() {}

    // ─── Getters / Setters ────────────────────────────────────────────────────

    public int     getScheduleId()            { return scheduleId; }
    public void    setScheduleId(int v)       { scheduleId = v; }

    public int     getDoctorId()              { return doctorId; }
    public void    setDoctorId(int v)         { doctorId = v; }

    public int     getHospitalId()            { return hospitalId; }
    public void    setHospitalId(int v)       { hospitalId = v; }

    public String  getDate()                  { return date; }
    public void    setDate(String v)          { date = v; }

    public String  getTime()                  { return time; }
    public void    setTime(String v)          { time = v; }

    public boolean isAvailable()              { return available; }
    public void    setAvailable(boolean v)    { available = v; }

    public String  getDoctorName()            { return doctorName; }
    public void    setDoctorName(String v)    { doctorName = v; }

    public String  getHospitalName()          { return hospitalName; }
    public void    setHospitalName(String v)  { hospitalName = v; }

    /** Texto combinado para mostrar en la UI: "2025-06-10  09:30". */
    public String getDisplayTime()            { return date + "  " + time; }
}
