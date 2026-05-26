package com.hospital.medapp.models;

public class User {
    public enum Role   { PATIENT, DOCTOR, ADMIN }
    public enum Status { ACTIVE, PENDING, SUSPENDED }

    private int    userId;
    private String name;
    private String email;
    private String password;
    private String dni;
    private String phone;
    private Role   role;
    private Status status;
    private String createdAt;

    public User() {}

    public User(String name, String email, String password,
                String dni, String phone, Role role) {
        this.name     = name;
        this.email    = email;
        this.password = password;
        this.dni      = dni;
        this.phone    = phone;
        this.role     = role;
        this.status   = (role == Role.DOCTOR) ? Status.PENDING : Status.ACTIVE;
    }

    // ─── Getters / Setters ────────────────────────────────────────────────────
    public int    getUserId()   { return userId; }
    public void   setUserId(int v)    { userId = v; }

    public String getName()     { return name; }
    public void   setName(String v)   { name = v; }

    public String getEmail()    { return email; }
    public void   setEmail(String v)  { email = v; }

    public String getPassword() { return password; }
    public void   setPassword(String v) { password = v; }

    public String getDni()      { return dni; }
    public void   setDni(String v)    { dni = v; }

    public String getPhone()    { return phone; }
    public void   setPhone(String v)  { phone = v; }

    public Role   getRole()     { return role; }
    public void   setRole(Role v)     { role = v; }

    public Status getStatus()   { return status; }
    public void   setStatus(Status v) { status = v; }

    public String getCreatedAt(){ return createdAt; }
    public void   setCreatedAt(String v) { createdAt = v; }

    public boolean isDoctor()  { return role == Role.DOCTOR; }
    public boolean isPatient() { return role == Role.PATIENT; }
    public boolean isActive()  { return status == Status.ACTIVE; }
}
