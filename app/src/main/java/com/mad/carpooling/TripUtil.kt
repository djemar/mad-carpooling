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
        val stops: HashMap<Int, String>? = hashMapOf(0 to "Florence, 18/04/21, 3h", 1 to "Siena, 18/04/21, 1h", 2 to "Pisa, 18/04/21, 1h")
    ) {}

    private val triplist =
        (1..10).map { Trip(idN++) }.toMutableList()

    fun getTrip(id: Int): Trip {
        return triplist[id]
    }

    fun getTripList(): MutableList<Trip> {
        return triplist
    }

    fun updateTrip(id: Int, trip: Trip) {
        triplist[id] = trip
    }

    fun addTrip(trip: Trip) {
        triplist.add(trip)
    }
}