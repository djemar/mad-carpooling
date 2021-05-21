package com.mad.carpooling.ui.maps

import android.Manifest
import android.location.Geocoder
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.mad.carpooling.R
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.dialogs.SettingsDialog
import org.osmdroid.config.Configuration.getInstance
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.OverlayItem
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.util.*
import kotlin.collections.ArrayList


class MapFragment : Fragment(R.layout.fragment_map), EasyPermissions.PermissionCallbacks {
    private lateinit var map: MapView;

    companion object {
        const val PERMISSION_LOCATION_REQUEST_CODE = 1
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        getInstance().load(
            requireContext(),
            PreferenceManager.getDefaultSharedPreferences(requireContext())
        );
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        map = view.findViewById<MapView>(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK);
        if (!hasLocationPermission()) {
            requestLocationPermission()
        }
        val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), map)
        locationOverlay.enableMyLocation()
        val mapController = map.controller
        mapController.setZoom(15.0)
        locationOverlay.enableFollowLocation()
        map.overlays.add(locationOverlay)
        val rotationGestureOverlay = RotationGestureOverlay(map);
        rotationGestureOverlay.isEnabled
        map.setMultiTouchControls(true);
        map.overlays.add(rotationGestureOverlay);

        //your items
        val items = ArrayList<OverlayItem>()
        items.add(
            OverlayItem(
                "Marcu", "Pacco", GeoPoint(
                    45.0580, 7.6482
                )
            )
        )

        var markerStart: Marker? = null

        val mapEventsReceiver: MapEventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                //do whatever you need here
                return false
            }

            override fun longPressHelper(p: GeoPoint): Boolean {
                //do whatever you need here
                val geocoder = Geocoder(requireContext(), Locale.getDefault())
                val address = geocoder.getFromLocation(p.latitude, p.longitude, 1)
                if (markerStart != null) {
                    (markerStart as Marker).closeInfoWindow()
                    map.overlays.remove(markerStart)
                }
                markerStart = Marker(map)
                markerStart!!.position = p
                markerStart!!.title = address[0].getAddressLine(0)
                map.overlays.add(markerStart)
                map.invalidate()

                return true
            }
        }
        val evOverlay = MapEventsOverlay(mapEventsReceiver)
        map.getOverlays().add(evOverlay)

        super.onViewCreated(view, savedInstanceState)
    }

    override fun onResume() {
        super.onResume();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        map.onResume(); //needed for compass, my location overlays, v6.0.0 and up
    }

    override fun onPause() {
        super.onPause();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        map.onPause();  //needed for compass, my location overlays, v6.0.0 and up
    }

    private fun hasLocationPermission() =
        EasyPermissions.hasPermissions(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        )

    private fun requestLocationPermission() {
        EasyPermissions.requestPermissions(
            this,
            "This application cannot work without Location Permission.",
            PERMISSION_LOCATION_REQUEST_CODE,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            SettingsDialog.Builder(requireActivity()).build().show()
        } else {
            requestLocationPermission()
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        Toast.makeText(
            requireContext(),
            "Permission Granted!",
            Toast.LENGTH_SHORT
        ).show()

    }

}