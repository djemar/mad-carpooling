package com.mad.carpooling.ui.trip_details

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.onNavDestinationSelected
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mad.carpooling.MainActivity
import com.mad.carpooling.R
import com.mad.carpooling.data.Trip
import com.mad.carpooling.ui.SharedViewModel
import com.mad.carpooling.ui.maps.MapRepository
import com.mad.carpooling.ui.maps.MapUtils
import com.mad.carpooling.ui.maps.MapViewModel
import com.mad.carpooling.ui.maps.MapViewModelFactory
import kotlinx.coroutines.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.FolderOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.util.*
import java.util.stream.Collectors
import kotlin.collections.ArrayList


class TripDetailsFragment : Fragment(R.layout.fragment_trip_details) {

    private val model: SharedViewModel by activityViewModels()
    private lateinit var mapViewModel: MapViewModel
    private lateinit var viewModelFactory: MapViewModelFactory
    private lateinit var trip: Trip
    private lateinit var ivCarPic: ImageView
    private lateinit var ivProfilePic: ImageView
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
    private lateinit var fab: FloatingActionButton
    private lateinit var bottomSheet: ConstraintLayout
    private lateinit var bsb: BottomSheetBehavior<ConstraintLayout>
    private lateinit var map: MapView;
    private lateinit var mapClickOverlay: View;
    private lateinit var btnEndTrip: MaterialButton
    private lateinit var ratingBar: RatingBar
    private var chattiness = false
    private var smoking = false
    private var pets = false
    private var music = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Configuration.getInstance().load(
            requireContext(),
            context?.getSharedPreferences("mad.carpooling.map", Context.MODE_PRIVATE)
        );
        viewModelFactory = MapViewModelFactory(MapRepository())
        mapViewModel = ViewModelProvider(this, viewModelFactory)
            .get(MapViewModel::class.java)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ivCarPic = view.findViewById(R.id.iv_tripDetails_car_pic)
        ivProfilePic = view.findViewById(R.id.iv_tripDetails_profile_pic)
        tvDuration = view.findViewById(R.id.tv_tripDetails_duration)
        tvSeats = view.findViewById(R.id.tv_tripDetails_seats)
        tvPrice = view.findViewById(R.id.tv_tripDetails_price)
        tvDescription = view.findViewById(R.id.tv_tripDetails_description)
        ibtnChattiness = view.findViewById(R.id.btn_tripDetails_chattiness)
        ibtnSmoking = view.findViewById(R.id.btn_tripDetails_smoking)
        ibtnPets = view.findViewById(R.id.btn_tripDetails_pets)
        ibtnMusic = view.findViewById(R.id.btn_tripDetails_music)
        tvNickname = view.findViewById(R.id.tv_tripDetails_fullName)
        profileLayout = view.findViewById(R.id.cl_tripDetails_profile)
        btnEndTrip = view.findViewById(R.id.btn_end_trip)
        ratingBar = view.findViewById<RatingBar>(R.id.rb_tripDetails_driver)
        mapClickOverlay = view.findViewById<View>(R.id.mapClickOverlay)

