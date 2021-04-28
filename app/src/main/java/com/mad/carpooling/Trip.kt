package com.mad.carpooling

import com.google.gson.Gson


class Trip(
    val id: Int = 1,
    val nickname: String = "Babayaga",
    val departure: String = "",
    val arrival: String = "",
    val duration: String = "",
    val depDate: String = "dd/mm/yyyy",
    val depTime: String = "hh:mm",
    val seats: Int = 1,
    val price: Float = 1f,
    val chattiness: Boolean = false,
    val smoking: Boolean = false,
    val pets: Boolean = false,
    val music: Boolean = false,
    val description: String = "",
    val stops: ArrayList<String>? = null,
    val carPhotoPath: String? = null
) {

/*    fun getTrip(id: Int): Trip {
        return defaultTripList[id]
    }*/

    fun getDefaultTripList(): ArrayList<Trip> {
        val defaultTripList = arrayListOf<Trip>(
            Trip(
                0,
                "Babayaga",
                "Torino",
                "Ivrea",
                "1 hour",
                "28/04/2021",
                "07:00",
                4,
                25f,
                false,
                false,
                true,
                false,
                "It will be a killer of a trip!",
                arrayListOf(
                    "Chivasso, 28/04/21, 20min",
                    "Caluso, 28/04/21, 20min"
                ),
                null
            ),
            Trip(
                1,
                "MrWinston",
                "Calascibetta",
                "Taranto",
                "7 hours",
                "30/04/2021",
                "05:00",
                1,
                45f,
                true,
                false,
                false,
                true,
                "Calascibetta caput mundi.",
                arrayListOf(
                    "Catania, 30/04/21, 1h",
                    "Messina, 30/04/21, 1h 30min",
                    "Cosenza, 30/04/21, 3h"
                ),
                null
            )
        )
        return defaultTripList
    }

/*    fun updateTrip(id: Int, trip: Trip) {
        defaultTripList[id] = trip
    }

    fun addTrip(trip: Trip) {
        defaultTripList.add(trip)
    }

    fun getJsonTripList() {
        val gson = Gson()
        val jsonTripList = gson.toJson(defaultTripList)
    }*/
}