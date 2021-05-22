package com.mad.carpooling.ui.trip_details

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.onNavDestinationSelected
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mad.carpooling.MainActivity
import com.mad.carpooling.R
import com.mad.carpooling.data.Trip
import com.mad.carpooling.ui.SharedViewModel
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
    private lateinit var profileLayout: ConstraintLayout
    private lateinit var fab: ExtendedFloatingActionButton
    private lateinit var bottomSheet: ConstraintLayout
    private lateinit var bsb: BottomSheetBehavior<ConstraintLayout>
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
        profileLayout = view.findViewById(R.id.cl_tripDetails_profile)
        bottomSheet = view.findViewById(R.id.bottom_sheet)
        fab = (activity as MainActivity).findViewById(R.id.fab)
        bsb = BottomSheetBehavior.from(bottomSheet)
        model.getTrips().observe(viewLifecycleOwner, { newTripsMap ->
            // Update the UI
            initTripDetails(newTripsMap, view)
        })
    }

    @SuppressLint("SetTextI18n")
    private fun initTripDetails(newTripsMap: HashMap<String, Trip>, view: View) {

        val args: TripDetailsFragmentArgs by navArgs()
        val db = Firebase.firestore

        trip = newTripsMap[args.id]!!

        Log.d("INFO", trip.owner!!.id)

        // ivCarPic to be init from remote resource
//        if (trip.imageCarURL != null) {
//            BitmapFactory.decodeFile(trip.imageCarURL)?.also { bitmap ->
//                ivCarPic.setImageBitmap(bitmap)
//            }
//        }
        if (trip.imageCarURL != "") {
            Glide.with(view).load(trip.imageCarURL).into(ivCarPic)
        }

        val userDoc =
            db.collection("users").document(trip.owner!!.id).addSnapshotListener { value, e ->
                if (e != null) {
                    Log.e("userDoc exception => ", e.toString())
                    return@addSnapshotListener
                }
                tvNickname.text = value?.get("nickname").toString()
                Glide.with(view).load(value?.get("imageUserRef"))
                    .into(ivProfilePic)
            }
        tvDepartureLocation.text = trip.departure
        tvArrivalLocation.text = trip.arrival
        tvDepartureDate.text =
            (SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM)).format(trip.timestamp.toDate())
        tvDepartureTime.text =
            (SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT)).format(trip.timestamp.toDate())
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

        if (trip.description == "") {
            val descTitle = view.findViewById<TextView>(R.id.tv_tripDetails_description)
            val descMcv = view.findViewById<MaterialCardView>(R.id.mcv_tripDetails_description)
            descTitle.visibility = View.GONE
            descMcv.visibility = View.GONE
        }

        val bsrv = view.findViewById<RecyclerView>(R.id.rv_bottom_sheet)
        bsrv.layoutManager = LinearLayoutManager(context)
        val bottomSheetAdapter = BottomSheetAdapter(trip.interestedPeople, trip)
        bsrv.adapter = bottomSheetAdapter
        Log.d("users:", bottomSheetAdapter.itemCount.toString())

        if (trip.owner?.id != model.getCurrentUser().value?.uid) {
            profileLayout.setOnClickListener {
                val action = TripDetailsFragmentDirections.actionNavTripDetailsToNavShowProfile(
                    trip.owner?.id!!
                )

                findNavController().navigate(action)
            }
        }

        bsb.state = BottomSheetBehavior.STATE_HIDDEN

        initFab(db, view)

        val scrollView = view.findViewById<ScrollView>(R.id.sv_tridDetails)
        scrollView.setOnScrollChangeListener { scrollView, scrollX, scrollY, oldScrollX, oldScrollY ->
            if (scrollY > oldScrollY && fab.visibility == View.VISIBLE && oldScrollY > 0) {
                fab.hide()
                if (bsb.state == BottomSheetBehavior.STATE_EXPANDED) {
                    bsb.state = BottomSheetBehavior.STATE_HIDDEN
                }
            } else if (scrollY < oldScrollY && fab.visibility != View.VISIBLE)
                fab.show()
        }

    }

    private fun initFab(db: FirebaseFirestore, view: View) {
        val value = TypedValue()
        view.context.theme.resolveAttribute(R.attr.colorSecondary, value, true)
        fab.setBackgroundColor(value.data)

        if (trip.owner!!.id != model.getCurrentUser().value?.uid) {

            if (trip.interestedPeople?.contains(model.getCurrentUser().value?.uid)!!) {
                fab.icon =
                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_fullstar)
                fab.text = ""
                if (trip.acceptedPeople?.contains(model.getCurrentUser().value?.uid)!!) {
                    fab.text = "accepted"
                    fab.extend()
                } else {
                    fab.text = ""
                    fab.shrink()
                }
            } else {
                fab.icon = ContextCompat.getDrawable(requireContext(), R.drawable.sl_favourite)
                fab.text = ""
            }

            fab.show()

            fab.setOnClickListener {
                if (trip.interestedPeople?.contains(model.getCurrentUser().value?.uid) == true) {
                    fab.icon = ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.sl_favourite
                    )
                    db.collection("trips").document(trip.id).update(
                        "interestedPeople",
                        FieldValue.arrayRemove(model.getCurrentUser().value?.uid)
                    ).addOnSuccessListener {
                        db.collection("users").document(model.getCurrentUser().value?.uid!!).update(
                            "favTrips", FieldValue.arrayRemove(trip.id)
                        )
                    }
                    if (trip.acceptedPeople?.contains(model.getCurrentUser().value?.uid!!)!!) {
                        db.collection("trips").document(trip.id).update(
                            "acceptedPeople",
                            FieldValue.arrayRemove(model.getCurrentUser().value?.uid!!)
                        ).addOnSuccessListener {
                            db.collection("trips").document(trip.id).update(
                                "seats", FieldValue.increment(1)
                            )
                        }
                        fab.text = ""
                        fab.shrink()
                    }
                } else {
                    fab.icon = ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_baseline_fullstar
                    )

                    db.collection("trips").document(trip.id).update(
                        "interestedPeople", FieldValue.arrayUnion(model.getCurrentUser().value?.uid)
                    ).addOnSuccessListener {
                        db.collection("users").document(model.getCurrentUser().value?.uid!!).update(
                            "favTrips", FieldValue.arrayUnion(trip.id)
                        )
                    }
                }
            }
        } else {
            if (trip.interestedPeople?.size == 0) {
                fab.shrink()
            } else {
                fab.text = "${trip.interestedPeople?.size.toString()} people"
                fab.extend()
            }
            fab.show()

            fab.icon = ContextCompat.getDrawable(
                requireContext(),
                R.drawable.sl_favourite
            )

            fab.setOnClickListener {
                if (bsb.state == BottomSheetBehavior.STATE_HIDDEN)
                    bsb.state = BottomSheetBehavior.STATE_EXPANDED
                else if (bsb.state == BottomSheetBehavior.STATE_EXPANDED)
                    bsb.state = BottomSheetBehavior.STATE_HIDDEN
            }
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
        optionsMenu.findItem(R.id.edit_trip).isVisible =
            trip.owner!!.id == model.getCurrentUser().value?.uid

        optionsMenu.findItem(R.id.visibility_trip).isVisible =
            trip.owner!!.id == model.getCurrentUser().value?.uid

        if (trip.visibility) {
            optionsMenu.findItem(R.id.visibility_trip).setIcon(R.drawable.ic_sharp_visibility);
        } else {
            optionsMenu.findItem(R.id.visibility_trip)
                .setIcon(R.drawable.ic_baseline_visibility_off);
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_trip_details, menu)
        optionsMenu = menu
        super.onCreateOptionsMenu(menu, inflater)
    }

}

