package com.example.tfgfrontend.ui.business

import android.os.Bundle
import android.view.View
import android.widget.CalendarView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.app.TimePickerDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.tfgfrontend.R
import com.example.tfgfrontend.domain.ScheduleValidator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import java.util.Locale

class BusinessCalendarFragment : Fragment(R.layout.fragment_business_calendar) {

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private var businessId: String = ""
    private var businessName: String = ""
    private var selectedYear = 0
    private var selectedMonth = 0
    private var selectedDay = 0
    private var isReserving = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        businessId = arguments?.getString("businessId").orEmpty()
        businessName = arguments?.getString("businessName").orEmpty()

        if (businessId.isBlank()) {
            Toast.makeText(requireContext(), "Negocio no válido", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }

        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            findNavController().navigateUp()
        }

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

        view.findViewById<TextView>(R.id.tvBusinessCalendarTitle).text =
            if (businessName.isNotBlank()) "Calendario de $businessName" else "Calendario"

        loadBusinessLogo(view)

        val calendarView = view.findViewById<CalendarView>(R.id.calendarBusiness)
        val today = Calendar.getInstance()
        calendarView.minDate = today.timeInMillis
        selectedYear = today.get(Calendar.YEAR)
        selectedMonth = today.get(Calendar.MONTH)
        selectedDay = today.get(Calendar.DAY_OF_MONTH)
        renderSelectedDay(view, selectedYear, selectedMonth, selectedDay)

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedYear = year
            selectedMonth = month
            selectedDay = dayOfMonth
            renderSelectedDay(view, year, month, dayOfMonth)
        }

        view.findViewById<View>(R.id.btnReserve).setOnClickListener {
            reserveSelectedDay()
        }

        view.findViewById<View>(R.id.btnMyReservations).setOnClickListener {
            findNavController().navigate(R.id.myReservationsFragment)
        }
    }

    private fun loadBusinessLogo(view: View) {
        firestore.collection("businesses").document(businessId).get()
            .addOnSuccessListener { businessDoc ->
                val logoView = view.findViewById<ImageView>(R.id.ivBusinessCalendarLogo)
                val aboutView = view.findViewById<TextView>(R.id.tvBusinessAbout)
                val logoUri = businessDoc.getString("logoUri").orEmpty()
                val logoManuallySelected = businessDoc.getBoolean("logoManuallySelected") ?: false
                val aboutText = businessDoc.getString("aboutBusiness").orEmpty().trim()
                if (!logoManuallySelected) {
                    logoView.setImageResource(R.drawable.listme)
                } else {
                    logoView.load(logoUri.ifBlank { null }) {
                        placeholder(R.drawable.listme)
                        error(R.drawable.listme)
                        fallback(R.drawable.listme)
                    }
                }

                if (aboutText.isNotBlank()) {
                    aboutView.text = aboutText
                    aboutView.visibility = View.VISIBLE
                } else {
                    aboutView.visibility = View.GONE
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "No se pudo cargar el negocio", Toast.LENGTH_SHORT).show()
            }
    }

    private fun renderSelectedDay(view: View, year: Int, month: Int, day: Int) {
        val tvSelectedDate = view.findViewById<TextView>(R.id.tvSelectedDate)
        val dateText = String.format(Locale.getDefault(), "%02d/%02d/%04d", day, month + 1, year)
        val dayName = getDayName(year, month, day)
        tvSelectedDate.text = "Día seleccionado: $dayName, $dateText"
    }

    private fun reserveSelectedDay() {
        if (isReserving) return

        val userId = auth.currentUser?.uid
        if (userId.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Debes iniciar sesión para reservar", Toast.LENGTH_SHORT).show()
            return
        }

        if (businessId.isBlank()) {
            Toast.makeText(requireContext(), "Negocio no válido", Toast.LENGTH_SHORT).show()
            return
        }

        val dateKey = String.format(Locale.getDefault(), "%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)

        val selectedDate = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth)
            set(Calendar.DAY_OF_MONTH, selectedDay)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val todayDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (selectedDate.before(todayDate)) {
            Toast.makeText(requireContext(), "No puedes reservar días pasados", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedDayKey = getDayKey(selectedYear, selectedMonth, selectedDay)
        val isToday = selectedDate.timeInMillis == todayDate.timeInMillis

        firestore.collection("businesses").document(businessId).get()
            .addOnSuccessListener { businessDoc ->
                val schedule = businessDoc.get("schedule") as? Map<*, *>
                val daySchedule = schedule?.get(selectedDayKey) as? Map<*, *>
                val openTime = daySchedule?.get("open")?.toString().orEmpty()
                val closeTime = daySchedule?.get("close")?.toString().orEmpty()

                if (!ScheduleValidator.isValidHour(openTime) || !ScheduleValidator.isValidHour(closeTime)) {
                    Toast.makeText(requireContext(), "Horario inválido para este día", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                if (!ScheduleValidator.isOpenBeforeClose(openTime, closeTime)) {
                    Toast.makeText(requireContext(), "Horario no disponible para este día", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                if (isToday) {
                    when {
                        isBeforeBusinessOpen(openTime) -> {
                            Toast.makeText(requireContext(), "No puedes reservar todavía: el negocio aún no ha abierto", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }
                        isBusinessClosedNow(closeTime) -> {
                            Toast.makeText(requireContext(), "No puedes reservar hoy: el negocio ya está cerrado", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }
                    }
                }

                showTimePickerWithinBusinessHours(
                    openTime = openTime,
                    closeTime = closeTime,
                    isToday = isToday
                ) { selectedTime ->
                    val reservationId = "${businessId}_${userId}_${dateKey}_$selectedTime"
                    val reservation = hashMapOf(
                        "businessId" to businessId,
                        "businessName" to businessName,
                        "userId" to userId,
                        "userEmail" to auth.currentUser?.email.orEmpty(),
                        "userDisplayName" to auth.currentUser?.email.orEmpty().substringBefore("@"),
                        "userPhotoUrl" to auth.currentUser?.photoUrl?.toString().orEmpty(),
                        "date" to dateKey,
                        "dayName" to getDayName(selectedYear, selectedMonth, selectedDay),
                        "reservationTime" to selectedTime,
                        "createdAt" to System.currentTimeMillis()
                    )

                    setReserveLoading(true)
                    firestore.collection("reservations")
                        .document(reservationId)
                        .set(reservation)
                        .addOnSuccessListener {
                            setReserveLoading(false)
                            Toast.makeText(requireContext(), "Reserva realizada correctamente", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            setReserveLoading(false)
                            Toast.makeText(requireContext(), "No se pudo guardar la reserva", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "No se pudo validar el horario del negocio", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showTimePickerWithinBusinessHours(
        openTime: String,
        closeTime: String,
        isToday: Boolean,
        onValidTimeSelected: (String) -> Unit
    ) {
        val openMinutes = toMinutes(openTime) ?: run {
            Toast.makeText(requireContext(), "Horario inválido para este día", Toast.LENGTH_SHORT).show()
            return
        }
        val closeMinutes = toMinutes(closeTime) ?: run {
            Toast.makeText(requireContext(), "Horario inválido para este día", Toast.LENGTH_SHORT).show()
            return
        }

        val startHour = openMinutes / 60
        val startMinute = openMinutes % 60

        TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                val selectedMinutes = hourOfDay * 60 + minute
                val now = Calendar.getInstance()
                val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

                val isWithinBusinessHours = selectedMinutes in openMinutes..closeMinutes
                if (!isWithinBusinessHours) {
                    Toast.makeText(
                        requireContext(),
                        "Elige un horario entre $openTime y $closeTime",
                        Toast.LENGTH_SHORT
                    ).show()
                    showTimePickerWithinBusinessHours(openTime, closeTime, isToday, onValidTimeSelected)
                    return@TimePickerDialog
                }

                if (isToday && selectedMinutes < nowMinutes) {
                    Toast.makeText(
                        requireContext(),
                        "Elige una hora actual o futura",
                        Toast.LENGTH_SHORT
                    ).show()
                    showTimePickerWithinBusinessHours(openTime, closeTime, isToday, onValidTimeSelected)
                    return@TimePickerDialog
                }

                onValidTimeSelected(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute))
            },
            startHour,
            startMinute,
            true
        ).show()
    }

    private fun toMinutes(value: String): Int? {
        val parts = value.split(":")
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        return hour * 60 + minute
    }

    private fun setReserveLoading(loading: Boolean) {
        isReserving = loading
        view?.findViewById<TextView>(R.id.btnReserve)?.apply {
            isEnabled = !loading
            text = if (loading) "Reservando..." else "Reservar"
            alpha = if (loading) 0.7f else 1f
        }
        view?.findViewById<View>(R.id.btnBack)?.isEnabled = !loading
        view?.findViewById<View>(R.id.btnMyReservations)?.isEnabled = !loading
    }

    private fun isBusinessClosedNow(closeTime: String): Boolean {
        val closeParts = closeTime.split(":")
        if (closeParts.size != 2) return false

        val closeHour = closeParts[0].toIntOrNull() ?: return false
        val closeMinute = closeParts[1].toIntOrNull() ?: return false

        val now = Calendar.getInstance()
        val closeCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, closeHour)
            set(Calendar.MINUTE, closeMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        return now.after(closeCalendar)
    }

    private fun isBeforeBusinessOpen(openTime: String): Boolean {
        val openParts = openTime.split(":")
        if (openParts.size != 2) return false

        val openHour = openParts[0].toIntOrNull() ?: return false
        val openMinute = openParts[1].toIntOrNull() ?: return false

        val now = Calendar.getInstance()
        val openCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, openHour)
            set(Calendar.MINUTE, openMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        return now.before(openCalendar)
    }

    private fun getDayKey(year: Int, month: Int, day: Int): String {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, day)
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
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

    private fun getDayName(year: Int, month: Int, day: Int): String {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, day)
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Lunes"
            Calendar.TUESDAY -> "Martes"
            Calendar.WEDNESDAY -> "Miércoles"
            Calendar.THURSDAY -> "Jueves"
            Calendar.FRIDAY -> "Viernes"
            Calendar.SATURDAY -> "Sábado"
            Calendar.SUNDAY -> "Domingo"
            else -> "Lunes"
        }
    }
}
