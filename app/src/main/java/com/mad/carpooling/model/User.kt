package com.mad.carpooling.model

class User(
    var uid: String = "uid",
    var fullname: String = "Fullname",
    var nickname: String = "Nickname",
    var email: String = "email@address.com",
    var location: String = "City",
    var imageUserRef: String? = null,
    var favTrips: ArrayList<String>? = java.util.ArrayList<String>()
)