package com.mad.carpooling.ui.trip_list

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
import android.icu.util.Calendar
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.onNavDestinationSelected
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.slider.RangeSlider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mad.carpooling.MainActivity
import com.mad.carpooling.R
import com.mad.carpooling.data.Trip
import com.mad.carpooling.ui.SharedViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


private lateinit var auth: FirebaseAuth

class OthersTripListFragment : Fragment(R.layout.fragment_trip_list) {
    private lateinit var rv: RecyclerView
    private lateinit var tripMap: HashMap<String, Trip>
    private lateinit var optionsMenu: Menu
    private lateinit var appBarLayout: AppBarLayout
    private lateinit var sliderPrice: RangeSlider
    private lateinit var tvSliderPrice: TextView
    private lateinit var btnSearch: MaterialButton
    private lateinit var btnClear: MaterialButton
    private lateinit var etSearchDeparture: EditText
    private lateinit var etSearchArrival: EditText
    private lateinit var etSearchDate: EditText
    private lateinit var etSearchTime: EditText
    private lateinit var chipSearchResults: Chip
    var searchIsValid: Boolean = false

    private val model: SharedViewModel by activityViewModels()

    // Use the 'by activityViewModels()' Kotlin property delegate
    // from the fragment-ktx artifact

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        chipSearchResults = view.findViewById(R.id.chip_search_results)
        val emptyView = view.findViewById<TextView>(R.id.no_trips_available)
        rv = view.findViewById<RecyclerView>(R.id.triplist_rv)
        rv.layoutManager = LinearLayoutManager(context)
        rv.isNestedScrollingEnabled = false; //prevent toolbar to expand on scroll

        val tripAdapter = OthersTripAdapter()
        rv.adapter = tripAdapter

        initFab(view)

