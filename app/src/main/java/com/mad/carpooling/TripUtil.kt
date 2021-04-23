package com.mad.carpooling

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
    companion object {
        var idN: Int = 0
    }

    private val triplist =
        (1..10).map { Trip(idN++) }.toMutableList()

    fun getTrip(id: Int): Trip {
        return triplist[id]
    }

    fun getTripList(): MutableList<Trip> {
        return triplist
    }

    fun updateTrip(id: Int, trip : Trip){
        triplist[id] = trip
    }

    fun addTrip(trip: Trip){
        triplist.add(trip)
    }
}