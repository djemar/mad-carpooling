package com.mad.carpooling.ui.maps

import android.Manifest
import android.content.Context
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import com.mad.carpooling.R
import com.mad.carpooling.data.Trip
import com.mad.carpooling.ui.SharedViewModel
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.dialogs.SettingsDialog
import org.osmdroid.config.Configuration.getInstance
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.FolderOverlay
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.util.stream.Collectors


class MapFragment : Fragment(R.layout.fragment_map), EasyPermissions.PermissionCallbacks {
    private lateinit var map: MapView;
    private lateinit var mapViewModel: MapViewModel
    private lateinit var viewModelFactory: MapViewModelFactory
    private val model: SharedViewModel by activityViewModels()
    private lateinit var trip: Trip

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
            context?.getSharedPreferences("mad.carpooling.map", Context.MODE_PRIVATE)
        );
        viewModelFactory = MapViewModelFactory(MapRepository())
        mapViewModel = ViewModelProvider(this, viewModelFactory)
            .get(MapViewModel::class.java)

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        postponeEnterTransition()
        val args: MapFragmentArgs by navArgs()
        map = view.findViewById<MapView>(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK);

        val previousFragment = findNavController().previousBackStackEntry?.destination?.id
        if (previousFragment == R.id.nav_trip_edit) {
            trip = model.getTrips().value!!.get(args.tid)!!
            initFromEdit()
        } else {
            trip = model.getTrips().value!!.get(args.tid)!!
            initFromDetails()
        }
        //TODO new trip

        super.onViewCreated(view, savedInstanceState)
    }

    private fun initFromDetails() {
        var clickedMarker : Marker = Marker(map)
        val waypoints = ArrayList<Marker>()
        val stopsMarkers = FolderOverlay()
        val rotationGestureOverlay = RotationGestureOverlay(map);
        map.setMultiTouchControls(true);
        map.overlays.add(rotationGestureOverlay);

        mapViewModel.address.observe(viewLifecycleOwner, { address ->
            if (address != null) {
                val strAddress = arrayListOf(
                    address.thoroughfare,
                    address.subThoroughfare,
                    address.postalCode,
                    address.locality,
                    address.countryName
                ).filterNotNull()
                clickedMarker.title = address.locality
                clickedMarker.snippet = strAddress.joinToString(", ")
                clickedMarker.showInfoWindow()
            } else {
                Snackbar.make(
                    map,
                    "There was an error: check your internet connection",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        })

        trip.geopoints.stream().forEach { gp ->
            run {
                val marker = Marker(map)
                marker.position = GeoPoint(gp.latitude, gp.longitude)
                marker.setOnMarkerClickListener { markerClick, mapView ->
                    clickedMarker = markerClick
                    mapViewModel.getFromLocation(markerClick.position, requireContext())
                    true
                }
                waypoints.add(marker)
                stopsMarkers.add(marker)
            }
        }

        MapUtils.redrawMarkers(waypoints, map, requireContext())

        map.post {
            run() {
                val box = MapUtils.computeArea(
                    trip.geopoints.stream().map { gp -> GeoPoint(gp.latitude, gp.longitude) }
                        .collect(Collectors.toList()) as ArrayList<GeoPoint>
                )
                map.zoomToBoundingBox(box, false, 110);
                map.invalidate()
            }
        };

        var routeOverlay: Polyline
        mapViewModel.route.observe(viewLifecycleOwner, { newRouteOverlay ->
            if (newRouteOverlay != null) {
                routeOverlay = newRouteOverlay
                routeOverlay.outlinePaint.strokeWidth = 10f
                routeOverlay.outlinePaint.style = Paint.Style.FILL_AND_STROKE
                routeOverlay.outlinePaint.strokeCap = Paint.Cap.ROUND
                routeOverlay.outlinePaint.strokeJoin = Paint.Join.ROUND
                map.overlays.add(routeOverlay)
                map.overlays.add(stopsMarkers)
                map.invalidate()
                startPostponedEnterTransition()
            }
        })
        mapViewModel.getRoute(waypoints, requireContext())
    }

    private fun initFromEdit() {
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
        map.setMultiTouchControls(true);
        map.overlays.add(rotationGestureOverlay);
        val waypoints = arrayListOf<Marker>()
        var routeOverlay: Polyline = Polyline()

        mapViewModel.address.observe(viewLifecycleOwner, { address ->
            if (address != null) {
                val marker = Marker(map)
                val strAddress = arrayListOf(
                    address.thoroughfare,
                    address.subThoroughfare,
                    address.postalCode,
                    address.locality,
                    address.countryName
                ).filterNotNull()
                marker.title = "Departure"
                marker.snippet = strAddress.joinToString(", ")
                marker.position = GeoPoint(address.latitude, address.longitude)
                marker.showInfoWindow()
                waypoints.add(marker)
                marker.icon =
                    MapUtils.getNumMarker(waypoints.size.toString(), requireContext())
                marker.id = waypoints.size.toString()
                marker.isDraggable = true
                marker.setOnMarkerClickListener { clickedMarker, mapView ->
                    clickedMarker.remove(mapView)
                    waypoints.remove(clickedMarker)
                    MapUtils.redrawMarkers(waypoints, mapView, requireContext())
                    true
                }
                map.overlays.add(marker)
                map.invalidate()
            } else {
                Snackbar.make(
                    map,
                    "There was an error: check your internet connection",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        })

        mapViewModel.route.observe(viewLifecycleOwner, { newRouteOverlay ->
            if (newRouteOverlay != null) {
                routeOverlay = newRouteOverlay
                map.overlays.add(routeOverlay)
                map.invalidate()
            }
        })

        val mapEventsReceiver: MapEventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                //do whatever you need here
                map.overlays.remove(routeOverlay)
                mapViewModel.getRoute(waypoints, requireContext())
                return false
            }

            override fun longPressHelper(p: GeoPoint): Boolean {
                //do whatever you need here
                mapViewModel.getFromLocation(p, requireContext())
                return true
            }
        }
        val evOverlay = MapEventsOverlay(mapEventsReceiver)
        map.overlays.add(evOverlay)
    }


    override fun onResume() {
        super.onResume();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        getInstance().load(
            requireContext(),
            context?.getSharedPreferences("mad.carpooling.map", Context.MODE_PRIVATE)
        );
        map.onResume(); //needed for compass, my location overlays, v6.0.0 and up
    }

    override fun onPause() {
        super.onPause();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        getInstance().save(
            requireContext(),
            context?.getSharedPreferences("mad.carpooling.map", Context.MODE_PRIVATE)
        );
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