package com.example.tfgfrontend.ui.register

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.tfgfrontend.R
import com.example.tfgfrontend.domain.ScheduleValidator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestore

class RegisterBusinessFragment : Fragment(R.layout.fragment_register_business) {
    private var selectedLogoUri: Uri? = null
    private var isRegisteringBusiness = false

    private val logoPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                requireContext().contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
            }
            selectedLogoUri = uri
            view?.findViewById<ImageView>(R.id.ivBusinessLogo)?.setImageURI(uri)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            findNavController().navigateUp()
        }

        view.findViewById<Button>(R.id.btnPickLogo).setOnClickListener {
            logoPicker.launch(arrayOf("image/*"))
        }

        view.findViewById<Button>(R.id.btnRegister).setOnClickListener {
            registerBusiness(view)
        }

        view.findViewById<TextView>(R.id.btnGoToBusinessLogin).setOnClickListener {
            findNavController().navigate(R.id.loginBusinessFragment)
        }
    }

    private fun registerBusiness(view: View) {
        if (isRegisteringBusiness) return

        val email = view.findViewById<EditText>(R.id.etEmail).text.toString().trim()
        val password = view.findViewById<EditText>(R.id.etPassword).text.toString().trim()
        val ownerName = view.findViewById<EditText>(R.id.etNombreApellidos).text.toString().trim()
        val businessName = view.findViewById<EditText>(R.id.etEmpresa).text.toString().trim()
        val dni = view.findViewById<EditText>(R.id.etDni).text.toString().trim()

        val mondayOpen = view.findViewById<EditText>(R.id.etMondayOpen).text.toString().trim()
        val mondayClose = view.findViewById<EditText>(R.id.etMondayClose).text.toString().trim()
        val tuesdayOpen = view.findViewById<EditText>(R.id.etTuesdayOpen).text.toString().trim()
        val tuesdayClose = view.findViewById<EditText>(R.id.etTuesdayClose).text.toString().trim()
        val wednesdayOpen = view.findViewById<EditText>(R.id.etWednesdayOpen).text.toString().trim()
        val wednesdayClose = view.findViewById<EditText>(R.id.etWednesdayClose).text.toString().trim()
        val thursdayOpen = view.findViewById<EditText>(R.id.etThursdayOpen).text.toString().trim()
        val thursdayClose = view.findViewById<EditText>(R.id.etThursdayClose).text.toString().trim()
        val fridayOpen = view.findViewById<EditText>(R.id.etFridayOpen).text.toString().trim()
        val fridayClose = view.findViewById<EditText>(R.id.etFridayClose).text.toString().trim()
        val saturdayOpen = view.findViewById<EditText>(R.id.etSaturdayOpen).text.toString().trim()
        val saturdayClose = view.findViewById<EditText>(R.id.etSaturdayClose).text.toString().trim()
        val sundayOpen = view.findViewById<EditText>(R.id.etSundayOpen).text.toString().trim()
        val sundayClose = view.findViewById<EditText>(R.id.etSundayClose).text.toString().trim()

        if (email.isEmpty() || password.isEmpty() || ownerName.isEmpty() || businessName.isEmpty() || dni.isEmpty() ||
            mondayOpen.isEmpty() || mondayClose.isEmpty() ||
            tuesdayOpen.isEmpty() || tuesdayClose.isEmpty() ||
            wednesdayOpen.isEmpty() || wednesdayClose.isEmpty() ||
            thursdayOpen.isEmpty() || thursdayClose.isEmpty() ||
            fridayOpen.isEmpty() || fridayClose.isEmpty() ||
            saturdayOpen.isEmpty() || saturdayClose.isEmpty() ||
            sundayOpen.isEmpty() || sundayClose.isEmpty()
        ) {
            Toast.makeText(requireContext(), "No se ha podido crear el negocio", Toast.LENGTH_SHORT).show()
            return
        }

        val weeklyRanges = listOf(
            Triple("Lunes", mondayOpen, mondayClose),
            Triple("Martes", tuesdayOpen, tuesdayClose),
            Triple("Miércoles", wednesdayOpen, wednesdayClose),
            Triple("Jueves", thursdayOpen, thursdayClose),
            Triple("Viernes", fridayOpen, fridayClose),
            Triple("Sábado", saturdayOpen, saturdayClose),
            Triple("Domingo", sundayOpen, sundayClose)
        )

        for ((dayName, open, close) in weeklyRanges) {
            if (!ScheduleValidator.isValidHour(open) || !ScheduleValidator.isValidHour(close)) {
                Toast.makeText(
                    requireContext(),
                    "$dayName: usa formato HH:mm (ej: 09:00)",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            if (!ScheduleValidator.isOpenBeforeClose(open, close)) {
                Toast.makeText(
                    requireContext(),
                    "$dayName: la apertura debe ser anterior al cierre",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
        }

        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()
        val defaultLogoUri = "android.resource://${requireContext().packageName}/${R.drawable.listme}"
        val logoUriToSave = selectedLogoUri?.toString() ?: defaultLogoUri
        val logoManuallySelected = selectedLogoUri != null

        setRegisterBusinessLoading(view, true)

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: return@addOnSuccessListener
                saveBusinessData(
                    uid = uid,
                    email = email,
                    ownerName = ownerName,
                    businessName = businessName,
                    dni = dni,
                    logoUriToSave = logoUriToSave,
                    logoManuallySelected = logoManuallySelected,
                    mondayOpen = mondayOpen,
                    mondayClose = mondayClose,
                    tuesdayOpen = tuesdayOpen,
                    tuesdayClose = tuesdayClose,
                    wednesdayOpen = wednesdayOpen,
                    wednesdayClose = wednesdayClose,
                    thursdayOpen = thursdayOpen,
                    thursdayClose = thursdayClose,
                    fridayOpen = fridayOpen,
                    fridayClose = fridayClose,
                    saturdayOpen = saturdayOpen,
                    saturdayClose = saturdayClose,
                    sundayOpen = sundayOpen,
                    sundayClose = sundayClose,
                    view = view,
                    firestore = firestore
                )
            }
            .addOnFailureListener {
                if (it is FirebaseAuthUserCollisionException) {
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnSuccessListener { result ->
                            val uid = result.user?.uid
                            if (uid.isNullOrBlank()) {
                                setRegisterBusinessLoading(view, false)
                                Toast.makeText(requireContext(), "No se pudo vincular la cuenta", Toast.LENGTH_SHORT).show()
                                return@addOnSuccessListener
                            }

                            saveBusinessData(
                                uid = uid,
                                email = email,
                                ownerName = ownerName,
                                businessName = businessName,
                                dni = dni,
                                logoUriToSave = logoUriToSave,
                                logoManuallySelected = logoManuallySelected,
                                mondayOpen = mondayOpen,
                                mondayClose = mondayClose,
                                tuesdayOpen = tuesdayOpen,
                                tuesdayClose = tuesdayClose,
                                wednesdayOpen = wednesdayOpen,
                                wednesdayClose = wednesdayClose,
                                thursdayOpen = thursdayOpen,
                                thursdayClose = thursdayClose,
                                fridayOpen = fridayOpen,
                                fridayClose = fridayClose,
                                saturdayOpen = saturdayOpen,
                                saturdayClose = saturdayClose,
                                sundayOpen = sundayOpen,
                                sundayClose = sundayClose,
                                view = view,
                                firestore = firestore
                            )
                        }
                        .addOnFailureListener {
                            setRegisterBusinessLoading(view, false)
                            Toast.makeText(
                                requireContext(),
                                "Ese correo ya existe. Si quieres usarlo como negocio, introduce la misma contraseña de esa cuenta",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                } else {
                    setRegisterBusinessLoading(view, false)
                    Log.e("RegisterBusiness", "Error creando usuario de negocio", it)
                    val message = when (it) {
                        is FirebaseNetworkException -> "Revisa la red, por favor"
                        is FirebaseAuthWeakPasswordException -> "La contraseña es demasiado corta"
                        is FirebaseAuthInvalidCredentialsException -> "El correo no es válido"
                        else -> "Revisa los datos e inténtalo de nuevo"
                    }
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveBusinessData(
        uid: String,
        email: String,
        ownerName: String,
        businessName: String,
        dni: String,
        logoUriToSave: String,
        logoManuallySelected: Boolean,
        mondayOpen: String,
        mondayClose: String,
        tuesdayOpen: String,
        tuesdayClose: String,
        wednesdayOpen: String,
        wednesdayClose: String,
        thursdayOpen: String,
        thursdayClose: String,
        fridayOpen: String,
        fridayClose: String,
        saturdayOpen: String,
        saturdayClose: String,
        sundayOpen: String,
        sundayClose: String,
        view: View,
        firestore: FirebaseFirestore
    ) {
        val schedule = hashMapOf(
            "monday" to hashMapOf("open" to mondayOpen, "close" to mondayClose),
            "tuesday" to hashMapOf("open" to tuesdayOpen, "close" to tuesdayClose),
            "wednesday" to hashMapOf("open" to wednesdayOpen, "close" to wednesdayClose),
            "thursday" to hashMapOf("open" to thursdayOpen, "close" to thursdayClose),
            "friday" to hashMapOf("open" to fridayOpen, "close" to fridayClose),
            "saturday" to hashMapOf("open" to saturdayOpen, "close" to saturdayClose),
            "sunday" to hashMapOf("open" to sundayOpen, "close" to sundayClose)
        )

        val businessData = hashMapOf(
            "uid" to uid,
            "email" to email,
            "ownerName" to ownerName,
            "businessName" to businessName,
            "dni" to dni,
            "logoUri" to logoUriToSave,
            "logoManuallySelected" to logoManuallySelected,
            "schedule" to schedule,
            "createdAt" to System.currentTimeMillis()
        )

        firestore.collection("businesses")
            .document(uid)
            .set(businessData)
            .addOnSuccessListener {
                setRegisterBusinessLoading(view, false)
                Toast.makeText(requireContext(), "Negocio registrado", Toast.LENGTH_SHORT).show()
                findNavController().navigate(
                    R.id.businessSetupFragment,
                    Bundle().apply {
                        putString("businessId", uid)
                        putString("businessName", businessName)
                    }
                )
            }
            .addOnFailureListener {
                setRegisterBusinessLoading(view, false)
                Log.e("RegisterBusiness", "Error guardando negocio en Firestore", it)
                Toast.makeText(requireContext(), "Revisa los datos e inténtalo de nuevo", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setRegisterBusinessLoading(view: View, loading: Boolean) {
        isRegisteringBusiness = loading
        val btnRegister = view.findViewById<Button>(R.id.btnRegister)
        val btnGoToBusinessLogin = view.findViewById<TextView>(R.id.btnGoToBusinessLogin)
        val btnPickLogo = view.findViewById<Button>(R.id.btnPickLogo)
        val btnBack = view.findViewById<View>(R.id.btnBack)

        btnRegister.isEnabled = !loading
        btnGoToBusinessLogin.isEnabled = !loading
        btnPickLogo.isEnabled = !loading
        btnBack.isEnabled = !loading

        btnRegister.text = if (loading) "Registrando negocio..." else "Registrar negocio"
        btnRegister.alpha = if (loading) 0.7f else 1f
    }

}
