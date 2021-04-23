package com.mad.carpooling.ui.trip_edit

import android.app.Activity
import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.icu.util.Calendar
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.widget.*
import androidx.activity.addCallback
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.button.MaterialButton
import com.mad.carpooling.R
import com.mad.carpooling.TripUtil

class TripEditFragment : Fragment(R.layout.fragment_trip_edit) {

    private lateinit var tripEditViewModel: TripEditViewModel
    private lateinit var trip: TripUtil.Trip

    private lateinit var optionsMenu: Menu

    private lateinit var tvDate : TextView
    private lateinit var tvTime : TextView
    private lateinit var etDepartureLocation : EditText
    private lateinit var etArrivalLocation : EditText
    private lateinit var etDuration : EditText
    private lateinit var etSeats : EditText
    private lateinit var etPrice : EditText
    private lateinit var etDescription : EditText
    private lateinit var ibtn_chattiness : ImageButton
    private lateinit var ibtn_smoking : ImageButton
    private lateinit var ibtn_pets : ImageButton
    private lateinit var ibtn_music : ImageButton
    var chattiness = false
    var smoking = false
    var pets = false
    var music = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)

        val callback = requireActivity().onBackPressedDispatcher.addCallback(this) {
            activity?.setResult(Activity.RESULT_CANCELED)
            findNavController().navigate(
                R.id.action_nav_trip_edit_to_nav_trip_details
            )
        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvDate = view.findViewById(R.id.tv_tripEdit_date)
        tvTime = view.findViewById(R.id.tv_tripEdit_time)
        etDepartureLocation = view.findViewById(R.id.et_departure)
        etArrivalLocation = view.findViewById(R.id.et_arrival)
        etDuration = view.findViewById(R.id.et_duration)
        etSeats = view.findViewById(R.id.et_seats)
        etPrice = view.findViewById(R.id.et_price)
        etDescription = view.findViewById(R.id.et_description)
        ibtn_chattiness = view.findViewById(R.id.btn_edit_chattiness)
        ibtn_smoking = view.findViewById(R.id.btn_edit_smoking)
        ibtn_pets = view.findViewById(R.id.btn_edit_pets)
        ibtn_music = view.findViewById(R.id.btn_edit_music)

        initTrip()

    /*    ibtn_chattiness.setOnClickListener {
            chattiness = changeStatePreference(chattiness, ibtn_chattiness)
        }*/
        ibtn_smoking.setOnClickListener {
            smoking = changeStatePreference(smoking, ibtn_smoking)
        }
        ibtn_pets.setOnClickListener {
            pets = changeStatePreference(pets, ibtn_pets)
        }
        ibtn_music.setOnClickListener {
            music = changeStatePreference(music, ibtn_music)
        }

        val btn_date = view.findViewById<MaterialButton>(R.id.edit_date)
        btn_date.setOnClickListener { showDatePickerDialog(view) }

        val btn_time = view.findViewById<MaterialButton>(R.id.edit_time)
        btn_time.setOnClickListener { showTimePickerDialog(view) }

    }

    private fun initTrip() {
        val args: TripEditFragmentArgs by navArgs()
        val tripId = args.id
        val bundle = args.stops
        val stops =
            bundle?.getSerializable("stops") as HashMap<Int, String>

        trip = TripUtil.Trip(
            args.id,
            "args.nickname",
            args.departure,
            args.arrival,
            args.duration,
            args.depDate,
            args.depTime,
            args.seats,
            args.price,
            false, false, false, false,
            args.description,
            stops
        )

        tvDate.text = trip.depDate
        tvTime.text = trip.depTime
        etDepartureLocation.setText(trip.departure)
        etArrivalLocation.setText(trip.arrival)
        etDuration.setText(trip.duration)
        etSeats.setText(trip.seats.toString())
        etPrice.setText(trip.price.toString())
        etDescription.setText(trip.description)

        chattiness = trip.chattiness
        smoking = trip.smoking
        pets = trip.pets
        music = trip.music

        initPreferences()
    }

    private fun initPreferences(){
        chattiness = changeStatePreference(chattiness, ibtn_chattiness)
        smoking = changeStatePreference(smoking, ibtn_smoking)
        pets = changeStatePreference(pets, ibtn_pets)
        music = changeStatePreference(music, ibtn_music)
    }

    private fun changeStatePreference(state : Boolean, btn : ImageButton) : Boolean{

        // TODO: substitute R.color.blue_700 with ?attr/colorPrimary

        if(state){
            val typedValue = TypedValue()
            val theme = requireContext().theme
            theme.resolveAttribute(R.attr.colorControlActivated, typedValue, true)
            @ColorInt val color = typedValue.data

            btn.setBackgroundResource(R.drawable.shape_preference_enabled)
            btn.setColorFilter(color)
        }
        else{
            btn.setBackgroundResource(R.drawable.shape_preference_disabled)
            btn.setColorFilter(ContextCompat.getColor(requireContext(), R.color.pref_disabled))
        }
        return !state
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_trip_edit, menu)
        optionsMenu = menu
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.save_trip -> {
                Log.d("CONDITION", "inside SAVE")
                val bundle = Bundle()
                bundle.putString("save_departureLocation.group05.lab2", etDepartureLocation.text.trim().toString())
                bundle.putString("save_departureDate.group05.lab2", tvDate.text.trim().toString())
                bundle.putString("save_departureTime.group05.lab2", tvTime.text.trim().toString())
                bundle.putString("save_arrivalLocation.group05.lab2", etArrivalLocation.text.trim().toString())
                bundle.putString("save_duration.group05.lab2", etDuration.text.trim().toString())
                bundle.putString("save_seats.group05.lab2", etSeats.text.trim().toString())
                bundle.putString("save_price.group05.lab2", etPrice.text.trim().toString())
                bundle.putString("save_description.group05.lab2", etDescription.text.trim().toString())
                bundle.putBoolean("save_chattiness.group05.lab2", chattiness)
                bundle.putBoolean("save_smoking.group05.lab2", smoking)
                bundle.putBoolean("save_pets.group05.lab2", pets)
                bundle.putBoolean("save_music.group05.lab2", music)

//                if (currentPhotoPath != null) { //TODO is this necessary?
//                    bundle.putString("save_profilePic.group05.lab1", currentPhotoPath)
//                }
//
//                saveToSharedPref()
                findNavController().navigate(
                    R.id.action_nav_trip_edit_to_nav_trip_details,
                    bundle
                )
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    }

    class DatePickerFragment(tvDate: TextView) : DialogFragment(), DatePickerDialog.OnDateSetListener {

        val tvDate = tvDate;
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            // Use the current date as the default date in the picker
            val c = Calendar.getInstance()
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)

            // Create a new instance of DatePickerDialog and return it
            return DatePickerDialog(requireContext(), this, year, month, day)
        }

        override fun onDateSet(view: DatePicker, year: Int, month: Int, day: Int) {
            var day = day.toString()
            var month = month.toString()
            var year = year.toString()
            tvDate.text = "$day/$month/$year"
        }

    }

    private fun showDatePickerDialog(v: View) {
        val dateFragment = DatePickerFragment(tvDate)
        dateFragment.show(requireActivity().supportFragmentManager, "datePicker")
        // should wait for fragment to close and then update the textView?
        //tvDate.text = dateFragment.date //TODO: too fast
    }

    class TimePickerFragment : DialogFragment(), TimePickerDialog.OnTimeSetListener {

        var time = ""

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            // Use the current time as the default values for the picker
            val c = Calendar.getInstance()
            val hour = c.get(Calendar.HOUR_OF_DAY)
            val minute = c.get(Calendar.MINUTE)

            // Create a new instance of TimePickerDialog and return it
            return TimePickerDialog(activity, this, hour, minute, DateFormat.is24HourFormat(activity))
        }

        override fun onTimeSet(view: TimePicker, hourOfDay: Int, minute: Int) {
            var hour = hourOfDay.toString()
            var minute = minute.toString()
            time = "$hour:$minute"
        }
    }

    private fun showTimePickerDialog(v: View) {
        val timeFragment = TimePickerFragment()
        timeFragment.show(requireActivity().supportFragmentManager, "timePicker")
        // should wait for fragment to close and then update the textView?
        tvTime.text = timeFragment.time //TODO: too fast
    }



}