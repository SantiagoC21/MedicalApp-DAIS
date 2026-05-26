package com.hospital.medapp.models;

/**
 * Registro de triaje clínico.
 * Contiene los signos vitales capturados via sensor PPG (cámara) + datos manuales.
 */
public class Triage {

    /** Prioridad Manchester simplificada. */
    public enum Priority { GREEN, YELLOW, ORANGE, RED }

    /** Modalidad del triaje. */
    public enum Type { REMOTE, PRESENTIAL }

    private int      triageId;
    private int      patientId;
    private int      doctorId;       // 0 si es autotriaje sin médico asignado
    private int      appointmentId;  // 0 si no proviene de una cita
    private double   heartRateBpm;
    private double   spo2Percent;
    private double   temperatureC;
    private int      systolicBp;
    private int      diastolicBp;
    private String   notes;
    private Priority priority;
    private Type     triageType;
    private String   triageDate;

    // Campos poblados por JOINs
    private String   patientName;
    private String   doctorName;

    public Triage() {}

    // ─── Getters / Setters ────────────────────────────────────────────────────

    public int      getTriageId()                { return triageId; }
    public void     setTriageId(int v)           { triageId = v; }

    public int      getPatientId()               { return patientId; }
    public void     setPatientId(int v)          { patientId = v; }

    public int      getDoctorId()                { return doctorId; }
    public void     setDoctorId(int v)           { doctorId = v; }

    public int      getAppointmentId()           { return appointmentId; }
    public void     setAppointmentId(int v)      { appointmentId = v; }

    public double   getHeartRateBpm()            { return heartRateBpm; }
    public void     setHeartRateBpm(double v)    { heartRateBpm = v; }

    public double   getSpo2Percent()             { return spo2Percent; }
    public void     setSpo2Percent(double v)     { spo2Percent = v; }

    public double   getTemperatureC()            { return temperatureC; }
    public void     setTemperatureC(double v)    { temperatureC = v; }

    public int      getSystolicBp()              { return systolicBp; }
    public void     setSystolicBp(int v)         { systolicBp = v; }

    public int      getDiastolicBp()             { return diastolicBp; }
    public void     setDiastolicBp(int v)        { diastolicBp = v; }

    public String   getNotes()                   { return notes; }
    public void     setNotes(String v)           { notes = v; }

    public Priority getPriority()                { return priority; }
    public void     setPriority(Priority v)      { priority = v; }

    public Type     getTriageType()              { return triageType; }
    public void     setTriageType(Type v)        { triageType = v; }

    public String   getTriageDate()              { return triageDate; }
    public void     setTriageDate(String v)      { triageDate = v; }

    public String   getPatientName()             { return patientName; }
    public void     setPatientName(String v)     { patientName = v; }

    public String   getDoctorName()              { return doctorName; }
    public void     setDoctorName(String v)      { doctorName = v; }

    // ─── Lógica de negocio ───────────────────────────────────────────────────

    /**
     * Calcula la prioridad de triaje basándose en los signos vitales,
     * siguiendo el modelo Manchester Triage simplificado.
     *
     * Criterios:
     *  🔴 ROJO   – SpO₂ < 90 % | FC < 40 o > 150 | Temp ≥ 40 °C
     *  🟠 NARANJA – SpO₂ < 94 % | FC < 50 o > 130 | Temp ≥ 39 °C
     *  🟡 AMARILLO– SpO₂ < 96 % | FC < 60 o > 100 | Temp ≥ 38 °C
     *  🟢 VERDE  – Signos dentro de rangos normales
     *
     * @param bpm   Frecuencia cardíaca en latidos por minuto.
     * @param spo2  Saturación de oxígeno en sangre (%).
     * @param temp  Temperatura corporal en °C.
     * @return Prioridad calculada.
     */
    public static Priority calculatePriority(double bpm, double spo2, double temp) {
        if (spo2 > 0 && spo2 < 90)  return Priority.RED;
        if (bpm  > 0 && (bpm < 40 || bpm > 150)) return Priority.RED;
        if (temp > 0 && temp >= 40.0) return Priority.RED;

        if (spo2 > 0 && spo2 < 94)  return Priority.ORANGE;
        if (bpm  > 0 && (bpm < 50 || bpm > 130)) return Priority.ORANGE;
        if (temp > 0 && temp >= 39.0) return Priority.ORANGE;

        if (spo2 > 0 && spo2 < 96)  return Priority.YELLOW;
        if (bpm  > 0 && (bpm < 60 || bpm > 100)) return Priority.YELLOW;
        if (temp > 0 && temp >= 38.0) return Priority.YELLOW;

        return Priority.GREEN;
    }
}
