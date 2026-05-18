package com.example.tfgfrontend.ui.business

import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import android.graphics.drawable.ColorDrawable
import android.graphics.Color
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tfgfrontend.R
import com.example.tfgfrontend.data.SessionPrefs
import coil.load
import coil.transform.CircleCropTransformation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

data class BusinessItem(
    val id: String = "",
    val businessName: String = "",
    val logoUri: String = "",
    val todayOpen: String = "",
    val todayClose: String = ""
)

class BusinessListFragment : Fragment(R.layout.fragment_business_list) {

    private val businessAdapter = BusinessAdapter { business ->
        findNavController().navigate(
            R.id.businessCalendarFragment,
            Bundle().apply {
                putString("businessId", business.id)
                putString("businessName", business.businessName)
            }
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            findNavController().navigate(R.id.startFragment)
        }

        val recycler = view.findViewById<RecyclerView>(R.id.rvBusinesses)
        val loadingText = view.findViewById<TextView>(R.id.tvLoadingBusinesses)
        val profileMenu = view.findViewById<ImageView>(R.id.ivProfileMenu)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = businessAdapter

        val userPhoto = FirebaseAuth.getInstance().currentUser?.photoUrl
        if (userPhoto != null) {
            profileMenu.load(userPhoto) {
                placeholder(R.drawable.ic_profile_placeholder)
                error(R.drawable.ic_profile_placeholder)
                transformations(CircleCropTransformation())
            }
        } else {
            profileMenu.setImageResource(R.drawable.ic_profile_placeholder)
        }

        profileMenu.setOnClickListener { anchor ->
            val popupView = LayoutInflater.from(requireContext())
                .inflate(R.layout.view_profile_menu, null, false)

            val popupWindow = PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
            )
            popupWindow.elevation = 12f
            popupWindow.isOutsideTouchable = true
            popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            popupWindow.animationStyle = R.style.ProfileMenuPopupAnimation

            val userEmail = FirebaseAuth.getInstance().currentUser?.email.orEmpty()
            val displayName = userEmail.substringBefore("@").ifBlank { "Usuario" }
            popupView.findViewById<TextView>(R.id.tvProfileName).text = displayName
            popupView.findViewById<TextView>(R.id.tvProfileEmail).text = userEmail

            popupView.findViewById<TextView>(R.id.tvMenuMyReservations).setOnClickListener {
                popupWindow.dismiss()
                findNavController().navigate(R.id.myReservationsFragment)
            }

            popupView.findViewById<TextView>(R.id.tvMenuLogout).setOnClickListener {
                popupWindow.dismiss()
                FirebaseAuth.getInstance().signOut()
                findNavController().navigate(R.id.startFragment)
            }

            popupView.findViewById<TextView>(R.id.tvMenuForgetDevice).setOnClickListener {
                popupWindow.dismiss()
                SessionPrefs.clearUserCredentials(requireContext())
                SessionPrefs.clearBusinessCredentials(requireContext())
                Toast.makeText(requireContext(), "Este equipo ya no recordará tus datos", Toast.LENGTH_SHORT).show()
            }

            popupWindow.showAsDropDown(anchor, -20, 8, Gravity.END)
        }

        FirebaseFirestore.getInstance().collection("businesses")
            .get()
            .addOnSuccessListener { result ->
                val businesses = result.documents.map { doc ->
                    val schedule = doc.get("schedule") as? Map<*, *>
                    val todayKey = getTodayKey()
                    val todayHours = schedule?.get(todayKey) as? Map<*, *>

                    BusinessItem(
                        id = doc.id,
                        businessName = doc.getString("businessName").orEmpty(),
                        logoUri = doc.getString("logoUri").orEmpty(),
                        todayOpen = todayHours?.get("open")?.toString().orEmpty(),
                        todayClose = todayHours?.get("close")?.toString().orEmpty()
                    )
                }.filter { it.businessName.isNotBlank() }

                businessAdapter.submitList(businesses)
                loadingText.visibility = View.GONE
            }
            .addOnFailureListener {
                loadingText.visibility = View.GONE
                Toast.makeText(requireContext(), "No se pudo cargar la lista de negocios", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getTodayKey(): String {
        return when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "monday"
            Calendar.TUESDAY -> "tuesday"
            Calendar.WEDNESDAY -> "wednesday"
            Calendar.THURSDAY -> "thursday"
            Calendar.FRIDAY -> "friday"
            Calendar.SATURDAY -> "saturday"
            Calendar.SUNDAY -> "sunday"
            else -> "monday"
        }
    }

    private class BusinessAdapter(
        private val onBusinessClick: (BusinessItem) -> Unit
    ) : RecyclerView.Adapter<BusinessAdapter.BusinessViewHolder>() {
        private val items = mutableListOf<BusinessItem>()

        fun submitList(newItems: List<BusinessItem>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BusinessViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_business, parent, false)
            return BusinessViewHolder(view)
        }

        override fun onBindViewHolder(holder: BusinessViewHolder, position: Int) {
            holder.bind(items[position], onBusinessClick)
        }

        override fun getItemCount(): Int = items.size

        class BusinessViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val logo = itemView.findViewById<ImageView>(R.id.ivBusinessItemLogo)
            private val name = itemView.findViewById<TextView>(R.id.tvBusinessItemName)
            private val todayHours = itemView.findViewById<TextView>(R.id.tvBusinessItemTodayHours)
            private val card = itemView.findViewById<View>(R.id.cardBusinessItem)

            fun bind(item: BusinessItem, onBusinessClick: (BusinessItem) -> Unit) {
                name.text = item.businessName
                todayHours.text = if (item.todayOpen.isNotBlank() && item.todayClose.isNotBlank()) {
                    "Hoy: ${item.todayOpen} - ${item.todayClose}"
                } else {
                    "Hoy: horario no disponible"
                }

                card.setOnClickListener {
                    onBusinessClick(item)
                }

                if (item.logoUri.isNotBlank()) {
                    try {
                        logo.setImageURI(Uri.parse(item.logoUri))
                    } catch (_: Exception) {
                        logo.setImageResource(R.drawable.listme)
                    }
                } else {
                    logo.setImageResource(R.drawable.listme)
                }
            }
        }
    }
}
