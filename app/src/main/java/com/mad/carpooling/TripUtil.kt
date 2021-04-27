package com.mad.carpooling

import android.content.Context
import com.google.gson.Gson




class TripUtil(

) {
    companion object {
        var idN: Int = 0
    }

    class Trip(
        val id: Int = 1,
        val nickname: String = "Babayaga",
        val departure: String = "From x",
        val arrival: String = "To y",
        val duration: String = "hh:mm",
        val depDate: String = "dd-mm-yyyy",
        val depTime: String = "hh:mm",
        val seats: Int = 1,
        val price: Float = 1f,
        val chattiness: Boolean = false,
        val smoking: Boolean = false,
        val pets: Boolean = false,
        val music: Boolean = false,
        val description: String = "Description",
        val stops: ArrayList<String>? = null
    ) {}

    private val tripList = arrayListOf<Trip>(
        Trip(
            0,
            "Babayaga",
            "Torino",
            "Ivrea",
            "01:00",
            "28-04-2021",
            "07:00",
            4,
            25f,
            false,
            false,
            true,
            false,
            "It will be a killer of a trip!",
            arrayListOf(
                "Torino, 18/04/21, 10min",
                "Rivoli, 18/04/21, 40min",
                "Ivrea, 18/04/21, 10min"
            )
        ),
        Trip(
            1,
            "MrWinston",
            "Calascibetta",
            "Bari",
            "06:00",
            "28-04-2024",
            "05:00",
            1,
            45f,
            true,
            false,
            false,
            true,
            "Calascibetta caput mundi.",
            arrayListOf(
                "Calascibetta, 18/04/24, 3h",
                "Messina, 18/04/24, 1h",
                "Reggio, 18/04/24, 1h",
                "Bari, 18/04/24, 1h",
            )
        )
    )

    fun getTrip(id: Int): Trip {
        return tripList[id]
    }

    fun getTripList(): ArrayList<Trip> {
        return tripList
    }

    fun updateTrip(id: Int, trip: Trip) {
        tripList[id] = trip
    }

    fun addTrip(trip: Trip) {
        tripList.add(trip)
    }

    fun getJsonTripList(){
        val gson = Gson()
        val jsonTripList = gson.toJson(tripList)
    }
}