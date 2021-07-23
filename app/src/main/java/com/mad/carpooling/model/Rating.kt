package com.mad.carpooling.model

class Rating(
    var driverRatings: Map<String, ArrayList<Any>> = mapOf(),
    var passengerRatings: Map<String, ArrayList<Any>> = mapOf()
)