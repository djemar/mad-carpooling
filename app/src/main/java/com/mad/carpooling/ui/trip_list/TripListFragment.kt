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

class TripListFragment : Fragment(R.layout.fragment_trip_list) {

    companion object {
        var idN: Int = 0
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rv = view.findViewById<RecyclerView>(R.id.triplist_rv)
        rv.layoutManager = LinearLayoutManager(context)
        //just an example, real trips needed
        val triplist =
            (1..10).map { Trip(idN++) }.toMutableList()
        //val triplist = listOf<Trip>() //to check emptyView
        val tripAdapter = TripAdapter(triplist)
        rv.adapter = tripAdapter

        val emptyView = view.findViewById<TextView>(R.id.no_trips_available)

        if (tripAdapter.itemCount == 0) //from getItemCount
            emptyView.isVisible = true

        val fab = view.findViewById<FloatingActionButton>(R.id.trip_add)
        fab.setOnClickListener {
            findNavController().navigate(R.id.action_nav_trip_list_to_nav_trip_edit) //a new one from scratch
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

class Trip(
    val id: Int = 1,
    val nickname: String = "Babayaga",
    val locationFrom: String = "From x",
    val locationTo: String = "To y",
    val duration: String = "hh:mm",
    val date: String = "dd-mm-yyyy",
    val time: String = "hh:mm",
    val seats: Int = 1,
    val price: Float = 1f,
    val chattiness: Boolean = false,
    val smoking: Boolean = false,
    val pets: Boolean = false,
    val music: Boolean = false,
    val description: String = "Description",
    val stops: HashMap<Int, String>? = null
) {

}

class TripAdapter(val triplist: List<Trip>) : RecyclerView.Adapter<TripAdapter.TripViewHolder>() {

    class TripViewHolder(v: View) : RecyclerView.ViewHolder(v) {

        val tripRL = v.findViewById<RelativeLayout>(R.id.trip_rl)
        val location = v.findViewById<TextView>(R.id.trip_from_to)
        val duration = v.findViewById<TextView>(R.id.trip_duration)
        val price = v.findViewById<TextView>(R.id.trip_price)
        val button = v.findViewById<ImageButton>(R.id.trip_edit)

        var navController: NavController? = null

        fun bind(trip: Trip) {

            location.text = "${trip.locationFrom} ${trip.locationTo}"
            duration.text = trip.duration
            price.text = trip.price.toString()

            tripRL.setOnClickListener {
                navController = Navigation.findNavController(tripRL)
                navController!!.navigate(R.id.action_nav_trip_list_to_nav_trip_details)
            }
            button.setOnClickListener {
                navController = Navigation.findNavController(button)
                //val bundle : Bundle
                navController!!.navigate(R.id.action_nav_trip_list_to_nav_trip_edit/*, bundle */) //modify an existing one
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

