package com.example.tfgfrontend.ui.business

import android.os.Bundle
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tfgfrontend.R
import coil.load
import coil.transform.CircleCropTransformation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONArray
import org.json.JSONObject

private data class ReservationItem(
    val id: String,
    val businessName: String,
    val dayName: String,
    val date: String,
    val reservationTime: String
)

class MyReservationsFragment : Fragment(R.layout.fragment_my_reservations) {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val reservationsAdapter by lazy {
        val email = auth.currentUser?.email.orEmpty()
        val username = email.substringBefore("@").ifBlank { "usuario" }
        ReservationsAdapter(
            userPhotoUri = auth.currentUser?.photoUrl?.toString().orEmpty(),
            userDisplayName = username
        ) { item, cancelButton ->
            cancelReservation(item.id, cancelButton)
        }
    }
    private val cachePrefs by lazy { requireContext().getSharedPreferences("reservation_cache", 0) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            findNavController().navigateUp()
        }

        val recycler = view.findViewById<RecyclerView>(R.id.rvMyReservations)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = reservationsAdapter

        loadReservations()
    }

    private fun loadReservations() {
        val userId = auth.currentUser?.uid
        if (userId.isNullOrBlank()) {
            val cached = loadCachedReservations()
            reservationsAdapter.submitList(cached)
            view?.findViewById<TextView>(R.id.tvEmptyReservations)?.visibility =
                if (cached.isEmpty()) View.VISIBLE else View.GONE
            if (!isNetworkAvailable()) {
                Toast.makeText(requireContext(), "Mostrando reservas guardadas en este dispositivo", Toast.LENGTH_SHORT).show()
            }
            return
        }

        firestore.collection("reservations")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { result ->
                val items = result.documents.map { doc ->
                    ReservationItem(
                        id = doc.id,
                        businessName = doc.getString("businessName").orEmpty(),
                        dayName = doc.getString("dayName").orEmpty(),
                        date = doc.getString("date").orEmpty(),
                        reservationTime = doc.getString("reservationTime").orEmpty()
                    )
                }.sortedByDescending { it.date }

                reservationsAdapter.submitList(items)
                cacheReservations(items)

                val emptyView = view?.findViewById<TextView>(R.id.tvEmptyReservations)
                emptyView?.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            }
            .addOnFailureListener {
                val cached = loadCachedReservations()
                reservationsAdapter.submitList(cached)
                view?.findViewById<TextView>(R.id.tvEmptyReservations)?.visibility =
                    if (cached.isEmpty()) View.VISIBLE else View.GONE
                val message = if (cached.isNotEmpty()) {
                    "Sin conexión: mostrando reservas guardadas"
                } else {
                    "No se pudieron cargar tus reservas"
                }
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
    }

    private fun cancelReservation(reservationId: String, cancelButton: Button) {
        firestore.collection("reservations").document(reservationId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Reserva cancelada", Toast.LENGTH_SHORT).show()
                loadReservations()
            }
            .addOnFailureListener {
                cancelButton.isEnabled = true
                cancelButton.text = "Cancelar"
                cancelButton.alpha = 1f
                Toast.makeText(requireContext(), "No se pudo cancelar la reserva", Toast.LENGTH_SHORT).show()
            }
    }

    private fun cacheReservations(items: List<ReservationItem>) {
        val array = JSONArray()
        for (item in items) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("businessName", item.businessName)
            obj.put("dayName", item.dayName)
            obj.put("date", item.date)
            obj.put("reservationTime", item.reservationTime)
            array.put(obj)
        }
        cachePrefs.edit().putString("items", array.toString()).apply()
    }

    private fun loadCachedReservations(): List<ReservationItem> {
        val raw = cachePrefs.getString("items", null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(
                        ReservationItem(
                            id = obj.optString("id"),
                            businessName = obj.optString("businessName"),
                            dayName = obj.optString("dayName"),
                            date = obj.optString("date"),
                            reservationTime = obj.optString("reservationTime")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService(ConnectivityManager::class.java)
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private class ReservationsAdapter(
        private val userPhotoUri: String,
        private val userDisplayName: String,
        private val onCancelClick: (ReservationItem, Button) -> Unit
    ) : RecyclerView.Adapter<ReservationsAdapter.ReservationViewHolder>() {

        private val items = mutableListOf<ReservationItem>()

        fun submitList(newItems: List<ReservationItem>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReservationViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_my_reservation, parent, false)
            return ReservationViewHolder(view)
        }

        override fun onBindViewHolder(holder: ReservationViewHolder, position: Int) {
            holder.bind(items[position], userPhotoUri, userDisplayName, onCancelClick)
        }

        override fun getItemCount(): Int = items.size

        class ReservationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val businessName = itemView.findViewById<TextView>(R.id.tvReservationBusinessName)
            private val date = itemView.findViewById<TextView>(R.id.tvReservationDate)
            private val time = itemView.findViewById<TextView>(R.id.tvReservationTime)
            private val userPhoto = itemView.findViewById<ImageView>(R.id.ivReservationUserPhoto)
            private val userLabel = itemView.findViewById<TextView>(R.id.tvReservationUserLabel)
            private val cancel = itemView.findViewById<Button>(R.id.btnCancelReservation)

            fun bind(
                item: ReservationItem,
                userPhotoUri: String,
                userDisplayName: String,
                onCancelClick: (ReservationItem, Button) -> Unit
            ) {
                businessName.text = if (item.businessName.isNotBlank()) item.businessName else "Negocio"
                date.text = "${item.dayName} - ${item.date}"
                if (item.reservationTime.isNotBlank()) {
                    time.text = item.reservationTime
                    time.visibility = View.VISIBLE
                } else {
                    time.visibility = View.GONE
                }
                userLabel.text = "Reserva de $userDisplayName"
                userPhoto.load(userPhotoUri.ifBlank { null }) {
                    placeholder(R.drawable.ic_profile_placeholder)
                    error(R.drawable.ic_profile_placeholder)
                    fallback(R.drawable.ic_profile_placeholder)
                    transformations(CircleCropTransformation())
                }
                cancel.isEnabled = true
                cancel.text = "Cancelar"
                cancel.alpha = 1f
                cancel.setOnClickListener {
                    cancel.isEnabled = false
                    cancel.text = "Cancelando..."
                    cancel.alpha = 0.7f
                    onCancelClick(item, cancel)
                }
            }
        }
    }
}