        bottomSheet = view.findViewById(R.id.bottom_sheet)
        fab = (activity as MainActivity).findViewById(R.id.fab)
        bsb = BottomSheetBehavior.from(bottomSheet)
        map = view.findViewById(R.id.mapDetails)
        model.getTrips().observe(viewLifecycleOwner, { newTripsMap ->
            // Update the UI
            initTripDetails(newTripsMap, view)
            initMap()
            (activity as MainActivity).invalidateOptionsMenu()
        })
    }

    private fun initMap() {
        mapClickOverlay.setOnClickListener {
            val action = TripDetailsFragmentDirections.actionNavTripDetailsToNavMap(
                trip.id
            )
            findNavController().navigate(action)
        }
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setOnTouchListener { _, _ -> //disable touch events
            true
        }

        val waypoints = ArrayList<Marker>()
        val stopsMarkers = FolderOverlay()
        //val stopsMarkers = RadiusMarkerClusterer(requireContext())
        //val clusterIcon: Bitmap? = ContextCompat.getDrawable( requireContext(), R.drawable.marker_cluster)?.toBitmap()
        //stopsMarkers.setIcon(clusterIcon)

        trip.geopoints.stream().forEach { gp ->
            run {
                val marker = Marker(map)
                marker.position = GeoPoint(gp.latitude, gp.longitude)
                waypoints.add(marker)
                stopsMarkers.add(marker)
            }
        }

        MapUtils.redrawMarkers(waypoints, map, requireContext())

        map.post(Runnable() {
            run() {
                val box = MapUtils.computeArea(
                    trip.geopoints.stream().map { gp -> GeoPoint(gp.latitude, gp.longitude) }
                        .collect(Collectors.toList()) as ArrayList<GeoPoint>
                )
                map.zoomToBoundingBox(box, false, 110);
                map.invalidate()
            }
        });

        var routeOverlay: Polyline
        mapViewModel.route.observe(viewLifecycleOwner, { newRouteOverlay ->
            if (newRouteOverlay != null) {
                routeOverlay = newRouteOverlay
                routeOverlay.outlinePaint.strokeWidth = 10f
                routeOverlay.outlinePaint.style = Paint.Style.FILL_AND_STROKE
                routeOverlay.outlinePaint.strokeCap = Paint.Cap.ROUND
                routeOverlay.outlinePaint.strokeJoin = Paint.Join.ROUND
                map.overlays.add(routeOverlay)
                map.overlays.add(stopsMarkers)
                map.invalidate()
            }
        })
        mapViewModel.getRoute(waypoints, requireContext())
    }


    @SuppressLint("SetTextI18n")
    private fun initTripDetails(newTripsMap: HashMap<String, Trip>, view: View) {

        val args: TripDetailsFragmentArgs by navArgs()
        val db = Firebase.firestore

        trip = newTripsMap[args.id]!!

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

        if (trip.description.isEmpty()) {
            tvDescription.text = "No description :("
        }

        val bsrv = view.findViewById<RecyclerView>(R.id.rv_bottom_sheet)
        bsrv.layoutManager = LinearLayoutManager(context)
        val bottomSheetAdapter = BottomSheetAdapter(trip.interestedPeople, trip)
        bsrv.adapter = bottomSheetAdapter
        Log.d("users:", bottomSheetAdapter.itemCount.toString())

        val profileInfo = view.findViewById<TextView>(R.id.tv_tripDetails_fullName)

        if (trip.owner?.id != model.getCurrentUser().value?.uid) {
            profileInfo.setOnClickListener {
                val action = TripDetailsFragmentDirections.actionNavTripDetailsToNavShowProfile(
                    trip.owner?.id!!
                )

                findNavController().navigate(action)
            }
        }

        bsb.state = BottomSheetBehavior.STATE_HIDDEN

        initFab(db, view)

        initBtnEndTripAndRatingBar(db, view)

        val scrollView = view.findViewById<ScrollView>(R.id.sv_tripDetails)
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

    private fun initBtnEndTripAndRatingBar(db: FirebaseFirestore, view: View) {
        if (trip.owner!!.id != model.getCurrentUser().value?.uid) {
            ratingBar.visibility = View.VISIBLE
            btnEndTrip.visibility = View.GONE

            db.collection("ratings").document(trip.owner?.id!!).get()
                .addOnSuccessListener { res ->
                    if (res.exists()) {
                        val mapRatingDriver: Map<String, ArrayList<Any>> =
                            res.get("driverRatings") as Map<String, ArrayList<Any>>
                        var vote: Float = 0f
                        for (array in mapRatingDriver.values)
                            vote = vote + array[0].toString().toFloat()
                        ratingBar.rating = (vote) / (mapRatingDriver.size.toFloat())
                    } else {
                        ratingBar.rating = 0f;
                    }
                }

            ratingBar.setOnTouchListener(View.OnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_UP && trip.finished) {
                    // TODO perform your action here
                    val reviewDial = ReviewDialogFragment(trip, view, "driverRatings", null)
                    reviewDial.show(requireActivity().supportFragmentManager, "driverReviewDialog")
                }
                return@OnTouchListener true
            })
        } else {
            ratingBar.visibility = View.GONE
            btnEndTrip.visibility = View.VISIBLE
            btnEndTrip.isEnabled =
                Calendar.getInstance().time >= trip.timestamp.toDate() && !trip.finished
            btnEndTrip.setOnClickListener() {
                val fragment = EndTripDialogFragment(trip)
                fragment.show(requireActivity().supportFragmentManager, "endTripDialog")
            }
        }
    }

    class EndTripDialogFragment(private var trip: Trip) :
        DialogFragment() {

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            // Use the Builder class for convenient dialog construction
            val builder: AlertDialog.Builder = AlertDialog.Builder(activity)
            val db = Firebase.firestore
            builder.setMessage("Do you really want to end this trip?")
                .setPositiveButton("Confirm", DialogInterface.OnClickListener { dialog, id ->
                    db.collection("trips").document(trip.id).update("finished", true)
                    if (trip.interestedPeople != null) {
                        for (p in trip.interestedPeople!!) {
                            if (!trip.acceptedPeople?.contains(p)!!) {
                                db.collection("trips").document(trip.id)
                                    .update("interestedPeople", FieldValue.arrayRemove(p))
                                db.collection("users").document(p)
                                    .update("favTrips", FieldValue.arrayRemove(trip.id))
                            }
                        }
                    }
                })
                .setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, id ->
                })
            // Create the AlertDialog object and return it
            return builder.create()
        }
    }

    // Review Dialog
    class ReviewDialogFragment(trip: Trip, view: View, var role: String, var passenger: String?) :
        DialogFragment() {
        var tripReview = trip
        var viewReview = view
        var reviewNickname: TextView? = null
        var reviewProfilePic: ImageView? = null
        var etReview: EditText? = null
        var rb_review: RatingBar? = null
        var uid = ""
        private val model: SharedViewModel by activityViewModels()

        override fun onStart() {
            super.onStart()
            reviewProfilePic =
                dialog?.findViewById<ImageView>(R.id.iv_review_profile_pic) as ImageView
            reviewNickname = dialog?.findViewById<TextView>(R.id.tv_review_nickname) as TextView
            etReview = dialog?.findViewById<EditText>(R.id.et_review) as EditText
            rb_review = dialog?.findViewById<RatingBar>(R.id.rt_review_layout) as RatingBar
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            // Use the Builder class for convenient dialog construction

            val db = Firebase.firestore
            val builder: AlertDialog.Builder = AlertDialog.Builder(activity)
            builder.setView(R.layout.review_layout)
            uid = if (passenger != null) passenger as String
            else tripReview.owner!!.id

            db.collection("users").document(uid).addSnapshotListener { value, e ->
                if (e != null) {
                    Log.e("userDoc exception => ", e.toString())
                    return@addSnapshotListener
                }
                reviewNickname?.text = value?.get("nickname").toString()
                Glide.with(viewReview).load(value?.get("imageUserRef"))
                    .into(reviewProfilePic!!)
            }

            builder.setTitle("Add a review")
                .setPositiveButton("Confirm", DialogInterface.OnClickListener { dialog, id ->
                    //TODO: add new value to a map into DB
                    val currentUser = model.getCurrentUser().value?.uid
                    val newArray: ArrayList<Any> =
                        arrayListOf(rb_review!!.rating, etReview?.text?.trim().toString())

                    db.collection("ratings").document(uid).get()
                        .addOnSuccessListener { res ->
                            if (res.exists()) {
                                db.collection("ratings").document(uid)
                                    .update("${role}.${currentUser}", newArray)
                            }
                        }
                })
                .setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, id ->
                })
            // Create the AlertDialog object and return it
            return builder.create()
        }
    }

    private fun initFab(db: FirebaseFirestore, view: View) {
        val value = TypedValue()
        view.context.theme.resolveAttribute(R.attr.themeName, value, true)
        if (value.string == "white")
            fab.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.amber_500)
        else
            fab.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.amber_200)

        if (trip.owner!!.id != model.getCurrentUser().value?.uid) {

            if (trip.interestedPeople?.contains(model.getCurrentUser().value?.uid)!!) {
                fab.setImageDrawable(
                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_fullstar)
                )
                if (trip.acceptedPeople?.contains(model.getCurrentUser().value?.uid)!!) {
                    fab.backgroundTintList =
                        ContextCompat.getColorStateList(requireContext(), R.color.green_700)
                } else {
                    if (value.string == "white")
                        fab.backgroundTintList =
                            ContextCompat.getColorStateList(requireContext(), R.color.amber_500)
                    else
                        fab.backgroundTintList =
                            ContextCompat.getColorStateList(requireContext(), R.color.amber_200)
                }
            } else {
                fab.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.sl_favourite
                    )
                )
            }

            fab.show()

            fab.setOnClickListener {
                if (trip.interestedPeople?.contains(model.getCurrentUser().value?.uid) == true) {
                    fab.setImageDrawable(
                        ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.sl_favourite
                        )
                    )
                    db.collection("trips").document(trip.id).update(
                        "interestedPeople",
                        FieldValue.arrayRemove(model.getCurrentUser().value?.uid)
                    ).addOnSuccessListener {
                        db.collection("users").document(model.getCurrentUser().value?.uid!!)
                            .update(
                                "favTrips", FieldValue.arrayRemove(trip.id)
                            )
                    }

                    if (trip.acceptedPeople?.contains(model.getCurrentUser().value?.uid!!) == true) {
                        db.collection("trips").document(trip.id).update(
                            "acceptedPeople",
                            FieldValue.arrayRemove(model.getCurrentUser().value?.uid!!)
                        ).addOnSuccessListener {
                            db.collection("trips").document(trip.id).update(
                                "seats", FieldValue.increment(1)
                            )
                        }
                    }
                } else {
                    fab.setImageDrawable(
                        ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.ic_baseline_fullstar
                        )
                    )

                    db.collection("trips").document(trip.id).update(
                        "interestedPeople",
                        FieldValue.arrayUnion(model.getCurrentUser().value?.uid)
                    ).addOnSuccessListener {
                        db.collection("users").document(model.getCurrentUser().value?.uid!!)
                            .update(
                                "favTrips", FieldValue.arrayUnion(trip.id)
                            )
                    }
                }
            }
        } else {
            if (trip.interestedPeople?.size == 0) {
                fab.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.sl_favourite
                    )
                )
            } else {
                fab.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_baseline_fullstar
                    )
                )
            }
            fab.show()

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
            trip.owner!!.id == model.getCurrentUser().value?.uid && !trip.finished

        optionsMenu.findItem(R.id.visibility_trip).isVisible =
            trip.owner!!.id == model.getCurrentUser().value?.uid && !trip.finished

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

    override fun onResume() {
        super.onResume();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        Configuration.getInstance().load(
            requireContext(),
            context?.getSharedPreferences("mad.carpooling.map", Context.MODE_PRIVATE)
        )
        map.onResume() //needed for compass, my location overlays, v6.0.0 and up
    }

    override fun onPause() {
        super.onPause();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        Configuration.getInstance().save(
            requireContext(),
            context?.getSharedPreferences("mad.carpooling.map", Context.MODE_PRIVATE)
        )
        map.overlays.clear()
        map.onPause()  //needed for compass, my location overlays, v6.0.0 and up
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
                    val action =
                        TripDetailsFragmentDirections.actionNavTripDetailsToNavShowProfile(
                            user!!
                        )
                    it.findNavController().navigate(action)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BottomSheetViewHolder {
        val layout =
            LayoutInflater.from(parent.context).inflate(R.layout.rv_bottom_sheet, parent, false)
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
        } else if (state.toLowerCase() == "remove") {
            btn.text = "remove"
            btn.setTextColor(ContextCompat.getColor(view.context, R.color.red_700))
            btn.strokeColor = ContextCompat.getColorStateList(view.context, R.color.red_700)
        } else {
            val value = TypedValue()
            view.context.theme.resolveAttribute(R.attr.themeName, value, true)
            btn.text = "rate"

            if (value.string == "white") {
                btn.strokeColor = ContextCompat.getColorStateList(view.context, R.color.blue_700)
                btn.setTextColor(ContextCompat.getColor(view.context, R.color.blue_700))
            } else {
                btn.strokeColor = ContextCompat.getColorStateList(view.context, R.color.blue_300)
                btn.setTextColor(ContextCompat.getColor(view.context, R.color.blue_300))
            }
        }
    }

    override fun onBindViewHolder(holder: BottomSheetViewHolder, position: Int) {
        val db = Firebase.firestore
        holder.bind(users?.get(position), holder)
        holder.btnAccept.visibility = View.VISIBLE

        if (!trip.finished) {
            if (trip.acceptedPeople?.contains(users?.get(position))!!) {
                initButtonState("remove", holder.btnAccept, holder.itemView)
            } else {
                initButtonState("accept", holder.btnAccept, holder.itemView)
                if (trip.seats >= 1) {
                    holder.btnAccept.isEnabled = true
                    holder.btnAccept.alpha = 1f
                } else {
                    holder.btnAccept.isEnabled = false
                    holder.btnAccept.alpha = 0.5f
                }
            }
        } else {
            if (trip.acceptedPeople?.contains(users?.get(position))!!) {
                initButtonState("rate", holder.btnAccept, holder.itemView)
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
            } else if (holder.btnAccept.text.toString().toLowerCase() == "remove") {
                db.collection("trips").document(trip.id).update(
                    "acceptedPeople", FieldValue.arrayRemove(users?.get(position))
                )
                db.collection("trips").document(trip.id).update(
                    "seats", FieldValue.increment(1)
                )
                changeButtonState("remove", holder.btnAccept, holder.itemView)
            } else {
                val fragment = TripDetailsFragment.ReviewDialogFragment(
                    trip, holder.itemView, "passengerRatings",
                    users?.get(position)
                )
                val activity = it.context as AppCompatActivity
                fragment.show(activity.supportFragmentManager, "reviewDialog")
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

        private var stopIcon: ImageView = v.findViewById(R.id.rv_stop_icon)
        private var stopCity: TextView = v.findViewById(R.id.stopCity)
        private var stopDate: TextView = v.findViewById(R.id.stopDate)
        private var stopAddress: TextView = v.findViewById(R.id.stopAddress)
        private var stopTime: TextView = v.findViewById(R.id.stopTime)

        fun bind(stop: String?, position: Int, size: Int?) {
            if (size != null) {
                when (position) {
                    0 -> stopIcon.setImageDrawable(
                        ContextCompat.getDrawable(
                            this.itemView.context,
                            R.drawable.ic_twotone_stop_start
                        )
                    )
                    size - 1 -> stopIcon.setImageDrawable(
                        ContextCompat.getDrawable(
                            this.itemView.context,
                            R.drawable.ic_twotone_stop_end
                        )
                    )
                    else -> stopIcon.setImageDrawable(
                        ContextCompat.getDrawable(
                            this.itemView.context,
                            R.drawable.ic_twotone_stop
                        )
                    )
                }
            }
            val stringArray = stop!!.split(",")
            stopCity.text = stringArray[0].trim()
            stopAddress.text = stringArray[1].trim()
            stopDate.text = stringArray[2].trim()
            stopTime.text = stringArray[3].trim()
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StopViewHolder {
        val layout =
            LayoutInflater.from(parent.context).inflate(R.layout.rv_stop_details, parent, false)
        return StopViewHolder(layout)
    }

    override fun onBindViewHolder(holder: StopViewHolder, position: Int) {
        holder.bind(stops?.get(position), position, stops?.size)
    }

    override fun getItemCount(): Int {
        return stops?.size ?: 0
    }

}
