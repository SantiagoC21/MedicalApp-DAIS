package com.hospital.medapp.models;

/**
 * Documento de validación enviado por un médico al registrarse.
 * El administrador revisa estos documentos antes de activar la cuenta.
 */
public class DoctorDocument {

    public enum DocType {
        TITLE,      // Título profesional
        LICENSE,    // Licencia médica / CMP (Colegio Médico del Perú)
        ID          // Documento de identidad (DNI)
    }

    public enum ValidationStatus {
        PENDING,    // Esperando revisión del administrador
        APPROVED,   // Documento válido – cuenta activada si todos aprobados
        REJECTED    // Documento rechazado – cuenta suspendida
    }

    private int              docId;
    private int              doctorId;
    private DocType          docType;
    private String           filePath;
    private ValidationStatus validationStatus;
    private String           uploadedAt;

    // Campo extra poblado por JOINs, no almacenado en esta tabla
    private String doctorName;

    public DoctorDocument() {}

    // ─── Getters / Setters ────────────────────────────────────────────────────

    public int              getDocId()                          { return docId; }
    public void             setDocId(int v)                     { docId = v; }

    public int              getDoctorId()                       { return doctorId; }
    public void             setDoctorId(int v)                  { doctorId = v; }

    public DocType          getDocType()                        { return docType; }
    public void             setDocType(DocType v)               { docType = v; }

    public String           getFilePath()                       { return filePath; }
    public void             setFilePath(String v)               { filePath = v; }

    public ValidationStatus getValidationStatus()               { return validationStatus; }
    public void             setValidationStatus(ValidationStatus v) { validationStatus = v; }

    public String           getUploadedAt()                     { return uploadedAt; }
    public void             setUploadedAt(String v)             { uploadedAt = v; }

    public String           getDoctorName()                     { return doctorName; }
    public void             setDoctorName(String v)             { doctorName = v; }

    /** Etiqueta legible del tipo de documento para mostrar en la UI. */
    public String getDocTypeLabel() {
        switch (docType) {
            case TITLE:   return "Título Profesional";
            case LICENSE: return "Licencia / CMP";
            case ID:      return "DNI";
            default:      return docType.name();
        }
    }
}
