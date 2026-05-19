# ListMe — TFG Frontend (Android)

Aplicación Android (Kotlin + XML, sin Compose) para la gestión de reservas entre clientes y negocios.

Este repositorio contiene el **frontend** del proyecto TFG **ListMe**.

---

## Descripción

ListMe digitaliza el flujo de reservas para reducir errores habituales de gestión manual (teléfono/mensajería), ofreciendo:

- acceso para **cliente** y **negocio**,
- consulta de negocios disponibles,
- reserva por **día y hora**,
- validación de horario según disponibilidad del negocio,
- consulta y cancelación de reservas,
- persistencia local segura y soporte básico sin conexión.

---

## Funcionalidades principales

### Cliente
- Registro e inicio de sesión (email/contraseña).
- Inicio de sesión con Google.
- Listado de negocios.
- Selección de fecha en calendario.
- Selección de hora mediante `TimePickerDialog`.
- Validación de hora dentro del rango de apertura/cierre del negocio.
- Módulo **Mis reservas** con opción de cancelación.
- Caché local de reservas para mostrar datos sin conexión.

### Negocio
- Registro con datos del negocio.
- Configuración de horario semanal.
- Inicio de sesión de negocio con validación.
- Consulta de reservas recibidas.
- Logo por defecto (`listme`) si no se ha elegido imagen manual.

---

## Stack tecnológico

- **Lenguaje:** Kotlin
- **UI:** XML + Fragments (Android Views)
- **Arquitectura de carpetas:** `ui / domain / data`
- **Backend (BaaS):** Firebase Authentication + Cloud Firestore
- **Persistencia local segura:** `EncryptedSharedPreferences`
- **Carga de imágenes:** Coil
- **Navegación:** Android Navigation Component

---

## Requisitos

- Android Studio (versión reciente)
- JDK 11
- Gradle Wrapper incluido en el proyecto
- Proyecto Firebase configurado

> Configuración actual del módulo `app`:
>
> - `compileSdk = 36`
> - `minSdk = 36`
> - `targetSdk = 36`

---

## Configuración y ejecución

1. Clonar el repositorio:

```bash
git clone https://github.com/Alejandro-pache/TfgFrontEnd.git
```

2. Abrir el proyecto en Android Studio.

3. Verificar `google-services.json` en:

```text
app/google-services.json
```

4. Sincronizar Gradle (**Sync Project with Gradle Files**).

5. Ejecutar la app en dispositivo/emulador compatible con API 36.

---

## Firestore

- Consola del proyecto:
  - https://console.firebase.google.com/project/fir-android-5fe32/firestore
- Reglas en el repositorio:
  - `firestore.rules`

Colecciones principales utilizadas:

- `businesses`
- `reservations`

---

## Estructura del proyecto

```text
app/src/main/java/com/example/tfgfrontend/
├── data/
│   └── SessionPrefs.kt
├── domain/
│   └── ScheduleValidator.kt
└── ui/
    ├── start/
    ├── login/
    ├── register/
    └── business/
```

---

## Estado del proyecto

Proyecto funcional para alcance de TFG, con mejoras recientes en:

- validación de hora de reserva,
- visualización de la hora en módulos de reservas,
- robustez del fallback de logo por defecto,
- ajustes de calidad y warnings clave de lint.

---

## Autor

**Alejandro Pache Porras**  
2º DAM — IES Las Salinas
