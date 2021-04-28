package com.mad.carpooling

import com.google.gson.Gson


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
                ),
                null
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