package com.mad.carpooling.ui.maps

import android.Manifest
import android.content.Context
import android.graphics.Paint
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.onNavDestinationSelected
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.mad.carpooling.MainActivity
import com.mad.carpooling.R
import com.mad.carpooling.data.Trip
import com.mad.carpooling.ui.SharedViewModel
import com.mad.carpooling.ui.trip_edit.TripEditViewModel
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.dialogs.SettingsDialog
import org.osmdroid.config.Configuration.getInstance
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.FolderOverlay
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Marker.OnMarkerDragListener
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.util.stream.Collectors


class MapFragment : Fragment(R.layout.fragment_map), EasyPermissions.PermissionCallbacks {
    private lateinit var map: MapView;
    private lateinit var btnDeleteMarker: MaterialButton;
    private lateinit var mapViewModel: MapViewModel
    private lateinit var viewModelFactory: MapViewModelFactory
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private val tripEditViewModel: TripEditViewModel by activityViewModels()
    private lateinit var trip: Trip
    private lateinit var optionsMenu: Menu
    private val waypoints = ArrayList<Marker>()

    companion object {
        const val PERMISSION_LOCATION_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
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
        btnDeleteMarker = view.findViewById(R.id.btn_delete_marker)
        val args: MapFragmentArgs by navArgs()
        map = view.findViewById<MapView>(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

        if (savedInstanceState == null) {
            val previousFragment = findNavController().previousBackStackEntry?.destination?.id
            if (previousFragment == R.id.nav_trip_edit) {
                if (args.tid == null) {
                    trip = tripEditViewModel.getTrip()
                    if (trip.stops?.size!! > 0) initFromEdit()
                    else initFromNew()
                } else {
                    trip = tripEditViewModel.getTrip()
                    initFromEdit()
                }
            } else {
                trip = sharedViewModel.getTrips().value!![args.tid]!!
                setHasOptionsMenu(false)
                initFromDetails()
            }
        } else {
            trip = tripEditViewModel.getTrip()
            initFromEdit()
        }

        map.invalidate();
        super.onViewCreated(view, savedInstanceState)
    }

    private fun initFromDetails() {
        val selectedMarker = MutableLiveData<Marker?>(null)
        val waypoints = ArrayList<Marker>()
        val stopsMarkers = FolderOverlay()
        val rotationGestureOverlay = RotationGestureOverlay(map);
        map.setMultiTouchControls(true);
        map.overlays.add(rotationGestureOverlay);

        selectedMarker.observe(viewLifecycleOwner, { marker ->
            btnDeleteMarker.isVisible = marker != null
            if (marker != null) {
                map.controller.animateTo(marker.position)
            }
        })

        mapViewModel.address.observe(viewLifecycleOwner, { address ->
            if (address != null) {
                val strAddress = arrayListOf(
                    address.thoroughfare,
                    address.subThoroughfare,
                    address.postalCode,
                    address.locality,
                    address.countryName
                ).filterNotNull()
                selectedMarker.value?.title = address.locality
                selectedMarker.value?.snippet = strAddress.joinToString(", ")
                selectedMarker.value?.showInfoWindow()
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
                    selectedMarker.postValue(markerClick)
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
                    trip.geopoints.map { gp -> GeoPoint(gp.latitude, gp.longitude) }.toList() as ArrayList<GeoPoint>
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
            }
        })
        mapViewModel.getRoute(waypoints, requireContext())
    }

    private fun initFromEdit() {
        val selectedMarker = MutableLiveData<Marker?>(null)
        var routeOverlay: Polyline = Polyline()
        //val waypoints = ArrayList<Marker>()
        val stopsMarkers = FolderOverlay()
        val rotationGestureOverlay = RotationGestureOverlay(map);
        map.setMultiTouchControls(true);
        map.overlays.add(rotationGestureOverlay);

        selectedMarker.observe(viewLifecycleOwner, { marker ->
            btnDeleteMarker.isVisible = marker != null
            if (marker != null) {
                map.controller.animateTo(marker.position)
            }
        })

        btnDeleteMarker.setOnClickListener {
            if (selectedMarker.value != null) {
                selectedMarker.value!!.closeInfoWindow()
                stopsMarkers.remove(selectedMarker.value)
                waypoints.remove(selectedMarker.value)
                selectedMarker.postValue(null)
                MapUtils.redrawMarkers(waypoints, map, requireContext())
                mapViewModel.getRoute(waypoints, requireContext())
            }
        }

        mapViewModel.address.observe(viewLifecycleOwner, { address ->
            if (address != null) {
                val strAddress = arrayListOf(
                    address.thoroughfare,
                    address.subThoroughfare,
                    address.postalCode,
                    address.locality,
                    address.countryName
                ).filterNotNull()
                if (selectedMarker.value != null) {
                    selectedMarker.value!!.title = address.locality
                    selectedMarker.value!!.snippet = strAddress.joinToString(", ")
                    selectedMarker.value!!.showInfoWindow()
                } else {
                    val marker = Marker(map)
                    marker.title = address.locality
                    marker.snippet = strAddress.joinToString(", ")
                    marker.position = GeoPoint(address.latitude, address.longitude)
                    marker.showInfoWindow()
                    if (!waypoints.stream()
                            .filter { m -> m.position == marker.position } // avoid marker recretion after rotation
                            .findFirst().isPresent
                    ) {
                        waypoints.add(marker)
                        marker.icon =
                            MapUtils.getNumMarker(waypoints.size.toString(), requireContext())
                        marker.id = waypoints.size.toString()
                        marker.isDraggable = true
                        //if(!stopsMarkers.items.contains(marker))
                        stopsMarkers.add(marker)
                        selectedMarker.postValue(marker)
                        marker.setOnMarkerDragListener(object : OnMarkerDragListener {
                            override fun onMarkerDragStart(marker: Marker) {}

                            override fun onMarkerDragEnd(marker: Marker) {
                                mapViewModel.getFromLocation(
                                    marker.position,
                                    requireContext()
                                )
                                mapViewModel.getRoute(waypoints, requireContext())
                            }

                            override fun onMarkerDrag(marker: Marker) {}
                        })
                        marker.setOnMarkerClickListener { markerClick, _ ->
                            selectedMarker.postValue(markerClick)
                            markerClick.showInfoWindow()
                            true
                        }
                    }
                    mapViewModel.getRoute(waypoints, requireContext())
                }
            } else {
                Snackbar.make(
                    map,
                    "There was an error: check your internet connection",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        })

        trip.geopoints.forEach { gp ->
            run {
                val marker = Marker(map)
                marker.position = GeoPoint(gp.latitude, gp.longitude)
                marker.isDraggable = true
                marker.setOnMarkerClickListener { markerClick, _ ->
                    selectedMarker.postValue(markerClick)
                    if (markerClick.title == null) mapViewModel.getFromLocation(
                        markerClick.position,
                        requireContext()
                    )
                    else markerClick.showInfoWindow()
                    true
                }
                marker.setOnMarkerDragListener(object : OnMarkerDragListener {
                    override fun onMarkerDragStart(marker: Marker) {}

                    override fun onMarkerDragEnd(marker: Marker) {
                        selectedMarker.postValue(marker)
                        mapViewModel.getFromLocation(
                            marker.position,
                            requireContext()
                        )
                        mapViewModel.getRoute(waypoints, requireContext())
                    }

                    override fun onMarkerDrag(marker: Marker) {}
                })
                waypoints.add(marker)
                stopsMarkers.add(marker)
            }
        }

        MapUtils.redrawMarkers(waypoints, map, requireContext())

        map.post {
            run() {
                val box = MapUtils.computeArea(
                    trip.geopoints.map { gp -> GeoPoint(gp.latitude, gp.longitude) }.toList() as ArrayList<GeoPoint>
                )
                map.zoomToBoundingBox(box, false, 110);
                map.invalidate()
            }
        };

        mapViewModel.route.observe(viewLifecycleOwner, { newRouteOverlay ->
            if (newRouteOverlay != null) {
                map.overlays.remove(routeOverlay)
                map.overlays.remove(stopsMarkers)
                routeOverlay = newRouteOverlay
                routeOverlay.outlinePaint.strokeWidth = 10f
                routeOverlay.outlinePaint.style = Paint.Style.FILL_AND_STROKE
                routeOverlay.outlinePaint.strokeCap = Paint.Cap.ROUND
                routeOverlay.outlinePaint.strokeJoin = Paint.Join.ROUND
                map.overlays.add(routeOverlay)
                map.overlays.add(stopsMarkers)
                map.invalidate()
            }
        })

        mapViewModel.getRoute(waypoints, requireContext())

        val mapEventsReceiver: MapEventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                //do whatever you need here
                selectedMarker.postValue(null)
                return false
            }

            override fun longPressHelper(p: GeoPoint): Boolean {
                //do whatever you need here
                selectedMarker.postValue(null)
                mapViewModel.getFromLocation(p, requireContext())
                return true
            }
        }
        val evOverlay = MapEventsOverlay(mapEventsReceiver)
        map.overlays.add(evOverlay)
    }

    private fun initFromNew() {
        val selectedMarker = MutableLiveData<Marker?>(null)
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
//        val waypoints = arrayListOf<Marker>()
        val stopsMarkers = FolderOverlay()
        var routeOverlay: Polyline = Polyline()

        selectedMarker.observe(viewLifecycleOwner, { marker ->
            btnDeleteMarker.isVisible = marker != null
            if (marker != null) {
                map.controller.animateTo(marker.position)
                marker.showInfoWindow()
            }
        })

        btnDeleteMarker.setOnClickListener {
            if (selectedMarker.value != null) {
                selectedMarker.value!!.closeInfoWindow()
                stopsMarkers.remove(selectedMarker.value)
                waypoints.remove(selectedMarker.value)
                MapUtils.redrawMarkers(waypoints, map, requireContext())
                mapViewModel.getRoute(waypoints, requireContext())
                selectedMarker.postValue(null)
            }
        }

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
                marker.title = address.locality
                marker.snippet = strAddress.joinToString(", ")
                marker.position = GeoPoint(address.latitude, address.longitude)
                marker.showInfoWindow()
                waypoints.add(marker)
                marker.icon =
                    MapUtils.getNumMarker(waypoints.size.toString(), requireContext())
                marker.id = waypoints.size.toString()
                marker.isDraggable = true
                marker.setOnMarkerClickListener { markerClick, _ ->
                    selectedMarker.postValue(markerClick)
                    true
                }
                marker.setOnMarkerDragListener(object : OnMarkerDragListener {
                    override fun onMarkerDragStart(marker: Marker) {}

                    override fun onMarkerDragEnd(marker: Marker) {
                        selectedMarker.postValue(marker)
                        if (marker.title == null) mapViewModel.getFromLocation(
                            marker.position,
                            requireContext()
                        )
                        mapViewModel.getRoute(waypoints, requireContext())
                    }

                    override fun onMarkerDrag(marker: Marker) {}
                })
                stopsMarkers.add(marker)
                selectedMarker.postValue(marker)
                map.invalidate()
                mapViewModel.getRoute(waypoints, requireContext())
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
                map.overlays.remove(routeOverlay)
                map.overlays.remove(stopsMarkers)
                routeOverlay = newRouteOverlay
                routeOverlay.outlinePaint.strokeWidth = 10f
                routeOverlay.outlinePaint.style = Paint.Style.FILL_AND_STROKE
                routeOverlay.outlinePaint.strokeCap = Paint.Cap.ROUND
                routeOverlay.outlinePaint.strokeJoin = Paint.Join.ROUND
                map.overlays.add(routeOverlay)
                map.overlays.add(stopsMarkers)
                map.invalidate()
            }
        })

        val mapEventsReceiver: MapEventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                //do whatever you need here
                selectedMarker.postValue(null)
                return false
            }

            override fun longPressHelper(p: GeoPoint): Boolean {
                //do whatever you need here
                selectedMarker.postValue(null)
                mapViewModel.getFromLocation(p, requireContext())
                return true
            }
        }
        val evOverlay = MapEventsOverlay(mapEventsReceiver)
        map.overlays.add(evOverlay)
        map.overlays.add(stopsMarkers)
    }

    private fun validateSave(): Boolean {
        if (waypoints.size < 2) {
            Snackbar.make(map, "You need at least 2 stops", Snackbar.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_map_edit, menu)
        optionsMenu = menu
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        validateSave()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.nav_trip_edit -> {
                if (validateSave()) {
                    createStopsArray()
                    tripEditViewModel.setTrip(trip)
/*                    val i = findNavController().backStack.size
                    val entry = findNavController().backStack.elementAt(i - 3).destination
                    findNavController().backStack.elementAt(i - 3).destination.id*/
                    //if (entry.id == R.id.nav_trip_details) {
                    val action: NavDirections = MapFragmentDirections.actionNavMapToNavTripEdit(
                        id = trip.id, isNew = trip.id == "id", fromMap = true
                    )
                    /*} else if (entry.id == R.id.nav_trip_list){
                        action = MapFragmentDirections.actionNavMapToNavTripEditSkipDetails(
                            id = trip.id
                        )
                    }else {
                        action = MapFragmentDirections.actionNavMapToNavTripEditNew(
                            id = trip.id
                        )
                    }*/
                    findNavController().navigate(action)
                }
                true
            }
            else -> item.onNavDestinationSelected(findNavController()) || super.onOptionsItemSelected(
                item
            )
        }
    }

    private fun createStopsArray() {
        val stops : ArrayList<String> = ArrayList<String>()
        val geopoints = ArrayList<com.google.firebase.firestore.GeoPoint>()
        //var i = 0

        waypoints.forEach {
            val marker = it as Marker
            val gp = com.google.firebase.firestore.GeoPoint(
                marker.position.latitude, marker.position.longitude
            )
            if (trip.geopoints.contains(gp)
            ) {
                val i = trip.geopoints.indexOf(gp)
                val stopDetails = trip.stops!![i].split(',')
                stops.add(
                    "${stopDetails[0]},${stopDetails[1]},${
                        stopDetails[2]
                    },${stopDetails[3]}"
                )
                geopoints.add(gp)
            } else {
                val stopDetails = marker.snippet.split(',')
                stops.add("${marker.title},${stopDetails[0]}${stopDetails[1]},,")
                geopoints.add(gp)
            }
        }
        trip.stops = stops
        trip.geopoints = geopoints
        tripEditViewModel.setTrip(trip)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val previousFragment = findNavController().previousBackStackEntry?.destination?.id
        if (this::trip.isInitialized && previousFragment != R.id.nav_trip_details && waypoints.size > 0) {
           // createStopsArray()
           // tripEditViewModel.setStops(trip.stops!!, trip.geopoints)
            outState.putBoolean("state", true)
        }
        super.onSaveInstanceState(outState)
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
        val previousFragment = findNavController().previousBackStackEntry?.destination?.id
        if (this::trip.isInitialized && previousFragment != R.id.nav_trip_details && waypoints.size > 0) {
            createStopsArray()
            //tripEditViewModel.setStops(trip.stops!!, trip.geopoints)
            //outState.putBoolean("state", true)
        }
        map.overlays.clear()
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