class BottomSheetAdapter(private val users: ArrayList<String>?, private val trip: Trip) :
    RecyclerView.Adapter<BottomSheetAdapter.BottomSheetViewHolder>() {

    class BottomSheetViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        private var nickname: TextView = v.findViewById(R.id.tv_bottom_sheet_user)
        private var profile: ConstraintLayout = v.findViewById(R.id.cl_bottom_sheet)
        private var profilePic: ImageView = v.findViewById(R.id.iv_bottomSheet_pic)
        var btnAccept: MaterialButton = v.findViewById(R.id.btn_bottom_sheet)

        fun bind(user: String?, holder: BottomSheetViewHolder) {
            val db = Firebase.firestore
            if (user != null) {
                db.collection("users").document(user).get().addOnSuccessListener {
                    nickname.text = it.get("nickname").toString()
                    Glide.with(holder.itemView).load(it?.get("imageUserRef"))
                        .into(profilePic!!)
                }
                profile.setOnClickListener {
                    val action = TripDetailsFragmentDirections.actionNavTripDetailsToNavShowProfile(
                        user!!
                    )
                    it.findNavController().navigate(action)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BottomSheetViewHolder {
        val layout =
            LayoutInflater.from(parent.context).inflate(R.layout.bottom_sheet_layout, parent, false)
        return BottomSheetViewHolder(layout)
    }

    private fun changeButtonState(state: String, btn: MaterialButton, view: View) {
        if (state.toLowerCase() == "accept") {
            btn.text = "remove"
            btn.setTextColor(ContextCompat.getColor(view.context, R.color.red_700))
            btn.strokeColor = ContextCompat.getColorStateList(view.context, R.color.red_700)
        } else {
            btn.text = "accept"
            btn.setTextColor(ContextCompat.getColor(view.context, R.color.green_700))
            btn.strokeColor = ContextCompat.getColorStateList(view.context, R.color.green_700)
        }
    }

    private fun initButtonState(state: String, btn: MaterialButton, view: View) {
        if (state.toLowerCase() == "accept") {
            btn.text = "accept"
            btn.setTextColor(ContextCompat.getColor(view.context, R.color.green_700))
            btn.strokeColor = ContextCompat.getColorStateList(view.context, R.color.green_700)
        } else {
            btn.text = "remove"
            btn.setTextColor(ContextCompat.getColor(view.context, R.color.red_700))
            btn.strokeColor = ContextCompat.getColorStateList(view.context, R.color.red_700)
        }
    }

    override fun onBindViewHolder(holder: BottomSheetViewHolder, position: Int) {
        val db = Firebase.firestore
        holder.bind(users?.get(position), holder)
        holder.btnAccept.visibility = View.VISIBLE

        if (trip.acceptedPeople?.contains(users?.get(position))!!) {
            initButtonState("remove", holder.btnAccept, holder.itemView)
        } else {
            initButtonState("accept", holder.btnAccept, holder.itemView)
            if(trip.seats>=1){
                holder.btnAccept.isEnabled = true
                holder.btnAccept.alpha = 1f
            }
            else {
                holder.btnAccept.isEnabled = false
                holder.btnAccept.alpha = 0.5f
            }
        }

        holder.btnAccept.setOnClickListener {
            if (holder.btnAccept.text.toString().toLowerCase() == "accept") {
                db.collection("trips").document(trip.id).update(
                    "acceptedPeople", FieldValue.arrayUnion(users?.get(position))
                )
                db.collection("trips").document(trip.id).update(
                    "seats", FieldValue.increment(-1)
                )
                changeButtonState("accept", holder.btnAccept, holder.itemView)
            } else {
                db.collection("trips").document(trip.id).update(
                    "acceptedPeople", FieldValue.arrayRemove(users?.get(position))
                )
                db.collection("trips").document(trip.id).update(
                    "seats", FieldValue.increment(1)
                )
                changeButtonState("remove", holder.btnAccept, holder.itemView)
            }
        }
    }

    override fun getItemCount(): Int {
        return users?.size ?: 0
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
