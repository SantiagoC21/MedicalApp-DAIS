package com.hospital.medapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.hospital.medapp.R;
import com.hospital.medapp.dao.UserDAO;
import com.hospital.medapp.models.User;
import com.hospital.medapp.utils.SessionManager;

// ══════════════════════════════════════════════════════════════════════════════
// RegisterActivity  – paciente normal o médico (requiere subir documentos)
// ══════════════════════════════════════════════════════════════════════════════
public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etEmail, etDni, etPhone, etPassword, etConfirmPass;
    private android.widget.RadioGroup rgRole;
    private android.widget.RadioButton rbPatient, rbDoctor;
    private android.widget.LinearLayout layoutDoctorDocs;
    private android.widget.Button btnUploadTitle, btnUploadLicense, btnRegister;

    private UserDAO userDAO;
    private android.net.Uri titleDocUri, licenseDocUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etName         = findViewById(R.id.etName);
        etEmail        = findViewById(R.id.etEmail);
        etDni          = findViewById(R.id.etDni);
        etPhone        = findViewById(R.id.etPhone);
        etPassword     = findViewById(R.id.etPassword);
        etConfirmPass  = findViewById(R.id.etConfirmPass);
        rgRole         = findViewById(R.id.rgRole);
        rbPatient      = findViewById(R.id.rbPatient);
        rbDoctor       = findViewById(R.id.rbDoctor);
        layoutDoctorDocs = findViewById(R.id.layoutDoctorDocs);
        btnUploadTitle   = findViewById(R.id.btnUploadTitle);
        btnUploadLicense = findViewById(R.id.btnUploadLicense);
        btnRegister      = findViewById(R.id.btnRegister);
        Button btnBack   = findViewById(R.id.btnBack);

        userDAO = new UserDAO(this);

        // Mostrar/ocultar sección de documentos según rol seleccionado
        rgRole.setOnCheckedChangeListener((group, checkedId) -> {
            layoutDoctorDocs.setVisibility(
                    checkedId == R.id.rbDoctor
                            ? android.view.View.VISIBLE
                            : android.view.View.GONE);
        });

        btnUploadTitle  .setOnClickListener(v -> pickDocument(101));
        btnUploadLicense.setOnClickListener(v -> pickDocument(102));
        btnRegister     .setOnClickListener(v -> attemptRegister());
        btnBack         .setOnClickListener(v -> finish());
    }

    private void pickDocument(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/pdf","image/*"});
        startActivityForResult(Intent.createChooser(intent, "Seleccionar documento"), requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == 101) {
                titleDocUri = data.getData();
                btnUploadTitle.setText("✔ Título subido");
            } else if (requestCode == 102) {
                licenseDocUri = data.getData();
                btnUploadLicense.setText("✔ Licencia subida");
            }
        }
    }

    private void attemptRegister() {
        String name     = etName       .getText().toString().trim();
        String email    = etEmail      .getText().toString().trim();
        String dni      = etDni        .getText().toString().trim();
        String phone    = etPhone      .getText().toString().trim();
        String password = etPassword   .getText().toString().trim();
        String confirm  = etConfirmPass.getText().toString().trim();

        // Validaciones básicas
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email)
                || TextUtils.isEmpty(dni) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Complete todos los campos obligatorios.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!password.equals(confirm)) {
            Toast.makeText(this, "Las contraseñas no coinciden.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Correo inválido.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Verificar unicidad
        if (userDAO.emailExists(email)) {
            Toast.makeText(this, "Este correo ya está registrado.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (userDAO.dniExists(dni)) {
            Toast.makeText(this, "Este DNI ya está registrado.", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isDoctor = rbDoctor.isChecked();

        // Si es médico, requiere documentos
        if (isDoctor && (titleDocUri == null || licenseDocUri == null)) {
            Toast.makeText(this,
                    "Debe subir su título profesional y licencia médica.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        User.Role role = isDoctor ? User.Role.DOCTOR : User.Role.PATIENT;
        User newUser = new User(name, email, password, dni, phone, role);
        long userId = userDAO.insertUser(newUser);

        if (userId < 0) {
            Toast.makeText(this, "Error al registrar. Intente nuevamente.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Guardar rutas de documentos para médicos
        if (isDoctor) {
            com.hospital.medapp.dao.DoctorDocumentDAO docDAO =
                    new com.hospital.medapp.dao.DoctorDocumentDAO(this);
            docDAO.saveDocument((int) userId,
                    com.hospital.medapp.models.DoctorDocument.DocType.TITLE,
                    titleDocUri.toString());
            docDAO.saveDocument((int) userId,
                    com.hospital.medapp.models.DoctorDocument.DocType.LICENSE,
                    licenseDocUri.toString());

            Toast.makeText(this,
                    "Registro exitoso. Su cuenta está pendiente de validación.",
                    Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Registro exitoso. ¡Ya puede iniciar sesión!",
                    Toast.LENGTH_SHORT).show();
        }

        finish();
    }
}
