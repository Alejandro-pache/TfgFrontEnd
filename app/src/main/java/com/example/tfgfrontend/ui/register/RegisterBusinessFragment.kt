package com.example.tfgfrontend.ui.register

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.tfgfrontend.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestore

class RegisterBusinessFragment : Fragment(R.layout.fragment_register_business) {
    private var selectedLogoUri: Uri? = null

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
    }

    private fun registerBusiness(view: View) {
        val email = view.findViewById<EditText>(R.id.etEmail).text.toString().trim()
        val password = view.findViewById<EditText>(R.id.etPassword).text.toString().trim()
        val ownerName = view.findViewById<EditText>(R.id.etNombreApellidos).text.toString().trim()
        val businessName = view.findViewById<EditText>(R.id.etEmpresa).text.toString().trim()
        val dni = view.findViewById<EditText>(R.id.etDni).text.toString().trim()

        if (email.isEmpty() || password.isEmpty() || ownerName.isEmpty() || businessName.isEmpty() || dni.isEmpty()) {
            Toast.makeText(requireContext(), "No se ha podido crear el negocio", Toast.LENGTH_SHORT).show()
            return
        }

        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()
        val defaultLogoUri = "android.resource://${requireContext().packageName}/${R.drawable.listme}"
        val logoUriToSave = selectedLogoUri?.toString() ?: defaultLogoUri

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: return@addOnSuccessListener
                val businessData = hashMapOf(
                    "uid" to uid,
                    "email" to email,
                    "ownerName" to ownerName,
                    "businessName" to businessName,
                    "dni" to dni,
                    "logoUri" to logoUriToSave,
                    "createdAt" to System.currentTimeMillis()
                )

                firestore.collection("businesses")
                    .document(uid)
                    .set(businessData)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Negocio registrado", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.businessListFragment)
                    }
                    .addOnFailureListener {
                        Log.e("RegisterBusiness", "Error guardando negocio en Firestore", it)
                        Toast.makeText(requireContext(), "Revisa los datos e inténtalo de nuevo", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Log.e("RegisterBusiness", "Error creando usuario de negocio", it)
                val message = when (it) {
                    is FirebaseNetworkException -> "Revisa la red, por favor"
                    is FirebaseAuthWeakPasswordException -> "La contraseña es demasiado corta"
                    is FirebaseAuthInvalidCredentialsException -> "El correo no es válido"
                    is FirebaseAuthUserCollisionException -> "Ese correo ya está registrado"
                    else -> "Revisa los datos e inténtalo de nuevo"
                }
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
    }
}
