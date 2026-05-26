<div align="center">

# 🏥 MedApp

**Aplicación Android para gestión de atención médica hospitalaria**

![Android](https://img.shields.io/badge/Android-API%2024%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Java](https://img.shields.io/badge/Java-11-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![SQLite](https://img.shields.io/badge/SQLite-Local%20DB-003B57?style=for-the-badge&logo=sqlite&logoColor=white)
![Material Design](https://img.shields.io/badge/Material%20Design-3-757575?style=for-the-badge&logo=material-design&logoColor=white)
![Version](https://img.shields.io/badge/Versión-1.0-blue?style=for-the-badge)

</div>

---

## 📋 Tabla de Contenidos

- [Descripción](#-descripción)
- [Características](#-características)
- [Capturas de Pantalla](#-capturas-de-pantalla)
- [Arquitectura](#-arquitectura)
- [Tecnologías](#-tecnologías)
- [Instalación](#-instalación)
- [Uso](#-uso)
- [Base de Datos](#-base-de-datos)
- [Sensor PPG](#-sensor-ppg-fotopletismografía)
- [Permisos](#-permisos)

---

## 📱 Descripción

**MedApp** es una aplicación móvil Android orientada a modernizar la gestión de atención médica hospitalaria. Conecta a **pacientes** y **médicos** en un flujo completo que va desde el agendamiento de citas hasta el **triaje remoto no invasivo** mediante fotopletismografía (PPG) usando la cámara del teléfono.

> Desarrollado como proyecto académico para la asignatura de **Desarrollo Adaptativo** — 10mo ciclo.

---

## ✨ Características

### 👤 Para Pacientes
- Registro e inicio de sesión seguro
- Búsqueda y selección de hospitales cercanos en Lima
- Agendamiento de citas con horarios disponibles por médico
- **Triaje remoto** con medición de signos vitales desde el teléfono
- Historial de citas y resultados de triaje

### 🩺 Para Médicos
- Dashboard personalizado con lista de citas
- Visualización de resultados de triaje por paciente
- Carga y gestión de documentos de habilitación profesional
- Gestión de horarios de atención

### 🏥 Sistema de Triaje
| Prioridad | Color | Descripción |
|-----------|-------|-------------|
| Crítico | 🔴 RED | Atención inmediata |
| Urgente | 🟠 ORANGE | Atención en < 30 min |
| Menos urgente | 🟡 YELLOW | Atención en < 2 h |
| No urgente | 🟢 GREEN | Atención programada |

---

## 📸 Capturas de Pantalla

> *Próximamente*

---

## 🏗️ Arquitectura

```
com.hospital.medapp/
│
├── activities/          # Vistas (UI Layer)
│   ├── LoginActivity
│   ├── RegisterActivity
│   ├── PatientDashboardActivity
│   ├── DoctorDashboardActivity
│   ├── HospitalListActivity
│   ├── AppointmentActivity
│   └── TriageActivity
│
├── adapters/            # RecyclerView Adapters
│   ├── AppointmentAdapter
│   ├── HospitalAdapter
│   └── ScheduleAdapter
│
├── dao/                 # Data Access Objects
│   ├── UserDAO
│   ├── HospitalDAO
│   ├── ScheduleDAO
│   ├── AppointmentDAO
│   ├── TriageDAO
│   └── DoctorDocumentDAO
│
├── database/            # SQLite Helper
│   └── DatabaseHelper
│
├── models/              # Entidades de dominio
│   ├── User
│   ├── Hospital
│   ├── Schedule
│   ├── Appointment
│   ├── Triage
│   └── DoctorDocument
│
└── utils/               # Utilidades
    ├── PPGProcessor     # Sensor de frecuencia cardíaca / SpO₂
    └── SessionManager   # Gestión de sesión de usuario
```

**Patrón arquitectónico:** DAO + Activities (MVP simplificado)

---

## 🛠️ Tecnologías

| Tecnología | Uso |
|---|---|
| **Java 11** | Lenguaje principal |
| **SQLite** | Base de datos local |
| **Camera2 API** | Captura de frames para PPG |
| **RecyclerView** | Listas y grillas |
| **CardView** | Tarjetas de UI |
| **ConstraintLayout** | Layouts responsivos |
| **Material Design** | Componentes de UI |
| **AndroidX AppCompat** | Compatibilidad hacia atrás |

---

## 🚀 Instalación

### Prerrequisitos

- Android Studio **Flamingo** o superior
- JDK 11
- Android SDK API 24+

### Pasos

1. **Clonar el repositorio**
   ```bash
   git clone https://github.com/SantiagoC21/MedicalApp-DAIS.git
   cd MedicalApp-DAIS
   ```

2. **Abrir en Android Studio**
   - `File → Open` → seleccionar la carpeta del proyecto

3. **Sincronizar Gradle**
   - Android Studio lo hará automáticamente, o presionar **Sync Now**

4. **Ejecutar la app**
   - Conectar un dispositivo físico o usar un emulador (API 24+)
   - Presionar ▶️ **Run**

> ⚠️ **Nota:** Para la función de triaje PPG se **requiere un dispositivo físico** con cámara trasera y flash LED. El emulador no puede simular la cámara de forma adecuada para esta función.

---

## 📖 Uso

### Roles de usuario

| Rol | Descripción | Estado inicial |
|-----|-------------|----------------|
| `PATIENT` | Paciente que solicita atención | `ACTIVE` |
| `DOCTOR` | Médico que brinda atención | `PENDING` (requiere validación de documentos) |
| `ADMIN` | Administrador del sistema | `ACTIVE` |

### Flujo principal

```
Registro / Login
      │
      ├── PATIENT → Dashboard Paciente
      │       ├── Ver hospitales disponibles
      │       ├── Agendar cita (hospital → doctor → horario)
      │       └── Realizar triaje remoto (PPG)
      │
      └── DOCTOR → Dashboard Médico
              ├── Ver citas programadas
              ├── Revisar triajes de pacientes
              └── Subir documentos de habilitación
```

---

## 🗄️ Base de Datos

La app usa **SQLite local** con las siguientes tablas:

```sql
users            -- Pacientes, médicos y administradores
hospitals        -- Centros de salud disponibles
schedules        -- Horarios de atención por médico/hospital
appointments     -- Citas agendadas
triage           -- Registros de triaje y signos vitales
doctor_documents -- Documentos de habilitación médica
```

#### Hospitales pre-cargados (Lima, Perú)

| Hospital | Especialidad |
|----------|-------------|
| Hospital Nacional Dos de Mayo | Medicina General |
| Hospital Rebagliati | Seguro Social |
| Instituto Nacional de Salud del Niño | Pediatría |
| Hospital Guillermo Almenara | Medicina General |
| Hospital María Auxiliadora | Medicina General |

---

## 💓 Sensor PPG (Fotopletismografía)

MedApp implementa medición de signos vitales sin hardware adicional usando la cámara trasera del teléfono:

### Frecuencia Cardíaca (BPM)
- Usa el **canal verde** del sensor de imagen (la hemoglobina absorbe ~530 nm)
- Cada latido produce una variación medible en la luz reflejada
- Algoritmo: normalización Z-score → media móvil → detección de picos → intervalos RR

### SpO₂ (Saturación de Oxígeno)
- Usa la relación entre el **canal rojo y azul** como aproximación a R/IR
- Fórmula: `SpO₂ ≈ 110 - 25 × (AC_red/DC_red) / (AC_blue/DC_blue)`
- Rango fisiológico limitado: **70% – 100%**

> ⚠️ **Aviso médico:** Las mediciones de SpO₂ son estimaciones orientativas. Para diagnóstico clínico se requiere un oxímetro de pulso certificado con sensor infrarrojo dedicado.

### Uso del sensor
1. Ir a **Triaje** desde el dashboard
2. Colocar el dedo suavemente sobre la **cámara trasera** cubriendo el flash
3. Mantener quieto durante **30 segundos**
4. Los resultados se guardan automáticamente en el historial del paciente

---

## 🔐 Permisos

| Permiso | Motivo |
|---------|--------|
| `CAMERA` | Captura de frames para medición PPG |
| `BODY_SENSORS` | Acceso a sensores de salud del dispositivo |
| `INTERNET` | Sincronización futura con backend |
| `READ_EXTERNAL_STORAGE` | Lectura de documentos médicos (API ≤ 32) |
| `READ_MEDIA_IMAGES` | Lectura de imágenes en API 33+ |

---

## 🤝 Contribuciones

Las contribuciones son bienvenidas. Para cambios importantes, por favor abre un **issue** primero para discutir qué te gustaría cambiar.

1. Fork del proyecto
2. Crear rama (`git checkout -b feature/nueva-funcionalidad`)
3. Commit de cambios (`git commit -m 'feat: agregar nueva funcionalidad'`)
4. Push a la rama (`git push origin feature/nueva-funcionalidad`)
5. Abrir un **Pull Request**

---

## 📄 Licencia

Este proyecto es de uso académico. Todos los derechos reservados © 2026.

---

<div align="center">

Hecho con ❤️ para la asignatura de **Desarrollo Adaptativo**

</div>
