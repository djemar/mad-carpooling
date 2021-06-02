package com.mad.carpooling.viewmodel

import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.location.Address
import android.net.ConnectivityManager
import android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mad.carpooling.model.Trip
import com.mad.carpooling.repository.MapRepository
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.util.*

class MapViewModel(
    private val mapRepository: MapRepository
) : ViewModel() {

    private lateinit var trip: Trip
    val address = MutableLiveData<Address?>()
    val route = MutableLiveData<Polyline?>()

    fun getFromLocation(p: GeoPoint, context: Context) {

        val cm = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = cm.getNetworkCapabilities(cm.activeNetwork)
        val isNetworkAvailable = capabilities?.hasCapability(NET_CAPABILITY_INTERNET) == true
        if (isNetworkAvailable) {
            viewModelScope.launch {
                val result = try {
                    mapRepository.getFromLocation(p)
                } catch (e: Exception) {
                    Log.e("getFromLocation -> ", e.message.toString())
                }
                when (result) {
                    is Address -> address.postValue(result)
                    else -> address.postValue(null) // Show error in UI
                }
            }
        } else {
            address.postValue(null)
        }
    }

    fun getRoute(waypoints: ArrayList<Marker>, ctx: Context) {
        if(waypoints.size > 1)
        viewModelScope.launch {
            val result = try {
                mapRepository.getRoute(waypoints, ctx)
            } catch (e: Exception) {
                Log.e("getRoute -> ", e.message.toString())
            }
            when (result) {
                is Polyline -> route.postValue(result)
                else -> route.postValue(null) // Show error in UI
            }
        }
    }

    fun setTrip(trip: Trip) {
        this.trip = trip
    }

    fun getTrip(): Trip {
        return trip
    }

}