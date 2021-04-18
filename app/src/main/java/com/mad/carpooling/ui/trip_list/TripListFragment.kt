package com.mad.carpooling.ui.trip_list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mad.carpooling.R

class TripListFragment : Fragment(R.layout.fragment_trip_list) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rv = view.findViewById<RecyclerView>(R.id.triplist_rv)
        rv.layoutManager = LinearLayoutManager(context)
        //just an example, real trips needed
        val triplist = (1..20).map { Trip("From_to $it", "duration $it", "price $it") }.toMutableList()
        val tripAdapter = TripAdapter(triplist)
        rv.adapter = tripAdapter

        val fab = view.findViewById<FloatingActionButton>(R.id.add)
        fab.setOnClickListener {
            findNavController().navigate(R.id.action_nav_trip_list_to_nav_trip_edit) //a new one from scratch
        }
    }
}

class Trip(val location: String, val duration: String, val price: String){

}

class TripAdapter(val triplist: List<Trip>): RecyclerView.Adapter<TripAdapter.TripViewHolder>(){

    class TripViewHolder(v: View): RecyclerView.ViewHolder(v){

        val tripLayout = v.findViewById<RelativeLayout>(R.id.trip)
        val location = v.findViewById<TextView>(R.id.from_to)
        val duration = v.findViewById<TextView>(R.id.duration)
        val price = v.findViewById<TextView>(R.id.price)
        val button = v.findViewById<Button>(R.id.edit)
        var navController: NavController? = null

        fun bind(trip: Trip){

            location.text = trip.location
            duration.text = trip.duration
            price.text = trip.price

            tripLayout.setOnClickListener{
                navController = Navigation.findNavController(tripLayout)
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
        val layout = LayoutInflater.from(parent.context).inflate(R.layout.triplist_layout, parent, false)
        return TripViewHolder(layout)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        holder.bind(triplist[position])
    }

    //could be used for task 5c (?)
    override fun getItemCount(): Int {
        return triplist.size;
    }

}

