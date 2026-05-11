package com.example.tfgfrontend.ui.business

import android.net.Uri
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
import com.example.tfgfrontend.R
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

data class BusinessItem(
    val businessName: String = "",
    val logoUri: String = "",
    val todayOpen: String = "",
    val todayClose: String = ""
)

class BusinessListFragment : Fragment(R.layout.fragment_business_list) {

    private val businessAdapter = BusinessAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            findNavController().navigate(R.id.startFragment)
        }

        val recycler = view.findViewById<RecyclerView>(R.id.rvBusinesses)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = businessAdapter

        FirebaseFirestore.getInstance().collection("businesses")
            .get()
            .addOnSuccessListener { result ->
                val businesses = result.documents.map { doc ->
                    val schedule = doc.get("schedule") as? Map<*, *>
                    val todayKey = getTodayKey()
                    val todayHours = schedule?.get(todayKey) as? Map<*, *>

                    BusinessItem(
                        businessName = doc.getString("businessName").orEmpty(),
                        logoUri = doc.getString("logoUri").orEmpty(),
                        todayOpen = todayHours?.get("open")?.toString().orEmpty(),
                        todayClose = todayHours?.get("close")?.toString().orEmpty()
                    )
                }.filter { it.businessName.isNotBlank() }

                businessAdapter.submitList(businesses)
            }
            .addOnFailureListener {
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

    private class BusinessAdapter : RecyclerView.Adapter<BusinessAdapter.BusinessViewHolder>() {
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
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        class BusinessViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val logo = itemView.findViewById<ImageView>(R.id.ivBusinessItemLogo)
            private val name = itemView.findViewById<TextView>(R.id.tvBusinessItemName)
            private val todayHours = itemView.findViewById<TextView>(R.id.tvBusinessItemTodayHours)

            fun bind(item: BusinessItem) {
                name.text = item.businessName
                todayHours.text = if (item.todayOpen.isNotBlank() && item.todayClose.isNotBlank()) {
                    "Hoy: ${item.todayOpen} - ${item.todayClose}"
                } else {
                    "Hoy: horario no disponible"
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
