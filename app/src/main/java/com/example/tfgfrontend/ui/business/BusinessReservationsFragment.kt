package com.example.tfgfrontend.ui.business

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.example.tfgfrontend.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

private data class BusinessReservationItem(
    val userDisplayName: String,
    val userEmail: String,
    val userPhotoUrl: String,
    val dayName: String,
    val date: String,
    val reservationTime: String
)

class BusinessReservationsFragment : Fragment(R.layout.fragment_business_reservations) {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val adapter = BusinessReservationsAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            findNavController().navigateUp()
        }

        val recycler = view.findViewById<RecyclerView>(R.id.rvBusinessReservations)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        loadBusinessReservations()
    }

    private fun loadBusinessReservations() {
        val businessId = auth.currentUser?.uid
        if (businessId.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Debes iniciar sesión como negocio", Toast.LENGTH_SHORT).show()
            return
        }

        firestore.collection("reservations")
            .whereEqualTo("businessId", businessId)
            .get()
            .addOnSuccessListener { result ->
                val items = result.documents.map { doc ->
                    val email = doc.getString("userEmail").orEmpty()
                    val fallback = doc.getString("userId").orEmpty().take(8)
                    val displayName = doc.getString("userDisplayName")
                        .orEmpty()
                        .ifBlank { email.substringBefore("@").ifBlank { fallback.ifBlank { "usuario" } } }

                    BusinessReservationItem(
                        userDisplayName = displayName,
                        userEmail = email,
                        userPhotoUrl = doc.getString("userPhotoUrl").orEmpty(),
                        dayName = doc.getString("dayName").orEmpty(),
                        date = doc.getString("date").orEmpty(),
                        reservationTime = doc.getString("reservationTime").orEmpty()
                    )
                }.sortedByDescending { it.date }

                adapter.submitList(items)
                view?.findViewById<TextView>(R.id.tvEmptyBusinessReservations)?.visibility =
                    if (items.isEmpty()) View.VISIBLE else View.GONE
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "No se pudieron cargar las reservas", Toast.LENGTH_SHORT).show()
            }
    }

    private class BusinessReservationsAdapter : RecyclerView.Adapter<BusinessReservationsAdapter.BusinessReservationViewHolder>() {

        private val items = mutableListOf<BusinessReservationItem>()

        fun submitList(newItems: List<BusinessReservationItem>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BusinessReservationViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_business_reservation, parent, false)
            return BusinessReservationViewHolder(view)
        }

        override fun onBindViewHolder(holder: BusinessReservationViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        class BusinessReservationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val userPhoto = itemView.findViewById<ImageView>(R.id.ivBusinessReservationUserPhoto)
            private val userName = itemView.findViewById<TextView>(R.id.tvBusinessReservationUserName)
            private val date = itemView.findViewById<TextView>(R.id.tvBusinessReservationDate)
            private val time = itemView.findViewById<TextView>(R.id.tvBusinessReservationTime)
            private val email = itemView.findViewById<TextView>(R.id.tvBusinessReservationUserEmail)

            fun bind(item: BusinessReservationItem) {
                userName.text = "Reserva de ${item.userDisplayName}"
                date.text = "${item.dayName} - ${item.date}"
                if (item.reservationTime.isNotBlank()) {
                    time.text = item.reservationTime
                    time.visibility = View.VISIBLE
                } else {
                    time.visibility = View.GONE
                }
                email.text = item.userEmail

                userPhoto.load(item.userPhotoUrl.ifBlank { null }) {
                    placeholder(R.drawable.ic_profile_placeholder)
                    error(R.drawable.ic_profile_placeholder)
                    fallback(R.drawable.ic_profile_placeholder)
                    transformations(CircleCropTransformation())
                }
            }
        }
    }
}
