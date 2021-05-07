package com.mad.carpooling.ui.trip_details

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.onNavDestinationSelected
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.mad.carpooling.R
import com.mad.carpooling.data.Trip
import com.mad.carpooling.ui.SharedViewModel
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*


class TripDetailsFragment : Fragment(R.layout.fragment_trip_details) {

    private val model: SharedViewModel by activityViewModels()
    private lateinit var trip: Trip
    private lateinit var ivCarPic: ImageView
    private lateinit var ivProfilePic: ImageView
    private lateinit var tvDepartureLocation: TextView
    private lateinit var tvDepartureDate: TextView
    private lateinit var tvDepartureTime: TextView
    private lateinit var tvArrivalLocation: TextView
    private lateinit var tvDuration: TextView
    private lateinit var tvSeats: TextView
    private lateinit var tvPrice: TextView
    private lateinit var tvDescription: TextView
    private lateinit var ibtnChattiness: ImageButton
    private lateinit var ibtnSmoking: ImageButton
    private lateinit var ibtnPets: ImageButton
    private lateinit var ibtnMusic: ImageButton
    private lateinit var optionsMenu: Menu
    private lateinit var tvNickname: TextView
    private lateinit var tripMap: HashMap<String, Trip>
    private var chattiness = false
    private var smoking = false
    private var pets = false
    private var music = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ivCarPic = view.findViewById(R.id.iv_tripDetails_car_pic)
        ivProfilePic = view.findViewById(R.id.iv_tripDetails_profile_pic)
        tvDepartureLocation = view.findViewById(R.id.tv_tripDetails_departureLocation)
        tvDepartureDate = view.findViewById(R.id.tv_tripDetails_departureDate)
        tvDepartureTime = view.findViewById(R.id.tv_tripDetails_departureTime)
        tvArrivalLocation = view.findViewById(R.id.tv_tripDetails_arrivalLocation)
        tvDuration = view.findViewById(R.id.tv_tripDetails_duration)
        tvSeats = view.findViewById(R.id.tv_tripDetails_seats)
        tvPrice = view.findViewById(R.id.tv_tripDetails_price)
        tvDescription = view.findViewById(R.id.tv_tripDetails_description_text)
        ibtnChattiness = view.findViewById(R.id.btn_tripDetails_chattiness)
        ibtnSmoking = view.findViewById(R.id.btn_tripDetails_smoking)
        ibtnPets = view.findViewById(R.id.btn_tripDetails_pets)
        ibtnMusic = view.findViewById(R.id.btn_tripDetails_music)
        tvNickname = view.findViewById(R.id.tv_tripDetails_fullName)


        initTripDetails(view)
    }

    @SuppressLint("SetTextI18n")
    private fun initTripDetails(view: View) {
        val args: TripDetailsFragmentArgs by navArgs()
        val db = Firebase.firestore

        tripMap = model.getTrips().value!!

        trip = tripMap[args.id]!!

        Log.e("INFO", trip.owner!!.id)

        // ivCarPic to be init from remote resource
//        if (trip.imageCarURL != null) {
//            BitmapFactory.decodeFile(trip.imageCarURL)?.also { bitmap ->
//                ivCarPic.setImageBitmap(bitmap)
//            }
//        }
        if (trip.imageCarURL != "") {
            Glide.with(requireContext()).load(trip.imageCarURL).into(ivCarPic)
        }

        val userDoc = db.collection("users").document(trip.owner!!.id).get().addOnSuccessListener{
            tvNickname.text = it.get("nickname").toString()
            Glide.with(requireContext()).load(it.get("imageUserRef")).into(ivProfilePic)
        }
        tvDepartureLocation.text = trip.departure
        tvArrivalLocation.text = trip.arrival
        tvDepartureDate.text =
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(trip.timestamp.toDate())
                .toString()
        tvDepartureTime.text =
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(trip.timestamp.toDate())
                .toString()
        tvDuration.text = trip.duration
        tvSeats.text = trip.seats.toString()
        tvPrice.text = "%.2f".format(trip.price)
        tvDescription.text = trip.description
        chattiness = trip.chattiness
        smoking = trip.smoking
        pets = trip.pets
        music = trip.music

        initPreferences()

        val rv = view.findViewById<RecyclerView>(R.id.rv_tripDetails_stops)
        rv.layoutManager = LinearLayoutManager(context)
        val stopAdapter = StopAdapter(trip.stops)
        rv.adapter = stopAdapter
        if (stopAdapter.itemCount == 0) {
            val tripStopsTitle = view.findViewById<TextView>(R.id.tv_tripDetails_stops)
            tripStopsTitle.visibility = View.GONE
        }

    }

    private fun editTrip() {

        val action = TripDetailsFragmentDirections.actionNavTripDetailsToNavTripEdit(
            trip.id,
            isNew = false
        )

        findNavController().navigate(action)
    }

    private fun initPreferences() {
        chattiness = changeStatePreference(chattiness, ibtnChattiness)
        smoking = changeStatePreference(smoking, ibtnSmoking)
        pets = changeStatePreference(pets, ibtnPets)
        music = changeStatePreference(music, ibtnMusic)
    }

    private fun changeStatePreference(state: Boolean, ibtn: ImageButton): Boolean {
        val typedValue = TypedValue()
        val theme = requireContext().theme

        val color: Int = if (state) {
            theme.resolveAttribute(R.attr.colorControlActivated, typedValue, true)
            typedValue.data
        } else {
            theme.resolveAttribute(R.attr.colorControlNormal, typedValue, true)
            typedValue.data
        }
        ibtn.isSelected = state
        ibtn.setColorFilter(color)
        return state
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.edit_trip -> {
                editTrip()
                true
            }
            else -> item.onNavDestinationSelected(findNavController()) || super.onOptionsItemSelected(
                item
            )
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        optionsMenu.findItem(R.id.edit_trip).isVisible = trip.owner!!.id == model.getCurrentUser().value?.uid
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_trip_details, menu)
        optionsMenu = menu
        super.onCreateOptionsMenu(menu, inflater)
    }

}

class StopAdapter(private val stops: ArrayList<String>?) :
    RecyclerView.Adapter<StopAdapter.StopViewHolder>() {

    class StopViewHolder(v: View) : RecyclerView.ViewHolder(v) {

        private var stopId: TextView = v.findViewById(R.id.stopId)
        private var stopName: TextView = v.findViewById(R.id.stopName)
        private var stopDate: TextView = v.findViewById(R.id.stopDate)
        private var stopTime: TextView = v.findViewById(R.id.stopTime)

        fun bind(stop: String?, position: Int) {
            stopId.text = (position + 1).toString()
            val stringArray = stop!!.split(",")
            stopName.text = stringArray[0].trim()
            stopDate.text = stringArray[1].trim()
            stopTime.text = stringArray[2].trim()
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StopViewHolder {
        val layout =
            LayoutInflater.from(parent.context).inflate(R.layout.stop_layout, parent, false)
        return StopViewHolder(layout)
    }

    override fun onBindViewHolder(holder: StopViewHolder, position: Int) {
        holder.bind(stops?.get(position), position)
    }

    override fun getItemCount(): Int {
        return stops?.size ?: 0
    }

}
