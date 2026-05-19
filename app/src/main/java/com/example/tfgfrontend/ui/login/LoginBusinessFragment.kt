package com.example.tfgfrontend.ui.login

import android.os.Bundle
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.tfgfrontend.R
import com.example.tfgfrontend.data.SessionPrefs
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.firestore.FirebaseFirestore

class LoginBusinessFragment : Fragment(R.layout.fragment_login_business) {
    private var isLoggingIn = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (SessionPrefs.shouldRememberBusiness(requireContext())) {
            view.findViewById<CheckBox>(R.id.cbRememberDevice).isChecked = true
            view.findViewById<EditText>(R.id.etDni).setText(SessionPrefs.getBusinessDni(requireContext()))
            view.findViewById<EditText>(R.id.etEmpresa).setText(SessionPrefs.getBusinessName(requireContext()))
            view.findViewById<EditText>(R.id.etEmail).setText(SessionPrefs.getBusinessEmail(requireContext()))
            view.findViewById<EditText>(R.id.etPassword).setText(SessionPrefs.getBusinessPassword(requireContext()))
        }

        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            findNavController().navigateUp()
        }

        view.findViewById<Button>(R.id.btnLoginBusiness).setOnClickListener {
            loginBusiness(view)
        }
    }

    private fun loginBusiness(view: View) {
        if (isLoggingIn) return

        val dni = view.findViewById<EditText>(R.id.etDni).text.toString().trim()
        val businessName = view.findViewById<EditText>(R.id.etEmpresa).text.toString().trim()
        val email = view.findViewById<EditText>(R.id.etEmail).text.toString().trim()
        val password = view.findViewById<EditText>(R.id.etPassword).text.toString().trim()

        if (dni.isEmpty() || businessName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "Rellena todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isNetworkAvailable() && SessionPrefs.shouldRememberBusiness(requireContext())) {
            val savedEmail = SessionPrefs.getBusinessEmail(requireContext())
            val savedPassword = SessionPrefs.getBusinessPassword(requireContext())
            val savedUid = SessionPrefs.getBusinessUid(requireContext())
            val savedBusinessName = SessionPrefs.getBusinessName(requireContext())
            if (email == savedEmail && password == savedPassword && savedUid.isNotBlank()) {
                Toast.makeText(requireContext(), "Acceso sin conexión", Toast.LENGTH_SHORT).show()
                findNavController().navigate(
                    R.id.businessSetupFragment,
                    Bundle().apply {
                        putString("businessId", savedUid)
                        putString("businessName", savedBusinessName)
                    }
                )
                return
            }
        }

        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()

        setLoginBusinessLoading(view, true)

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (uid.isNullOrBlank()) {
                    setLoginBusinessLoading(view, false)
                    auth.signOut()
                    Toast.makeText(requireContext(), "No se pudo validar la cuenta", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                firestore.collection("businesses")
                    .document(uid)
                    .get()
                    .addOnSuccessListener { document ->
                        if (!document.exists()) {
                            setLoginBusinessLoading(view, false)
                            auth.signOut()
                            Toast.makeText(requireContext(), "Esta cuenta no pertenece a un negocio", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }

                        val savedDni = document.getString("dni").orEmpty().trim()
                        val savedBusinessName = document.getString("businessName").orEmpty().trim()
                        val savedEmail = document.getString("email").orEmpty().trim()

                        val dniMatches = savedDni.equals(dni, ignoreCase = true)
                        val businessMatches = savedBusinessName.equals(businessName, ignoreCase = true)
                        val emailMatches = savedEmail.equals(email, ignoreCase = true)

                        if (dniMatches && businessMatches && emailMatches) {
                            setLoginBusinessLoading(view, false)
                            val rememberDevice = view.findViewById<CheckBox>(R.id.cbRememberDevice).isChecked
                            if (rememberDevice) {
                                SessionPrefs.saveBusinessCredentials(
                                    context = requireContext(),
                                    dni = dni,
                                    businessName = businessName,
                                    email = email,
                                    password = password,
                                    businessUid = uid
                                )
                            } else {
                                SessionPrefs.clearBusinessCredentials(requireContext())
                            }
                            Toast.makeText(requireContext(), "Bienvenido a tu negocio", Toast.LENGTH_SHORT).show()
                            findNavController().navigate(
                                R.id.businessSetupFragment,
                                Bundle().apply {
                                    putString("businessId", uid)
                                    putString("businessName", savedBusinessName)
                                }
                            )
                        } else {
                            setLoginBusinessLoading(view, false)
                            auth.signOut()
                            Toast.makeText(
                                requireContext(),
                                "Los datos del negocio no coinciden",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    .addOnFailureListener {
                        setLoginBusinessLoading(view, false)
                        auth.signOut()
                        Log.e("LoginBusiness", "Error consultando negocio", it)
                        Toast.makeText(requireContext(), "No se pudo validar el negocio", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                setLoginBusinessLoading(view, false)
                Log.e("LoginBusiness", "Error iniciando sesión de negocio", it)
                val message = when (it) {
                    is FirebaseNetworkException -> "Revisa la red, por favor"
                    is FirebaseAuthInvalidCredentialsException -> "Correo o contraseña incorrectos"
                    else -> "No se pudo iniciar sesión"
                }
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
    }

    private fun setLoginBusinessLoading(view: View, loading: Boolean) {
        isLoggingIn = loading
        val btnLoginBusiness = view.findViewById<Button>(R.id.btnLoginBusiness)
        val btnBack = view.findViewById<View>(R.id.btnBack)

        btnLoginBusiness.isEnabled = !loading
        btnBack.isEnabled = !loading
        btnLoginBusiness.text = if (loading) "Validando..." else "Acceder a negocio"
        btnLoginBusiness.alpha = if (loading) 0.7f else 1f
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService(ConnectivityManager::class.java)
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