        model.getCurrentUser().observe(viewLifecycleOwner, Observer { currentUser ->
            // update after login/logout
            model.getOthersTrips().observe(viewLifecycleOwner, Observer { newTripsMap ->
                // Update the UI

                tripAdapter.submitList(newTripsMap.values.toList())
                tripMap = newTripsMap
                emptyView.isVisible = newTripsMap.isEmpty()
                initSearch(newTripsMap, tripAdapter)
            })
        })

    }


    private fun initFab(view: View) {

        val fab = (activity as MainActivity).findViewById<ExtendedFloatingActionButton>(R.id.fab)
        fab.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_add)
        fab.shrink()
        fab.show()

        fab.setOnClickListener {
            val action = OthersTripListFragmentDirections.actionNavOthersTripListToNavTripEdit(
                "",
                isNew = true
            )
            val navController = Navigation.findNavController(view)
            navController.navigate(action) //a new one from scratch
        }
        rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0 && fab.visibility == View.VISIBLE) {
                    fab.hide()
                } else if (dy < 0 && fab.visibility != View.VISIBLE) {
                    fab.show()
                }
            }
        })
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_others_trip, menu)
        optionsMenu = menu
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_search -> {
                val isExpanded = (appBarLayout.height - appBarLayout.bottom) != 0
                appBarLayout.setExpanded(isExpanded, true)
                if (isExpanded) {
                    item.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_close)
                    item.iconTintList = ColorStateList.valueOf(Color.WHITE)
                } else {
                    item.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_search)
                    item.iconTintList = ColorStateList.valueOf(Color.WHITE)
                }
                true
            }
            else -> item.onNavDestinationSelected(findNavController()) || super.onOptionsItemSelected(
                item
            )
        }
    }

    private fun initSearch(tripsMap: HashMap<String, Trip>, tripAdapter: OthersTripAdapter) {
        sliderPrice = (activity as MainActivity).findViewById(R.id.range_slider)
        tvSliderPrice = (activity as MainActivity).findViewById(R.id.tv_price_slider)
        btnSearch = (activity as MainActivity).findViewById(R.id.btn_search)
        btnClear = (activity as MainActivity).findViewById(R.id.btn_clear)
        etSearchDeparture = (activity as MainActivity).findViewById(R.id.et_search_departure)
        etSearchArrival = (activity as MainActivity).findViewById(R.id.et_search_arrival)
        etSearchDate = (activity as MainActivity).findViewById(R.id.et_search_date)
        etSearchTime = (activity as MainActivity).findViewById(R.id.et_search_time)

        etSearchDate.setOnClickListener {
            showDatePickerDialog()
        }

        etSearchTime.setOnClickListener {
            showTimePickerDialog()
        }


        sliderPrice.valueFrom = tripsMap.minByOrNull { it.value.price }?.value?.price ?: 0f
        sliderPrice.valueTo = tripsMap.maxByOrNull { it.value.price }?.value?.price ?: 0f
        sliderPrice.values = mutableListOf(sliderPrice.valueFrom, sliderPrice.valueTo)
        tvSliderPrice.text =
            "${("%.2f".format(sliderPrice.valueFrom))} - ${("%.2f".format(sliderPrice.valueTo))} €"
        sliderPrice.addOnChangeListener { slider, value, fromUser ->
            tvSliderPrice.text =
                "${("%.2f".format(slider.values[0]))} - ${("%.2f".format(slider.values[1]))} €"
            btnSearch.isEnabled =
                slider.values[0] != slider.valueFrom || slider.values[1] != slider.valueTo || searchIsValid
        }
        appBarLayout = (activity as MainActivity).findViewById(R.id.appbar_layout) as AppBarLayout

        findNavController().addOnDestinationChangedListener { _, _, _ ->
            appBarLayout.setExpanded(
                false
            )
        }

        validateSearch()
        btnSearch.setOnClickListener {
            tripAdapter.filterTrips(
                etSearchDeparture.text?.trim().toString(),
                etSearchArrival.text?.trim().toString(),
                etSearchDate.text?.trim().toString(),
                etSearchTime.text?.trim().toString(),
                sliderPrice.values,
                tripsMap.values
            )
            val searchItem = optionsMenu.findItem(R.id.action_search)
            searchItem.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_search)
            searchItem.iconTintList = ColorStateList.valueOf(Color.WHITE)
            appBarLayout.setExpanded(false)
            rv.setPadding(0, (40f * Resources.getSystem().displayMetrics.density).toInt(), 0, 0)

            chipSearchResults.visibility = View.VISIBLE
            chipSearchResults.setOnCloseIconClickListener {
                clearSearch(tripAdapter)
                chipSearchResults.visibility = View.GONE
                rv.setPadding(0, 0, 0, 0)
            }
        }
        btnClear.setOnClickListener {
            clearSearch(tripAdapter)
        }
    }

    private fun clearSearch(tripAdapter: OthersTripAdapter) {
        etSearchDeparture.text?.clear()
        etSearchArrival.text?.clear()
        etSearchDate.text?.clear()
        etSearchTime.text?.clear()
        sliderPrice.values = mutableListOf(sliderPrice.valueFrom, sliderPrice.valueTo)
        tripAdapter.submitList(tripMap.values.toList())
    }

    private fun validateSearch() {

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                btnSearch.isEnabled =
                    etSearchDeparture.text.trim().isNotEmpty() || etSearchArrival.text.trim()
                        .isNotEmpty() || etSearchDate.text.trim()
                        .isNotEmpty() || etSearchTime.text.trim()
                        .isNotEmpty()
                searchIsValid =
                    etSearchDeparture.text.trim().isNotEmpty() || etSearchArrival.text.trim()
                        .isNotEmpty() || etSearchDate.text.trim()
                        .isNotEmpty() || etSearchTime.text.trim()
                        .isNotEmpty()
            }

            override fun afterTextChanged(s: Editable?) {
            }

        }

        etSearchDeparture.addTextChangedListener(textWatcher)
        etSearchArrival.addTextChangedListener(textWatcher)
        etSearchDate.addTextChangedListener(textWatcher)
        etSearchTime.addTextChangedListener(textWatcher)
    }

    class OthersTripAdapter() :
        ListAdapter<Trip, OthersTripAdapter.TripViewHolder>(TaskDiffCallback()) {

        class TaskDiffCallback : DiffUtil.ItemCallback<Trip>() {

            override fun areItemsTheSame(oldItem: Trip, newItem: Trip): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Trip, newItem: Trip): Boolean {
                return oldItem == newItem
            }
        }

        class TripViewHolder(v: View) : RecyclerView.ViewHolder(v) {

            val tripRL: RelativeLayout = v.findViewById<RelativeLayout>(R.id.trip_rl)
            val btnStar: CheckBox = v.findViewById(R.id.trip_star)
            private val ivCar = v.findViewById<ImageView>(R.id.trip_car)
            private val location = v.findViewById<TextView>(R.id.trip_from_to)
            private val duration = v.findViewById<TextView>(R.id.trip_duration)
            private val price = v.findViewById<TextView>(R.id.trip_price)

            @SuppressLint("SetTextI18n")
            fun bind(trip: Trip) {
                auth = Firebase.auth
                val user = auth.currentUser
                location.text = "${trip.departure} - ${trip.arrival}"
                duration.text = "Duration: ${trip.duration}"
                price.text = "Price: ${("%.2f".format(trip.price))} €"
                if (trip.imageCarURL != "") {
                    Glide.with(this.itemView).load(trip.imageCarURL).into(ivCar)
                }
                btnStar.isChecked = trip.interestedPeople?.contains(user?.uid) == true
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
            val layout =
                LayoutInflater.from(parent.context).inflate(R.layout.triplist_layout, parent, false)
            return TripViewHolder(layout)
        }

        override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
            auth = Firebase.auth
            val user = auth.currentUser
            val db = Firebase.firestore
            holder.bind(getItem(position))
            holder.tripRL.setOnClickListener {
                val action =
                    OthersTripListFragmentDirections.actionNavOthersTripListToNavTripDetails(
                        getItem(position).id,
                    )
                Navigation.findNavController(holder.tripRL).navigate(action)
            }
            holder.btnStar.visibility = View.VISIBLE
            holder.btnStar.setOnClickListener {
                val action =
                    OthersTripListFragmentDirections.actionNavOthersTripListToNavTripDetails(
                        getItem(position).id,
                    )
                holder.btnStar.setOnCheckedChangeListener { it, isChecked ->
                    if (isChecked) {
                        db.collection("trips").document(getItem(position).id).update(
                            "interestedPeople", FieldValue.arrayUnion(user.uid)
                        ).addOnSuccessListener {
                            db.collection("users").document(user?.uid!!).update(
                                "favTrips", FieldValue.arrayUnion(getItem(position).id)
                            )
                        }
                    } else {
                        db.collection("trips").document(getItem(position).id).update(
                            "interestedPeople", FieldValue.arrayRemove(user.uid)
                        ).addOnSuccessListener {
                            db.collection("users").document(user?.uid!!).update(
                                "favTrips", FieldValue.arrayRemove(getItem(position).id)
                            )
                        }
                    }
                }

            }
        }

        fun filterTrips(
            departure: String,
            arrival: String,
            date: String,
            time: String,
            prices: MutableList<Float>,
            tripList: MutableCollection<Trip>
        ) {
            var formattedDate: String = ""
            var formattedTime: String = ""
            if (date != "") {
                val tmpDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(date)
                formattedDate =
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(tmpDate)
                        .toString()
            }
            if (time != "") {
                val tmpTime = SimpleDateFormat("HH:mm", Locale.getDefault()).parse(time)
                formattedTime =
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(tmpTime).toString()
            }
            val resultList = ArrayList<Trip>()
            for (trip in tripList) {
                val tripDate = SimpleDateFormat(
                    "dd/MM/yyyy",
                    Locale.getDefault()
                ).format(trip.timestamp.toDate())
                    .toString()
                val tripTime =
                    SimpleDateFormat(
                        "HH:mm",
                        Locale.getDefault()
                    ).format(trip.timestamp.toDate())
                        .toString()

                if (trip.departure.toLowerCase(Locale.ROOT)
                        .contains(departure.toLowerCase(Locale.ROOT))
                    && trip.arrival.toLowerCase(Locale.ROOT)
                        .contains(arrival.toLowerCase(Locale.ROOT))
                    && trip.price >= prices[0] && trip.price <= prices[1]
                    && tripDate.contains(formattedDate)
                    && tripTime.contains(formattedTime) //TODO this seems useless, better use a range slider for time too
                ) {
                    resultList.add(trip)
                }
            }
            submitList(resultList)
        }


    }

    class DatePickerFragment(tvDate: TextView) : DialogFragment(),
        DatePickerDialog.OnDateSetListener {

        private val tvDate = tvDate

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            // Use the current date as the default date in the picker
            val c = Calendar.getInstance()
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)

            // Create a new instance of DatePickerDialog and return it
            return DatePickerDialog(requireContext(), this, year, month, day)
        }

        @SuppressLint("SetTextI18n")
        override fun onDateSet(view: DatePicker, year: Int, month: Int, day: Int) {
            tvDate.text = "${day}/${(month + 1)}/${year}"
        }
    }


    private fun showDatePickerDialog() {
        val dateFragment = DatePickerFragment(etSearchDate)
        dateFragment.show(requireActivity().supportFragmentManager, "datePicker")
    }

    class TimePickerFragment(tvTime: TextView) : DialogFragment(),
        TimePickerDialog.OnTimeSetListener {

        private val tvTime = tvTime

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            // Use the current time as the default values for the picker
            val c = Calendar.getInstance()
            val hour = c.get(Calendar.HOUR_OF_DAY)
            val minute = c.get(Calendar.MINUTE)

            // Create a new instance of TimePickerDialog and return it
            return TimePickerDialog(
                activity,
                this,
                hour,
                minute,
                DateFormat.is24HourFormat(activity)
            )
        }

        @SuppressLint("SetTextI18n")
        override fun onTimeSet(view: TimePicker, hourOfDay: Int, minute: Int) {
            if (minute < 10) {
                tvTime.text = "$hourOfDay:0$minute"
            } else {
                tvTime.text = "$hourOfDay:$minute"
            }
        }
    }

    private fun showTimePickerDialog() {
        val timeFragment = TimePickerFragment(etSearchTime)
        timeFragment.show(requireActivity().supportFragmentManager, "timePicker")
    }

}


