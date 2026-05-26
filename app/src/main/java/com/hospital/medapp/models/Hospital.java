// ──────────────────────────────────────────────────────────────────────────────
// Hospital.java
// ──────────────────────────────────────────────────────────────────────────────
package com.hospital.medapp.models;

public class Hospital {
    private int    hospitalId;
    private String name;
    private String address;
    private String phone;
    private String specialty;
    private double latitude;
    private double longitude;

    public Hospital() {}

    public int    getHospitalId()            { return hospitalId; }
    public void   setHospitalId(int v)       { hospitalId = v; }
    public String getName()                  { return name; }
    public void   setName(String v)          { name = v; }
    public String getAddress()               { return address; }
    public void   setAddress(String v)       { address = v; }
    public String getPhone()                 { return phone; }
    public void   setPhone(String v)         { phone = v; }
    public String getSpecialty()             { return specialty; }
    public void   setSpecialty(String v)     { specialty = v; }
    public double getLatitude()              { return latitude; }
    public void   setLatitude(double v)      { latitude = v; }
    public double getLongitude()             { return longitude; }
    public void   setLongitude(double v)     { longitude = v; }

    @Override
    public String toString() { return name + " – " + specialty; }
}
