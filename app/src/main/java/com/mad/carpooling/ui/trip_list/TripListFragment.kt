package com.mad.carpooling.ui.trip_list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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
        val triplist = (1..5).map { Trip("Trip$it") }.toMutableList()
        val tripAdapter = TripAdapter(triplist)
        rv.adapter = tripAdapter

        val fab = view.findViewById<FloatingActionButton>(R.id.add)
        fab.setOnClickListener {
            findNavController().navigate(R.id.action_nav_trip_list_to_nav_trip_edit) //a new one from scratch
        }
    }
}

class Trip(val name: String){

}

class TripAdapter(val triplist: List<Trip>): RecyclerView.Adapter<TripAdapter.TripViewHolder>(){

    class TripViewHolder(v: View): RecyclerView.ViewHolder(v){

        val tripId = v.findViewById<TextView>(R.id.trip)
        val button = v.findViewById<Button>(R.id.edit)
        var navController: NavController? = null

        fun bind(trip: Trip){
            tripId.text = trip.name
            tripId.setOnClickListener{
                navController = Navigation.findNavController(tripId)
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

