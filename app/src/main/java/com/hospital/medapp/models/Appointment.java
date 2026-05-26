package com.hospital.medapp.models;

/** Cita médica entre un paciente y un médico en un hospital. */
public class Appointment {

    public enum Status { PENDING, CONFIRMED, CANCELLED, COMPLETED }

    private int    appointmentId;
    private int    patientId;
    private int    doctorId;
    private int    hospitalId;
    private int    scheduleId;
    private Status status;
    private String reason;
    private String createdAt;

    // Campos poblados por JOINs
    private String patientName;
    private String doctorName;
    private String hospitalName;
    private String scheduleDate;
    private String scheduleTime;

    public Appointment() {}

    // ─── Getters / Setters ────────────────────────────────────────────────────

    public int    getAppointmentId()             { return appointmentId; }
    public void   setAppointmentId(int v)        { appointmentId = v; }

    public int    getPatientId()                 { return patientId; }
    public void   setPatientId(int v)            { patientId = v; }

    public int    getDoctorId()                  { return doctorId; }
    public void   setDoctorId(int v)             { doctorId = v; }

    public int    getHospitalId()                { return hospitalId; }
    public void   setHospitalId(int v)           { hospitalId = v; }

    public int    getScheduleId()                { return scheduleId; }
    public void   setScheduleId(int v)           { scheduleId = v; }

    public Status getStatus()                    { return status; }
    public void   setStatus(Status v)            { status = v; }

    public String getReason()                    { return reason; }
    public void   setReason(String v)            { reason = v; }

    public String getCreatedAt()                 { return createdAt; }
    public void   setCreatedAt(String v)         { createdAt = v; }

    public String getPatientName()               { return patientName; }
    public void   setPatientName(String v)       { patientName = v; }

    public String getDoctorName()                { return doctorName; }
    public void   setDoctorName(String v)        { doctorName = v; }

    public String getHospitalName()              { return hospitalName; }
    public void   setHospitalName(String v)      { hospitalName = v; }

    public String getScheduleDate()              { return scheduleDate; }
    public void   setScheduleDate(String v)      { scheduleDate = v; }

    public String getScheduleTime()              { return scheduleTime; }
    public void   setScheduleTime(String v)      { scheduleTime = v; }
}
