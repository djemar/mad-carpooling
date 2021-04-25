package com.mad.carpooling.ui.trip_list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mad.carpooling.R
import com.mad.carpooling.TripUtil

class TripListFragment : Fragment(R.layout.fragment_trip_list) {


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rv = view.findViewById<RecyclerView>(R.id.triplist_rv)
        rv.layoutManager = LinearLayoutManager(context)
        //just an example, real trips needed
        val tripUtil = TripUtil()
        val triplist = tripUtil.getTripList()
        //val triplist = listOf<Trip>() //to check emptyView
        val tripAdapter = TripAdapter(triplist.toList())
        rv.adapter = tripAdapter

        val emptyView = view.findViewById<TextView>(R.id.no_trips_available)

        if (tripAdapter.itemCount == 0) //from getItemCount
            emptyView.isVisible = true

        val fab = view.findViewById<FloatingActionButton>(R.id.trip_add)
        var navController: NavController? = null
        fab.setOnClickListener {
            val action = TripListFragmentDirections.actionNavTripListToNavTripEdit(
                isNew = true
            )
            navController = Navigation.findNavController(fab)
            navController!!.navigate(action) //a new one from scratch
        }
        rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0 && fab.getVisibility() === View.VISIBLE) {
                    fab.hide()
                } else if (dy < 0 && fab.getVisibility() !== View.VISIBLE) {
                    fab.show()
                }
            }
        })
    }
}


class TripAdapter(val triplist: List<TripUtil.Trip>) : RecyclerView.Adapter<TripAdapter.TripViewHolder>() {

    class TripViewHolder(v: View) : RecyclerView.ViewHolder(v) {

        val tripRL = v.findViewById<RelativeLayout>(R.id.trip_rl)
        val location = v.findViewById<TextView>(R.id.trip_from_to)
        val duration = v.findViewById<TextView>(R.id.trip_duration)
        val price = v.findViewById<TextView>(R.id.trip_price)
        val button = v.findViewById<ImageButton>(R.id.trip_edit)

        var navController: NavController? = null

        fun bind(trip: TripUtil.Trip) {

            location.text = "${trip.departure} ${trip.arrival}"
            duration.text = trip.duration
            price.text = trip.price.toString()

            val bundle = Bundle()
            bundle.putSerializable("stops", trip.stops)

            tripRL.setOnClickListener {
                val action = TripListFragmentDirections.actionNavTripListToNavTripDetails(
                    trip.id,
                    trip.departure,
                    trip.arrival,
                    trip.duration,
                    trip.price,
                    trip.seats,
                    trip.depDate,
                    trip.depTime,
                    trip.chattiness,
                    trip.smoking,
                    trip.pets,
                    trip.music,
                    trip.description,
                    bundle
                )
                navController = Navigation.findNavController(tripRL)
                navController!!.navigate(action)
            }
            button.setOnClickListener {
                val action = TripListFragmentDirections.actionNavTripListToNavTripEdit(
                    trip.id,
                    trip.departure,
                    trip.arrival,
                    trip.duration,
                    trip.price,
                    trip.seats,
                    trip.depDate,
                    trip.depTime,
                    trip.chattiness,
                    trip.smoking,
                    trip.pets,
                    trip.music,
                    trip.description,
                    bundle,
                    false
                )
                navController = Navigation.findNavController(button)
                navController!!.navigate(action) //modify an existing one
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val layout =
            LayoutInflater.from(parent.context).inflate(R.layout.triplist_layout, parent, false)
        return TripViewHolder(layout)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        holder.bind(triplist[position])
    }

    override fun getItemCount(): Int {
        return triplist.size;
    }

}

