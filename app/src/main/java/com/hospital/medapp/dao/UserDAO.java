package com.hospital.medapp.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.hospital.medapp.database.DatabaseHelper;
import com.hospital.medapp.models.User;

import java.util.ArrayList;
import java.util.List;

/**
 * DAO para la entidad User.
 * Encapsula todas las operaciones CRUD e incluye lógica de autenticación.
 */
public class UserDAO {

    private final DatabaseHelper dbHelper;

    public UserDAO(Context context) {
        dbHelper = DatabaseHelper.getInstance(context);
    }

    // ─── INSERT ───────────────────────────────────────────────────────────────

    /**
     * Registra un nuevo usuario. Los médicos quedan en estado PENDING hasta
     * que sus documentos sean validados por un administrador.
     *
     * @return el ID insertado, o -1 si hubo error (ej: email/DNI duplicado).
     */
    public long insertUser(User user) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = buildContentValues(user);
        long id = db.insert(DatabaseHelper.TABLE_USERS, null, cv);
        return id;
    }

    // ─── SELECT ───────────────────────────────────────────────────────────────

    /** Busca un usuario por email y contraseña (login). */
    public User authenticate(String email, String password) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sel = DatabaseHelper.COL_USER_EMAIL + "=? AND "
                   + DatabaseHelper.COL_USER_PASSWORD + "=?";
        Cursor c = db.query(DatabaseHelper.TABLE_USERS,
                null, sel, new String[]{email, password},
                null, null, null);
        User user = null;
        if (c.moveToFirst()) user = cursorToUser(c);
        c.close();
        return user;
    }

    /** Obtiene un usuario por su ID. */
    public User getUserById(int userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(DatabaseHelper.TABLE_USERS, null,
                DatabaseHelper.COL_USER_ID + "=?",
                new String[]{String.valueOf(userId)},
                null, null, null);
        User user = null;
        if (c.moveToFirst()) user = cursorToUser(c);
        c.close();
        return user;
    }

    /** Verifica si ya existe un email registrado. */
    public boolean emailExists(String email) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(DatabaseHelper.TABLE_USERS,
                new String[]{DatabaseHelper.COL_USER_ID},
                DatabaseHelper.COL_USER_EMAIL + "=?",
                new String[]{email}, null, null, null);
        boolean exists = c.getCount() > 0;
        c.close();
        return exists;
    }

    /** Verifica si ya existe un DNI registrado. */
    public boolean dniExists(String dni) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(DatabaseHelper.TABLE_USERS,
                new String[]{DatabaseHelper.COL_USER_ID},
                DatabaseHelper.COL_USER_DNI + "=?",
                new String[]{dni}, null, null, null);
        boolean exists = c.getCount() > 0;
        c.close();
        return exists;
    }

    /** Retorna todos los médicos activos. */
    public List<User> getActiveDoctors() {
        return getUsersByRoleAndStatus(User.Role.DOCTOR, User.Status.ACTIVE);
    }

    /** Retorna todos los pacientes activos. */
    public List<User> getActivePatients() {
        return getUsersByRoleAndStatus(User.Role.PATIENT, User.Status.ACTIVE);
    }

    /** Retorna médicos con documentos pendientes de aprobación. */
    public List<User> getPendingDoctors() {
        return getUsersByRoleAndStatus(User.Role.DOCTOR, User.Status.PENDING);
    }

    private List<User> getUsersByRoleAndStatus(User.Role role, User.Status status) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<User> list = new ArrayList<>();
        String sel = DatabaseHelper.COL_USER_ROLE   + "=? AND "
                   + DatabaseHelper.COL_USER_STATUS + "=?";
        Cursor c = db.query(DatabaseHelper.TABLE_USERS, null,
                sel, new String[]{role.name(), status.name()},
                null, null, DatabaseHelper.COL_USER_NAME + " ASC");
        while (c.moveToNext()) list.add(cursorToUser(c));
        c.close();
        return list;
    }

    // ─── UPDATE ───────────────────────────────────────────────────────────────

    /**
     * Actualiza datos básicos del perfil del usuario.
     */
    public boolean updateProfile(User user) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_USER_NAME,  user.getName());
        cv.put(DatabaseHelper.COL_USER_PHONE, user.getPhone());
        int rows = db.update(DatabaseHelper.TABLE_USERS, cv,
                DatabaseHelper.COL_USER_ID + "=?",
                new String[]{String.valueOf(user.getUserId())});
        return rows > 0;
    }

    /**
     * Cambia el estado de un usuario (ej: PENDING → ACTIVE para médicos aprobados).
     */
    public boolean updateStatus(int userId, User.Status newStatus) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_USER_STATUS, newStatus.name());
        int rows = db.update(DatabaseHelper.TABLE_USERS, cv,
                DatabaseHelper.COL_USER_ID + "=?",
                new String[]{String.valueOf(userId)});
        return rows > 0;
    }

    /** Cambia la contraseña del usuario. */
    public boolean updatePassword(int userId, String newPassword) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_USER_PASSWORD, newPassword);
        int rows = db.update(DatabaseHelper.TABLE_USERS, cv,
                DatabaseHelper.COL_USER_ID + "=?",
                new String[]{String.valueOf(userId)});
        return rows > 0;
    }

    // ─── DELETE ───────────────────────────────────────────────────────────────

    /** Elimina lógicamente: suspende al usuario en lugar de borrarlo. */
    public boolean suspendUser(int userId) {
        return updateStatus(userId, User.Status.SUSPENDED);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private ContentValues buildContentValues(User u) {
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_USER_NAME,     u.getName());
        cv.put(DatabaseHelper.COL_USER_EMAIL,    u.getEmail());
        cv.put(DatabaseHelper.COL_USER_PASSWORD, u.getPassword());
        cv.put(DatabaseHelper.COL_USER_DNI,      u.getDni());
        cv.put(DatabaseHelper.COL_USER_PHONE,    u.getPhone());
        cv.put(DatabaseHelper.COL_USER_ROLE,     u.getRole().name());
        cv.put(DatabaseHelper.COL_USER_STATUS,   u.getStatus().name());
        return cv;
    }

    private User cursorToUser(Cursor c) {
        User u = new User();
        u.setUserId   (c.getInt   (c.getColumnIndexOrThrow(DatabaseHelper.COL_USER_ID)));
        u.setName     (c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_USER_NAME)));
        u.setEmail    (c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_USER_EMAIL)));
        u.setPassword (c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_USER_PASSWORD)));
        u.setDni      (c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_USER_DNI)));
        u.setPhone    (c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_USER_PHONE)));
        u.setRole     (User.Role  .valueOf(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_USER_ROLE))));
        u.setStatus   (User.Status.valueOf(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_USER_STATUS))));
        u.setCreatedAt(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_USER_CREATED_AT)));
        return u;
    }
}
