package com.mad.carpooling.repository

import android.content.Context
import android.location.Address
import android.util.Log
import com.mad.carpooling.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.osmdroid.bonuspack.location.GeocoderNominatim
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.util.*
import java.util.stream.Collectors

class MapRepository {

    suspend fun getFromLocation(p: GeoPoint): Address = withContext(Dispatchers.IO) {
        val geocoder = GeocoderNominatim(
            Locale.getDefault(),
            BuildConfig.APPLICATION_ID
        )
        val address = async { geocoder.getFromLocation(p.latitude, p.longitude, 1) }
        try {
            val a = address.await()
            return@withContext a[0]
        } catch (e: Throwable) {
            Log.e("Marker Location", "Error ->" + e.message)
            return@withContext Address(Locale.getDefault())
        }
    }

    suspend fun getRoute(waypoints: ArrayList<Marker>, ctx: Context): Polyline =
        withContext(Dispatchers.IO) {
            val roadManager: RoadManager = OSRMRoadManager(ctx, BuildConfig.APPLICATION_ID)
            val gp = waypoints.stream().map(Marker::getPosition)
                .collect(Collectors.toList()) as ArrayList<GeoPoint>
            val road = async { roadManager.getRoad(gp) }
            val r = road.await()
            return@withContext RoadManager.buildRoadOverlay(r) as Polyline
        }


}