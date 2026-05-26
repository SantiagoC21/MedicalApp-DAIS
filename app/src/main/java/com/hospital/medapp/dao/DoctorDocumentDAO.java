package com.hospital.medapp.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.hospital.medapp.database.DatabaseHelper;
import com.hospital.medapp.models.DoctorDocument;

import java.util.ArrayList;
import java.util.List;

/**
 * DAO para los documentos de validación de médicos.
 * Los admins consultan esta tabla para aprobar o rechazar cuentas de médicos.
 */
public class DoctorDocumentDAO {

    private final DatabaseHelper dbHelper;

    public DoctorDocumentDAO(Context context) {
        dbHelper = DatabaseHelper.getInstance(context);
    }

    // ─── INSERT ───────────────────────────────────────────────────────────────

    /**
     * Guarda la ruta local de un documento subido por el médico.
     * @param doctorId   ID del médico recién registrado.
     * @param docType    Tipo de documento (TITLE, LICENSE, ID).
     * @param filePath   URI o path local del archivo.
     * @return ID insertado o -1 si falla.
     */
    public long saveDocument(int doctorId,
                             DoctorDocument.DocType docType,
                             String filePath) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv  = new ContentValues();
        cv.put(DatabaseHelper.COL_DOC_DOCTOR_ID, doctorId);
        cv.put(DatabaseHelper.COL_DOC_TYPE,      docType.name());
        cv.put(DatabaseHelper.COL_DOC_PATH,      filePath);
        cv.put(DatabaseHelper.COL_DOC_STATUS,
               DoctorDocument.ValidationStatus.PENDING.name());
        return db.insert(DatabaseHelper.TABLE_DOCTOR_DOCS, null, cv);
    }

    // ─── SELECT ───────────────────────────────────────────────────────────────

    /** Todos los documentos de un médico específico. */
    public List<DoctorDocument> getDocumentsByDoctor(int doctorId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<DoctorDocument> list = new ArrayList<>();
        Cursor c = db.query(DatabaseHelper.TABLE_DOCTOR_DOCS, null,
                DatabaseHelper.COL_DOC_DOCTOR_ID + "=?",
                new String[]{String.valueOf(doctorId)},
                null, null,
                DatabaseHelper.COL_DOC_UPLOADED_AT + " DESC");
        while (c.moveToNext()) list.add(cursorToDoc(c));
        c.close();
        return list;
    }

    /**
     * Documentos pendientes de validación (para la pantalla del admin).
     * Hace JOIN con users para mostrar el nombre del médico.
     */
    public List<DoctorDocument> getPendingDocuments() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<DoctorDocument> list = new ArrayList<>();

        String sql =
            "SELECT d.*, u." + DatabaseHelper.COL_USER_NAME + " AS doctor_name "
            + "FROM "  + DatabaseHelper.TABLE_DOCTOR_DOCS + " d "
            + "JOIN "  + DatabaseHelper.TABLE_USERS       + " u "
            + "  ON d." + DatabaseHelper.COL_DOC_DOCTOR_ID + " = u." + DatabaseHelper.COL_USER_ID
            + " WHERE d." + DatabaseHelper.COL_DOC_STATUS + " = 'PENDING'"
            + " ORDER BY d." + DatabaseHelper.COL_DOC_UPLOADED_AT;

        Cursor c = db.rawQuery(sql, null);
        while (c.moveToNext()) {
            DoctorDocument doc = cursorToDoc(c);
            int dnIdx = c.getColumnIndex("doctor_name");
            if (dnIdx >= 0) doc.setDoctorName(c.getString(dnIdx));
            list.add(doc);
        }
        c.close();
        return list;
    }

    /** Verifica si todos los documentos de un médico están aprobados. */
    public boolean allDocumentsApproved(int doctorId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        // Contar pendientes o rechazados que aún existan
        Cursor c = db.query(DatabaseHelper.TABLE_DOCTOR_DOCS,
                new String[]{"COUNT(*)"},
                DatabaseHelper.COL_DOC_DOCTOR_ID + "=? AND "
                        + DatabaseHelper.COL_DOC_STATUS + " != 'APPROVED'",
                new String[]{String.valueOf(doctorId)},
                null, null, null);
        int notApproved = 0;
        if (c.moveToFirst()) notApproved = c.getInt(0);
        c.close();
        return notApproved == 0;
    }

    // ─── UPDATE ───────────────────────────────────────────────────────────────

    /**
     * Aprueba o rechaza un documento.
     * Si se aprueban todos los docs del médico, activa su cuenta automáticamente.
     */
    public boolean updateDocumentStatus(int docId,
                                        DoctorDocument.ValidationStatus newStatus,
                                        UserDAO userDAO) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Obtener el doctorId antes de actualizar
        Cursor c = db.query(DatabaseHelper.TABLE_DOCTOR_DOCS,
                new String[]{DatabaseHelper.COL_DOC_DOCTOR_ID},
                DatabaseHelper.COL_DOC_ID + "=?",
                new String[]{String.valueOf(docId)},
                null, null, null);
        if (!c.moveToFirst()) { c.close(); return false; }
        int doctorId = c.getInt(0);
        c.close();

        // Actualizar estado del documento
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_DOC_STATUS, newStatus.name());
        int rows = db.update(DatabaseHelper.TABLE_DOCTOR_DOCS, cv,
                DatabaseHelper.COL_DOC_ID + "=?",
                new String[]{String.valueOf(docId)});

        // Si fue aprobado y todos los docs del médico están OK → activar cuenta
        if (rows > 0 && newStatus == DoctorDocument.ValidationStatus.APPROVED) {
            if (allDocumentsApproved(doctorId) && userDAO != null) {
                userDAO.updateStatus(doctorId, com.hospital.medapp.models.User.Status.ACTIVE);
            }
        }

        // Si fue rechazado → mantener/poner al médico en SUSPENDED
        if (rows > 0 && newStatus == DoctorDocument.ValidationStatus.REJECTED && userDAO != null) {
            userDAO.updateStatus(doctorId, com.hospital.medapp.models.User.Status.SUSPENDED);
        }

        return rows > 0;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private DoctorDocument cursorToDoc(Cursor c) {
        DoctorDocument doc = new DoctorDocument();
        doc.setDocId         (c.getInt   (c.getColumnIndexOrThrow(DatabaseHelper.COL_DOC_ID)));
        doc.setDoctorId      (c.getInt   (c.getColumnIndexOrThrow(DatabaseHelper.COL_DOC_DOCTOR_ID)));
        doc.setDocType       (DoctorDocument.DocType.valueOf(
                               c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_DOC_TYPE))));
        doc.setFilePath      (c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_DOC_PATH)));
        doc.setValidationStatus(DoctorDocument.ValidationStatus.valueOf(
                               c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_DOC_STATUS))));
        doc.setUploadedAt    (c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_DOC_UPLOADED_AT)));
        return doc;
    }
}
