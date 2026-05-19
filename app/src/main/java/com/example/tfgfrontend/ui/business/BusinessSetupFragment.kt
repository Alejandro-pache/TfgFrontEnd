package com.example.tfgfrontend.ui.business

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.tfgfrontend.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class BusinessSetupFragment : Fragment(R.layout.fragment_business_setup) {

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private var isSaving = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val businessId = arguments?.getString("businessId").orEmpty()
        val businessName = arguments?.getString("businessName").orEmpty()

        if (businessId.isBlank()) {
            Toast.makeText(requireContext(), "No se encontró el negocio", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }

        val aboutInput = view.findViewById<EditText>(R.id.etBusinessAbout)
        val saveButton = view.findViewById<Button>(R.id.btnSaveBusinessAbout)
        val profile = view.findViewById<ImageView>(R.id.ivBusinessProfile)

        val userPhoto = auth.currentUser?.photoUrl
        if (userPhoto != null) {
            profile.load(userPhoto) {
                placeholder(R.drawable.ic_profile_placeholder)
                error(R.drawable.ic_profile_placeholder)
            }
        } else {
            profile.setImageResource(R.drawable.ic_profile_placeholder)
        }

        profile.setOnClickListener {
            findNavController().navigate(R.id.businessReservationsFragment)
        }

        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            findNavController().navigateUp()
        }

        view.findViewById<Button>(R.id.btnGoToCalendar).setOnClickListener {
            openBusinessCalendar(businessId, businessName)
        }

        firestore.collection("businesses").document(businessId).get()
            .addOnSuccessListener { doc ->
                aboutInput.setText(doc.getString("aboutBusiness").orEmpty())
            }

        saveButton.setOnClickListener {
            if (isSaving) return@setOnClickListener

            val aboutText = aboutInput.text.toString().trim()
            setSavingState(view, true)

            firestore.collection("businesses")
                .document(businessId)
                .update("aboutBusiness", aboutText)
                .addOnSuccessListener {
                    setSavingState(view, false)
                    Toast.makeText(requireContext(), "Información guardada", Toast.LENGTH_SHORT).show()
                    openBusinessCalendar(businessId, businessName)
                }
                .addOnFailureListener {
                    setSavingState(view, false)
                    Toast.makeText(requireContext(), "No se pudo guardar la información", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun openBusinessCalendar(businessId: String, businessName: String) {
        findNavController().navigate(
            R.id.businessCalendarFragment,
            Bundle().apply {
                putString("businessId", businessId)
                putString("businessName", businessName)
            }
        )
    }

    private fun setSavingState(view: View, saving: Boolean) {
        isSaving = saving
        val saveButton = view.findViewById<Button>(R.id.btnSaveBusinessAbout)
        val backButton = view.findViewById<View>(R.id.btnBack)
        val calendarButton = view.findViewById<Button>(R.id.btnGoToCalendar)

        saveButton.isEnabled = !saving
        backButton.isEnabled = !saving
        calendarButton.isEnabled = !saving
        saveButton.alpha = if (saving) 0.7f else 1f
        saveButton.text = if (saving) "Guardando..." else "Guardar información"
    }
}
