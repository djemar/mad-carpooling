package com.mad.carpooling.ui.maps

import android.content.Context
import android.location.Address
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mad.carpooling.data.Trip
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.util.*

class MapViewModel(
    private val mapRepository: MapRepository
) : ViewModel() {

    val address = MutableLiveData<Address?>()
    val route = MutableLiveData<Polyline?>()

    fun getFromLocation(p: GeoPoint) {
        viewModelScope.launch {
            val result = try {
                mapRepository.getFromLocation(p)
            } catch(e: Exception) {
                Log.e("getFromLocation -> ", e.message.toString())
            }
            when (result) {
                is Address -> address.postValue(result)
                else -> address.postValue(null) // Show error in UI
            }
        }
    }

    fun getRoute(waypoints: ArrayList<Marker>, ctx: Context) {
        viewModelScope.launch {
            val result = try {
                mapRepository.getRoute(waypoints, ctx)
            } catch(e: Exception) {
                Log.e("getRoute -> ", e.message.toString())
            }
            when (result) {
                is Polyline -> route.postValue(result)
                else -> route.postValue(null) // Show error in UI
            }
        }
    }

}