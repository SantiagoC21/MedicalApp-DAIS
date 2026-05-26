package com.hospital.medapp.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.hospital.medapp.models.User;

/**
 * Gestiona la sesión activa del usuario usando SharedPreferences.
 */
public class SessionManager {

    private static final String PREF_NAME       = "MedAppSession";
    private static final String KEY_USER_ID     = "user_id";
    private static final String KEY_USER_NAME   = "user_name";
    private static final String KEY_USER_EMAIL  = "user_email";
    private static final String KEY_USER_ROLE   = "user_role";
    private static final String KEY_IS_LOGGED   = "is_logged_in";

    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        prefs  = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    /** Crea la sesión después del login exitoso. */
    public void createSession(User user) {
        editor.putBoolean(KEY_IS_LOGGED,  true);
        editor.putInt    (KEY_USER_ID,    user.getUserId());
        editor.putString (KEY_USER_NAME,  user.getName());
        editor.putString (KEY_USER_EMAIL, user.getEmail());
        editor.putString (KEY_USER_ROLE,  user.getRole().name());
        editor.apply();
    }

    /** Verifica si hay sesión activa. */
    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED, false);
    }

    public int    getUserId()   { return prefs.getInt   (KEY_USER_ID,    -1); }
    public String getUserName() { return prefs.getString(KEY_USER_NAME,  ""); }
    public String getUserEmail(){ return prefs.getString(KEY_USER_EMAIL, ""); }

    public User.Role getUserRole() {
        String r = prefs.getString(KEY_USER_ROLE, User.Role.PATIENT.name());
        try { return User.Role.valueOf(r); }
        catch (IllegalArgumentException e) { return User.Role.PATIENT; }
    }

    public boolean isDoctor()  { return getUserRole() == User.Role.DOCTOR; }
    public boolean isPatient() { return getUserRole() == User.Role.PATIENT; }
    public boolean isAdmin()   { return getUserRole() == User.Role.ADMIN; }

    /** Cierra sesión y limpia todas las preferencias. */
    public void logout() {
        editor.clear();
        editor.apply();
    }
}
