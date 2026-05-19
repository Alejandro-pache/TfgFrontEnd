package com.example.tfgfrontend.ui.register

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.tfgfrontend.R
import com.example.tfgfrontend.databinding.FragmentRegisterUserBinding
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

class RegisterUserFragment : Fragment(R.layout.fragment_register_user) {

    private var _binding: FragmentRegisterUserBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private var isRegistering = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentRegisterUserBinding.bind(view)
        auth = FirebaseAuth.getInstance()

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnRegister.setOnClickListener {
            registerUser()
        }

        binding.btnGoToLogin.setOnClickListener {
            findNavController().navigate(R.id.loginClientFragment)
        }
    }

    private fun registerUser() {
        if (isRegistering) return

        val fullName = binding.etFullName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(requireContext(), "Rellena todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(requireContext(), "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
            return
        }

        setRegisterLoading(true)

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                setRegisterLoading(false)
                auth.signOut()
                Toast.makeText(requireContext(), "Cuenta creada. Ahora inicia sesión", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.loginClientFragment)
            }
            .addOnFailureListener {
                setRegisterLoading(false)
                Log.e("RegisterUser", "Error creando usuario", it)
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

    private fun setRegisterLoading(loading: Boolean) {
        isRegistering = loading
        binding.btnRegister.isEnabled = !loading
        binding.btnBack.isEnabled = !loading
        binding.btnGoToLogin.isEnabled = !loading
        binding.btnRegister.text = if (loading) "Registrando..." else "Registrar usuario"
        binding.btnRegister.alpha = if (loading) 0.7f else 1f
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
