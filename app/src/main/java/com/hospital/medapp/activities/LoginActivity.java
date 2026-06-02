package com.hospital.medapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.hospital.medapp.R;
import com.hospital.medapp.dao.UserDAO;
import com.hospital.medapp.models.User;
import com.hospital.medapp.utils.SessionManager;

// ══════════════════════════════════════════════════════════════════════════════
// LoginActivity
// ══════════════════════════════════════════════════════════════════════════════
public class LoginActivity extends AppCompatActivity {

    private EditText      etEmail, etPassword;
    private UserDAO       userDAO;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        session = new SessionManager(this);

        // Si ya hay sesión activa → redirigir directo
        if (session.isLoggedIn()) {
            redirectToDashboard();
            return;
        }

        setContentView(R.layout.activity_login);

        etEmail    = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        Button   btnLogin    = findViewById(R.id.btnLogin);
        TextView tvRegister  = findViewById(R.id.tvGoRegister);

        userDAO = new UserDAO(this);

        btnLogin.setOnClickListener(v -> attemptLogin());

        tvRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void attemptLogin() {
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Complete todos los campos.", Toast.LENGTH_SHORT).show();
            return;
        }

        User user = userDAO.authenticate(email, password);

        if (user == null) {
            Toast.makeText(this, "Correo o contraseña incorrectos.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (user.getStatus() == User.Status.PENDING) {
            new AlertDialog.Builder(this)
                    .setTitle("Simulación de Aprobación")
                    .setMessage("Esta cuenta de médico está PENDING (pendiente de validación de documentos).\n\nPara facilitar la evaluación académica, ¿desea auto-aprobar los documentos y activar la cuenta ahora?")
                    .setPositiveButton("Sí, auto-aprobar", (dialog, which) -> {
                        boolean ok = userDAO.updateStatus(user.getUserId(), User.Status.ACTIVE);
                        if (ok) {
                            user.setStatus(User.Status.ACTIVE);
                            session.createSession(user);
                            Toast.makeText(this, "Cuenta aprobada y activada con éxito.", Toast.LENGTH_SHORT).show();
                            redirectToDashboard();
                        } else {
                            Toast.makeText(this, "Error al activar la cuenta.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("No", (dialog, which) -> {
                        Toast.makeText(this, "Su cuenta está pendiente de validación de documentos.", Toast.LENGTH_LONG).show();
                    })
                    .setCancelable(false)
                    .show();
            return;
        }

        if (user.getStatus() == User.Status.SUSPENDED) {
            Toast.makeText(this, "Cuenta suspendida. Contacte al administrador.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        session.createSession(user);
        redirectToDashboard();
    }

    private void redirectToDashboard() {
        Intent intent;
        User.Role role = session.getUserRole();
        if (role == User.Role.DOCTOR) {
            intent = new Intent(this, DoctorDashboardActivity.class);
        } else {
            intent = new Intent(this, PatientDashboardActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}


