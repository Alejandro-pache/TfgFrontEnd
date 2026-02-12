package com.example.tfgfrontend.ui.login

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.tfgfrontend.R
import com.example.tfgfrontend.databinding.FragmentLoginClientBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth
import kotlinx.coroutines.launch
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.Credential
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL

class LoginClientFragment : Fragment(R.layout.fragment_login_client) {

    private var _binding: FragmentLoginClientBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentLoginClientBinding.bind(view)

        auth = Firebase.auth
        credentialManager = CredentialManager.create(requireContext())

        binding.btnLogin.setOnClickListener {
            loginWithEmail()
        }

        binding.btnGoogle.setOnClickListener {
            signInWithGoogle()
        }

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun loginWithEmail() {

        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "Rellena todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    findNavController().navigate(R.id.businessListFragment)
                } else {
                    Toast.makeText(requireContext(), "Error al iniciar sesión", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun signInWithGoogle() {

        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(getString(R.string.default_web_client_id))
            .setFilterByAuthorizedAccounts(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = requireContext()
                )

                handleSignIn(result.credential)

            } catch (e: GetCredentialException) {
                Log.e("GoogleSignIn", e.message ?: "Error")
            }
        }
    }

    private fun handleSignIn(credential: Credential) {

        if (credential is CustomCredential &&
            credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {

            val googleIdTokenCredential =
                GoogleIdTokenCredential.createFrom(credential.data)

            firebaseAuthWithGoogle(googleIdTokenCredential.idToken)
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {

        val firebaseCredential =
            GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(firebaseCredential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    findNavController().navigate(R.id.businessListFragment)
                } else {
                    Log.e("GoogleSignIn", task.exception?.message ?: "Auth error")
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
