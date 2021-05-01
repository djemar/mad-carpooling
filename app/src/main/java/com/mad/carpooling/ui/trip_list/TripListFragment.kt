package com.mad.carpooling.ui.trip_list

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mad.carpooling.R
import com.mad.carpooling.Trip
import com.mad.carpooling.ui.SharedViewModel
import org.json.JSONObject

private var currentUser: String? = null

class TripListFragment : Fragment(R.layout.fragment_trip_list) {
    private lateinit var rv: RecyclerView
    private var tripMap: HashMap<String, Trip>? = null

    // Use the 'by activityViewModels()' Kotlin property delegate
    // from the fragment-ktx artifact
    private val model: SharedViewModel by activityViewModels()


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        model.getTrips().observe(viewLifecycleOwner, Observer { newTripsMap ->
            // Update the UI
            updateTripList(newTripsMap, view)
        })

        // initTripList()

    }

//    private fun initTripList() {
//        currentUser = getCurrentUser()
//        tripMap = model.getTrips().value
//    }

    private fun updateTripList(tripsMap: HashMap<String, Trip>, view: View) {
        currentUser = getCurrentUser()
        rv = view.findViewById<RecyclerView>(R.id.triplist_rv)
        rv.layoutManager = LinearLayoutManager(context)
        //just an example, real trips needed

        val tripAdapter = TripAdapter(ArrayList((tripsMap.values)))
        rv.adapter = tripAdapter

        //TODO check on tripList size instead
        val emptyView = view.findViewById<TextView>(R.id.no_trips_available)
        if (tripAdapter.itemCount == 0) //from getItemCount
            emptyView.isVisible = true

        val fab = view.findViewById<FloatingActionButton>(R.id.trip_add)
        var navController: NavController?
        fab.setOnClickListener {
            val action = TripListFragmentDirections.actionNavTripListToNavTripEdit(
                "",
                isNew = true
            )
            navController = Navigation.findNavController(fab)
            navController!!.navigate(action) //a new one from scratch
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

    private fun getCurrentUser(): String? {
        val sharedPref =
            context?.getSharedPreferences("profile_pref.group05.lab1", Context.MODE_PRIVATE)
                ?: return null
        val jsonString = sharedPref.getString(getString(R.string.saved_profile_data), null)
        return if (jsonString != null) {
            val jsonObject = JSONObject(jsonString)
            jsonObject.getString(
                "json_nickname.group05.lab1"
            )
        } else "babayaga" //just for testing purposes
    }


    class TripAdapter(private val tripList: ArrayList<Trip>) :
        RecyclerView.Adapter<TripAdapter.TripViewHolder>() {

        class TripViewHolder(v: View) : RecyclerView.ViewHolder(v) {

            val tripRL = v.findViewById<RelativeLayout>(R.id.trip_rl)
            val btnEdit = v.findViewById<ImageButton>(R.id.trip_edit)
            private val ivCar = v.findViewById<ImageView>(R.id.trip_car)
            private val location = v.findViewById<TextView>(R.id.trip_from_to)
            private val duration = v.findViewById<TextView>(R.id.trip_duration)
            private val price = v.findViewById<TextView>(R.id.trip_price)

            private var navController: NavController? = null

            @SuppressLint("SetTextI18n")
            fun bind(trip: Trip) {
                location.text = "${trip.departure} - ${trip.arrival}"
                duration.text = "Duration: ${trip.duration}"
                price.text = "Price: ${("%.2f".format(trip.price))} €"
                if (trip.imageCarURL != "") {
                    val storageRef = Firebase.storage.reference.child("images_car/${trip.imageCarURL}")
                    Glide.with(this.itemView).load(storageRef).into(ivCar)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
            val layout =
                LayoutInflater.from(parent.context).inflate(R.layout.triplist_layout, parent, false)
            return TripViewHolder(layout)
        }

        override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
            val trip = tripList[position]
            trip.let { holder.bind(it) }
            holder.tripRL.setOnClickListener {
                val action = TripListFragmentDirections.actionNavTripListToNavTripDetails(
                    trip.id,
                )
                Navigation.findNavController(holder.tripRL).navigate(action)
            }
            if (trip.owner!!.id == currentUser) {
                holder.btnEdit.setOnClickListener {
                    val action = TripListFragmentDirections.actionNavTripListToNavTripEdit(
                        trip.id,
                        isNew = false
                    )
                    Navigation.findNavController(holder.btnEdit)
                        .navigate(action) //modify an existing one
                }
            } else holder.btnEdit.visibility = View.GONE
        }

        override fun getItemCount(): Int {
            return tripList.size
        }

    }
}

