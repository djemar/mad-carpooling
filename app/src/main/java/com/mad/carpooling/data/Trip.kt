package com.mad.carpooling.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference


data class Trip(
    var id: String = "id",
    var owner: DocumentReference? = null,
    var departure: String = "",
    var arrival: String = "",
    var duration: String = "",
    var timestamp: Timestamp = Timestamp.now(),
    var seats: Int = 1,
    var price: Float = 1f,
    var chattiness: Boolean = false,
    var smoking: Boolean = false,
    var pets: Boolean = false,
    var music: Boolean = false,
    var description: String = "",
    var stops: ArrayList<String>? = java.util.ArrayList<String>(),
    var imageCarURL: String? = null, //TODO change also in DB to imageCarRef
    var interestedPeople: ArrayList<String>? = java.util.ArrayList<String>()
) {
}