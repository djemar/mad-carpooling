package com.mad.carpooling

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
        val stops: HashMap<Int, String>? = hashMapOf(
            0 to "Florence, 18/04/21, 3h",
            1 to "Siena, 18/04/21, 1h",
            2 to "Pisa, 18/04/21, 1h"
        )
    ) {}

    private val tripList = mutableListOf<Trip>(
        Trip(
            1,
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
            hashMapOf(
                0 to "Torino, 18/04/21, 10min",
                1 to "Rivoli, 18/04/21, 40min",
                2 to "Ivrea, 18/04/21, 10min"
            )
        ),
        Trip(
            2,
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
            hashMapOf(
                0 to "Calascibetta, 18/04/24, 3h",
                1 to "Messina, 18/04/24, 1h",
                2 to "Reggio, 18/04/24, 1h",
                3 to "Bari, 18/04/24, 1h",
            )
        )
    )

    fun getTrip(id: Int): Trip {
        return tripList[id]
    }

    fun getTripList(): MutableList<Trip> {
        return tripList
    }

    fun updateTrip(id: Int, trip: Trip) {
        tripList[id] = trip
    }

    fun addTrip(trip: Trip) {
        tripList.add(trip)
    }
}