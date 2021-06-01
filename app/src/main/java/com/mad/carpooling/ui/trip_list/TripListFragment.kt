package com.mad.carpooling.ui.trip_list

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
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
import com.google.firebase.Timestamp
import com.mad.carpooling.MainActivity
import com.mad.carpooling.R
import com.mad.carpooling.data.Trip
import com.mad.carpooling.ui.SharedViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class TripListFragment : Fragment(R.layout.fragment_trip_list) {
    private lateinit var rv: RecyclerView
    private var tripMap: HashMap<String, Trip>? = null

    // Use the 'by activityViewModels()' Kotlin property delegate
    // from the fragment-ktx artifact
    private val model: SharedViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val emptyView = view.findViewById<TextView>(R.id.no_trips_available)

        //val swipeContainer = view.findViewById<SwipeRefreshLayout>(R.id.swipeContainer)
        //swipeContainer.isEnabled = false

        model.getMyTrips().observe(viewLifecycleOwner, Observer { newTripsMap ->
            // Update the UI
            emptyView.isVisible = newTripsMap.isEmpty()
            updateTripList(newTripsMap, view)
        })

    }

    private fun updateTripList(tripsMap: HashMap<String, Trip>, view: View) {
        rv = view.findViewById<RecyclerView>(R.id.triplist_rv)
        rv.layoutManager = LinearLayoutManager(context)
        rv.isNestedScrollingEnabled = false; //prevent toolbar to expand on scroll

        val tripAdapter = TripAdapter(ArrayList((tripsMap.values)))
        rv.adapter = tripAdapter

        //TODO check on tripList size instead
        val emptyView = view.findViewById<TextView>(R.id.no_trips_available)
        if (tripAdapter.itemCount == 0) //from getItemCount
            emptyView.isVisible = true

        val fab = view.findViewById<FloatingActionButton>(R.id.fab_triplist)

        var navController: NavController?
        fab.setOnClickListener {
            val action = TripListFragmentDirections.actionNavTripListToNavTripEdit(
                "",
                isNew = true
            )
            navController = Navigation.findNavController(view)
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

    class TripAdapter(private val tripList: ArrayList<Trip>) :
        RecyclerView.Adapter<TripAdapter.TripViewHolder>() {

        class TripViewHolder(v: View) : RecyclerView.ViewHolder(v) {

            val tripRL: RelativeLayout = v.findViewById<RelativeLayout>(R.id.trip_rl)
            val btnEdit: ImageButton = v.findViewById<ImageButton>(R.id.trip_edit)
            private val ivCar = v.findViewById<ImageView>(R.id.trip_car)
            private val location = v.findViewById<TextView>(R.id.trip_from_to)
            private val timestamp = v.findViewById<TextView>(R.id.trip_timestamp)
            private val price = v.findViewById<TextView>(R.id.trip_price)

            private var navController: NavController? = null

            @SuppressLint("SetTextI18n")
            fun bind(trip: Trip) {
                if (!trip.visibility)
                    tripRL.alpha = 0.5f
                location.text = "${trip.departure} - ${trip.arrival}"
                timestamp.text = (SimpleDateFormat.getDateTimeInstance(
                    SimpleDateFormat.MEDIUM,
                    SimpleDateFormat.SHORT
                )).format(trip.timestamp.toDate())
                price.text = "Price: ${("%.2f".format(trip.price))} €"
                if (trip.imageCarURL != "") {
                    Glide.with(this.itemView).load(trip.imageCarURL).into(ivCar)
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
            holder.btnEdit.isVisible = trip.timestamp > Timestamp.now()

            holder.btnEdit.setOnClickListener {
                val action = TripListFragmentDirections.actionNavTripListToNavTripEdit(
                    trip.id,
                    isNew = false
                )
                Navigation.findNavController(holder.btnEdit)
                    .navigate(action) //modify an existing one

            }
        }

        override fun getItemCount(): Int {
            return tripList.size
        }

    }
}

