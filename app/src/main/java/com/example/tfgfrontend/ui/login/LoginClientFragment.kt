package com.example.tfgfrontend.ui.login

import android.os.Bundle
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.tfgfrontend.R
import com.example.tfgfrontend.databinding.FragmentLoginClientBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.Credential
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.example.tfgfrontend.data.SessionPrefs

class LoginClientFragment : Fragment(R.layout.fragment_login_client) {

    private var _binding: FragmentLoginClientBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager
    private var activeAuthAction: AuthAction? = null

    private enum class AuthAction {
        EMAIL,
        GOOGLE
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentLoginClientBinding.bind(view)

        auth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(requireContext())

        if (SessionPrefs.shouldRememberUser(requireContext())) {
            binding.cbRememberDevice.isChecked = true
            binding.etEmail.setText(SessionPrefs.getUserEmail(requireContext()))
            binding.etPassword.setText(SessionPrefs.getUserPassword(requireContext()))
        }

        binding.btnLogin.setOnClickListener {
            if (activeAuthAction != null) return@setOnClickListener
            loginWithEmail()
        }

        binding.btnGoogle.setOnClickListener {
            if (activeAuthAction != null) return@setOnClickListener
            signInWithGoogle()
        }

        binding.btnGoToRegister.setOnClickListener {
            findNavController().navigate(R.id.registerUserFragment)
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

        if (!isNetworkAvailable() && SessionPrefs.shouldRememberUser(requireContext())) {
            val savedEmail = SessionPrefs.getUserEmail(requireContext())
            val savedPassword = SessionPrefs.getUserPassword(requireContext())
            if (email == savedEmail && password == savedPassword) {
                Toast.makeText(requireContext(), "Acceso sin conexión", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.businessListFragment)
                return
            }
        }

        setAuthLoading(AuthAction.EMAIL, true)

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                setAuthLoading(AuthAction.EMAIL, false)
                if (task.isSuccessful) {
                    if (binding.cbRememberDevice.isChecked) {
                        SessionPrefs.saveUserCredentials(requireContext(), email, password)
                    } else {
                        SessionPrefs.clearUserCredentials(requireContext())
                    }
                    findNavController().navigate(R.id.businessListFragment)
                } else {
                    Toast.makeText(requireContext(), "Error al iniciar sesión", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun signInWithGoogle() {

        setAuthLoading(AuthAction.GOOGLE, true)

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
                setAuthLoading(AuthAction.GOOGLE, false)
                Log.e("GoogleSignIn", e.message ?: "Error")
                Toast.makeText(requireContext(), "No se pudo iniciar con Google", Toast.LENGTH_SHORT).show()
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
        } else {
            setAuthLoading(AuthAction.GOOGLE, false)
            Log.e("GoogleSignIn", "Credencial de Google no válida")
            Toast.makeText(requireContext(), "Error con la cuenta de Google", Toast.LENGTH_SHORT).show()
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {

        val firebaseCredential =
            GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(firebaseCredential)
            .addOnCompleteListener { task ->
                setAuthLoading(AuthAction.GOOGLE, false)
                if (task.isSuccessful) {
                    if (binding.cbRememberDevice.isChecked) {
                        val email = auth.currentUser?.email.orEmpty()
                        SessionPrefs.saveUserCredentials(requireContext(), email, "")
                    } else {
                        SessionPrefs.clearUserCredentials(requireContext())
                    }
                    findNavController().navigate(R.id.businessListFragment)
                } else {
                    Log.e("GoogleSignIn", task.exception?.message ?: "Auth error")
                    Toast.makeText(requireContext(), "Error al autenticar con Google", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun setAuthLoading(action: AuthAction, isLoading: Boolean) {
        if (isLoading) {
            activeAuthAction = action
        } else if (activeAuthAction == action) {
            activeAuthAction = null
        }

        val loading = activeAuthAction != null
        binding.btnLogin.isEnabled = !loading
        binding.btnGoogle.isEnabled = !loading
        binding.btnBack.isEnabled = !loading
        binding.btnGoToRegister.isEnabled = !loading

        setButtonStateText(binding.btnLogin, if (activeAuthAction == AuthAction.EMAIL) "Iniciando sesión..." else "Iniciar Sesión")
        setButtonStateText(binding.btnGoogle, if (activeAuthAction == AuthAction.GOOGLE) "Conectando..." else "Continuar con Google")
    }

    private fun setButtonStateText(button: Button, text: String) {
        button.text = text
        button.alpha = if (button.isEnabled) 1f else 0.7f
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService(ConnectivityManager::class.java)
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
