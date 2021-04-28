package com.mad.carpooling.ui.trip_list

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mad.carpooling.R
import com.mad.carpooling.Trip
import org.json.JSONObject

private var currentUser: String? = null

class TripListFragment : Fragment(R.layout.fragment_trip_list) {
    private var tripList: ArrayList<Trip>? = null
    private lateinit var ivCar: ImageView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initTripList()

        val rv = view.findViewById<RecyclerView>(R.id.triplist_rv)
        rv.layoutManager = LinearLayoutManager(context)
        //just an example, real trips needed

        val tripAdapter = TripAdapter(tripList!!)
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

    private fun initTripList() {
        currentUser = getCurrentUser()
        tripList = getSavedTripList()

        if (tripList == null) {
            tripList = Trip().getDefaultTripList() // for testing purposes
            val jsonObj = JSONObject()

            val gson = Gson()
            val jsonTripList = gson.toJson(tripList)
            jsonObj.put("json_tripList.group05.lab2", jsonTripList)

            val sharedPref =
                context?.getSharedPreferences("trip_pref.group05.lab2", Context.MODE_PRIVATE)
                    ?: return
            with(sharedPref.edit()) {
                putString(getString(R.string.saved_profile_data), jsonObj.toString())
                apply()
            }
        }
    }

    private fun getSavedTripList(): ArrayList<Trip>? {
        var gson = Gson()
        val sharedPref =
            context?.getSharedPreferences("trip_pref.group05.lab2", Context.MODE_PRIVATE)
                ?: return null
        val jsonString = sharedPref.getString(getString(R.string.saved_profile_data), null)
        if (jsonString != null) {
            val jsonObject = JSONObject(jsonString)
            var jsonTripList = jsonObject.getString(
                "json_tripList.group05.lab2"
            )
            val myType = object : TypeToken<ArrayList<Trip>>() {}.type
            return gson.fromJson(jsonTripList, myType)
        } else return null
    }


    private fun getCurrentUser(): String? {
        val sharedPref =
            context?.getSharedPreferences("profile_pref.group05.lab1", Context.MODE_PRIVATE)
                ?: return null
        val jsonString = sharedPref.getString(getString(R.string.saved_profile_data), null)
        if (jsonString != null) {
            val jsonObject = JSONObject(jsonString)
            return jsonObject.getString(
                "json_nickname.group05.lab1"
            )
        } else return "Babayaga"; //just for testing purposes
    }


    class TripAdapter(val tripList: ArrayList<Trip>) :
        RecyclerView.Adapter<TripAdapter.TripViewHolder>() {

        class TripViewHolder(v: View) : RecyclerView.ViewHolder(v) {

            val ivCar = v.findViewById<ImageView>(R.id.trip_car)
            val tripRL = v.findViewById<RelativeLayout>(R.id.trip_rl)
            val location = v.findViewById<TextView>(R.id.trip_from_to)
            val duration = v.findViewById<TextView>(R.id.trip_duration)
            val price = v.findViewById<TextView>(R.id.trip_price)
            val btnEdit = v.findViewById<ImageButton>(R.id.trip_edit)

            var navController: NavController? = null

            fun bind(trip: Trip) {

                location.text = "${trip.departure} ${trip.arrival}"
                duration.text = trip.duration
                price.text = trip.price.toString()
                if(trip.carPhotoPath != null){
                    BitmapFactory.decodeFile(trip.carPhotoPath)?.also { bitmap ->
                        ivCar.setImageBitmap(bitmap)
                    }
                }

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
                if (trip.nickname == currentUser) {
                    btnEdit.setOnClickListener {
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
                        navController = Navigation.findNavController(btnEdit)
                        navController!!.navigate(action) //modify an existing one
                    }
                } else btnEdit.visibility = View.GONE
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
            val layout =
                LayoutInflater.from(parent.context).inflate(R.layout.triplist_layout, parent, false)
            return TripViewHolder(layout)
        }

        override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
            holder.bind(tripList!!.get(position))
        }

        override fun getItemCount(): Int {
            return tripList!!.size;
        }

    }
}

