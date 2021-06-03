package com.mad.carpooling.ui.trip_edit

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.onNavDestinationSelected
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.mad.carpooling.MainActivity
import com.mad.carpooling.R
import com.mad.carpooling.model.Trip
import com.mad.carpooling.repository.TripRepository
import com.mad.carpooling.repository.UserRepository
import com.mad.carpooling.ui.DatePickerFragment
import com.mad.carpooling.ui.TimePickerFragment
import com.mad.carpooling.util.TripUtils
import com.mad.carpooling.viewmodel.SharedViewModel
import com.mad.carpooling.viewmodel.SharedViewModelFactory
import com.mad.carpooling.viewmodel.TripEditViewModel
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.FormatStyle
import java.util.*


private lateinit var tvDuration: TextView

class TripEditFragment : Fragment(R.layout.fragment_trip_edit) {

    private val sharedViewModel: SharedViewModel by activityViewModels {
        SharedViewModelFactory(
            TripRepository(),
            UserRepository()
        )
    }
    private val tripEditViewModel: TripEditViewModel by activityViewModels()
    private lateinit var optionsMenu: Menu
    private lateinit var ivCarPic: ImageView
    private lateinit var etSeats: EditText
    private lateinit var etPrice: EditText
    private lateinit var etDescription: EditText
    private lateinit var ibtnChattiness: ImageButton
    private lateinit var ibtnSmoking: ImageButton
    private lateinit var ibtnPets: ImageButton
    private lateinit var ibtnMusic: ImageButton
    private lateinit var stops: ArrayList<String>
    private lateinit var stopIcon: ImageView
    private lateinit var trip: Trip
    private lateinit var btnMap: MaterialButton
    private var isNew = false
    private var tripMap: HashMap<String, Trip>? = null
    private var currentPhotoPath: String? = null
    private var REQUEST_IMAGE_CAPTURE = 1
    private var REQUEST_IMAGE_FROM_GALLERY = 2
    private var TMP_FILENAME_IMG = "temp_car_pic_img.jpg"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ivCarPic = view.findViewById(R.id.iv_tripEdit_car_pic)
        tvDuration = view.findViewById(R.id.tv_tripEdit_duration)
        etSeats = view.findViewById(R.id.et_seats)
        etPrice = view.findViewById(R.id.et_price)
        etDescription = view.findViewById(R.id.et_description)
        ibtnChattiness = view.findViewById(R.id.btn_edit_chattiness)
        ibtnSmoking = view.findViewById(R.id.btn_edit_smoking)
        ibtnPets = view.findViewById(R.id.btn_edit_pets)
        ibtnMusic = view.findViewById(R.id.btn_edit_music)
        btnMap = view.findViewById(R.id.btn_tripEdit_map)

        initTrip(view, tripEditViewModel, savedInstanceState)

        ibtnChattiness.setOnClickListener {
            trip.chattiness = changeStatePreference(!trip.chattiness, ibtnChattiness)
        }
        ibtnSmoking.setOnClickListener {
            trip.smoking = changeStatePreference(!trip.smoking, ibtnSmoking)
        }
        ibtnPets.setOnClickListener {
            trip.pets = changeStatePreference(!trip.pets, ibtnPets)
        }
        ibtnMusic.setOnClickListener {
            trip.music = changeStatePreference(!trip.music, ibtnMusic)
        }

        val btnCamera = view.findViewById<MaterialButton>(R.id.btn_tripEdit_camera)
        registerForContextMenu(btnCamera)
        btnCamera.setOnClickListener { activity?.openContextMenu(btnCamera) }

    }

    private fun initTrip(view: View, viewModel: TripEditViewModel, savedInstanceState: Bundle?) {
        val args: TripEditFragmentArgs by navArgs()
        val rv = view.findViewById<RecyclerView>(R.id.rv_tripEdit_stops)
        val emptyView = view.findViewById<TextView>(R.id.no_stops_available)
        rv.layoutManager = LinearLayoutManager(context)
        rv.isNestedScrollingEnabled = false
        tripMap = sharedViewModel.getMyTrips().value
        isNew = args.isNew


        if (savedInstanceState == null) {
            val previousFragment = findNavController().previousBackStackEntry?.destination?.id
            if (!args.isNew) {  // navigating from any edit btn
                if (!args.fromMap && args.id != "id")
                    viewModel.setTrip(tripMap?.get(args.id)!!.copy())
            } else { // navigating from tripList FAB
                (activity as MainActivity).supportActionBar?.title = "Create New Trip"
                if (!args.fromMap)
                    viewModel.setTrip(Trip())
            }
        } else {
            currentPhotoPath = savedInstanceState.getString("state_currentPhotoPath")
        }

        trip = viewModel.getTrip()

        emptyView.isVisible = trip.stops.isEmpty()

        btnMap.setOnClickListener {
            if (this::trip.isInitialized && trip != null) {
                trip.stops = stops
                viewModel.setTrip(trip)
            }
            val action = TripEditFragmentDirections.actionNavTripEditToNavMap(
                if (isNew) null else trip.id
            )
            findNavController().navigate(action)
        }

        if (currentPhotoPath != null) {
            BitmapFactory.decodeFile(currentPhotoPath)?.also { bitmap ->
                ivCarPic.setImageBitmap(bitmap)
            }
        } else {
            if (trip.imageCarURL != "" && trip.imageCarURL != null) {
                Glide.with(requireContext()).load(trip.imageCarURL).into(ivCarPic)
            }
        }

        stops = trip.stops.toMutableList() as ArrayList<String>
        val stopEditAdapter = StopEditAdapter(stops)
        if (stops.isNotEmpty() && TripUtils.checkStopsValidity(stops)) tvDuration.text =
            TripUtils.calcDuration(stops)
        etSeats.setText(trip.seats.toString())
        etPrice.setText(trip.price.toString())
        etDescription.setText(trip.description)

        rv.adapter = stopEditAdapter
        initPreferences()


        val fab = view.findViewById<FloatingActionButton>(R.id.fab_tripedit)
        if (!isNew) {
            initFab(viewModel, fab)

            val scrollView = view.findViewById<ScrollView>(R.id.sv_editTrip)
            scrollView.setOnScrollChangeListener { scrollView, scrollX, scrollY, oldScrollX, oldScrollY ->
                if (scrollY > oldScrollY && fab.visibility == View.VISIBLE && oldScrollY > 0) {
                    fab.hide()

                } else if (scrollY < oldScrollY && fab.visibility != View.VISIBLE)
                    fab.show()
            }
        } else {
            fab.hide()
        }

    }

    private fun initFab(viewModel: TripEditViewModel, fab: FloatingActionButton) {

        fab.show()

        TripUtils.changeStateFab(fab, trip, requireContext())

        fab.setOnClickListener {
            if (trip.visibility) {
                var hideVis = HideDialogFragment(viewModel, fab)
                hideVis.show(requireActivity().supportFragmentManager, "visibilityDialog")
            } else {
                var showVis = ShowDialogFragment(viewModel, fab)
                showVis.show(requireActivity().supportFragmentManager, "visibilityDialog")
            }

        }
    }

    class HideDialogFragment(viewModel: TripEditViewModel, var fab: FloatingActionButton) :
        DialogFragment() {
        var trip = viewModel.getTrip()

        override fun onPause() {
            super.onPause()
            dismissAllowingStateLoss()
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            // Use the Builder class for convenient dialog construction
            val builder: AlertDialog.Builder = AlertDialog.Builder(activity)
            builder.setMessage("Do you want to hide the trip?")
                .setPositiveButton("Confirm") { dialog, id ->
                    // hide the trip
                    trip.visibility = false
                    TripUtils.changeStateFab(fab, trip, requireContext())
                }
                .setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, id ->
                    // User cancelled the dialog

                })
            // Create the AlertDialog object and return it
            return builder.create()
        }
    }

    class ShowDialogFragment(viewModel: TripEditViewModel, var fab: FloatingActionButton) :
        DialogFragment() {
        var trip = viewModel.getTrip()

        override fun onPause() {
            super.onPause()
            dismissAllowingStateLoss()
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            // Use the Builder class for convenient dialog construction
            val builder: AlertDialog.Builder = AlertDialog.Builder(activity)
            builder.setMessage("Do you want to show the trip?")
                .setPositiveButton("Confirm") { dialog, id ->
                    trip.visibility = true
                    TripUtils.changeStateFab(fab, trip, requireContext())
                }
                .setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, id ->
                })
            // Create the AlertDialog object and return it
            return builder.create()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (this::trip.isInitialized && trip != null && findNavController().currentDestination?.id != R.id.nav_map) {
            trip.stops = stops
            tripEditViewModel.setTrip(trip)
            outState.putString("state_currentPhotoPath", currentPhotoPath)
        }
    }

    private fun initPreferences() {
        trip.chattiness = changeStatePreference(trip.chattiness, ibtnChattiness)
        trip.smoking = changeStatePreference(trip.smoking, ibtnSmoking)
        trip.pets = changeStatePreference(trip.pets, ibtnPets)
        trip.music = changeStatePreference(trip.music, ibtnMusic)
    }

    private fun changeStatePreference(state: Boolean, btn: ImageButton): Boolean {
        val typedValue = TypedValue()
        val theme = requireContext().theme

        val color: Int = if (state) {
            theme.resolveAttribute(R.attr.colorControlActivated, typedValue, true)
            typedValue.data
        } else {
            theme.resolveAttribute(R.attr.colorControlNormal, typedValue, true)
            typedValue.data
        }
        btn.isSelected = state
        btn.setColorFilter(color)
        return state
    }

    private fun dispatchGalleryPickerIntent() {
        val galleryPickerIntent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.INTERNAL_CONTENT_URI
        )
        startActivityForResult(galleryPickerIntent, REQUEST_IMAGE_FROM_GALLERY)

    }

    private fun updatePathFromGallery(imageUri: Uri?) {
        val inputStream = requireContext().contentResolver.openInputStream(imageUri!!)
        val cursor = requireContext().contentResolver.query(imageUri, null, null, null, null)
        cursor?.use { c ->
            val nameIndex = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (c.moveToFirst()) {
                val name = c.getString(nameIndex)
                inputStream?.let { inputStream ->
                    // create same file with same name
                    val file = File(requireContext().cacheDir, name)
                    val os = file.outputStream()
                    os.use {
                        inputStream.copyTo(it)
                    }
                    currentPhotoPath = file.absolutePath
                }
            }
        }
    }

    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            takePictureIntent.also {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                    Log.e("photoFile", "Error creating file")
                    null
                }
                // Continue only if the File was successfully created
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        requireContext(),
                        "com.mad.group05.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                }
            }
        } catch (e: ActivityNotFoundException) {
            // display error state to the user
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ITALY).format(Date())
        val storageDir: File? = activity?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    private fun setPic() {
        // Get the dimensions of the View
        //16:9 ratio
        val targetW = 720
        val targetH = 405

        val bmOptions = BitmapFactory.Options().apply {
            // Get the dimensions of the bitmap
            inJustDecodeBounds = true


            BitmapFactory.decodeFile(currentPhotoPath, this)

            val photoW: Int = outWidth
            val photoH: Int = outHeight

            // Determine how much to scale down the image
            val scaleFactor: Int =
                1.coerceAtLeast((photoW / targetW).coerceAtMost(photoH / targetH))

            // Decode the image file into a Bitmap sized to fill the View
            inJustDecodeBounds = false
            inSampleSize = scaleFactor
        }
        BitmapFactory.decodeFile(currentPhotoPath, bmOptions)?.also { bitmap ->
            ivCarPic.setImageBitmap(validateBitmapOrientation(bitmap))
        }
    }

    private fun validateBitmapOrientation(bitmap: Bitmap): Bitmap? {
        val ei = currentPhotoPath?.let { ExifInterface(it) }
        val orientation: Int? = ei?.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED
        )

        val rotatedBitmap: Bitmap? = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(bitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(bitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(bitmap, 270f)
            ExifInterface.ORIENTATION_NORMAL -> bitmap
            else -> bitmap
        }

        saveRotatedBitmap(rotatedBitmap)
        return rotatedBitmap
    }

    private fun rotateImage(source: Bitmap, angle: Float): Bitmap? {
        val matrix = Matrix()

        matrix.postRotate(angle)
        return Bitmap.createBitmap(
            source, 0, 0, source.width, source.height,
            matrix, true
        )
    }

    private fun saveRotatedBitmap(rotatedBitmap: Bitmap?) {
        val oldFile = File(currentPhotoPath!!)
        val filename = TMP_FILENAME_IMG
        val imgPath = activity?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val myFile = File(imgPath, filename)
        if (myFile.exists()) {
            myFile.delete()
        }
        val fileOutputStream = FileOutputStream(myFile)

        rotatedBitmap?.compress(Bitmap.CompressFormat.JPEG, 90, fileOutputStream)
        fileOutputStream.flush()
        fileOutputStream.close()
        oldFile.delete()    // delete old img with wrong orientation, maybe is better to add a check if successful?
        currentPhotoPath = myFile.absolutePath  // update the path to point to the new fixed img
    }

    private fun saveCarImage() {
        val imgPath = activity?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val uniqueString = trip.owner?.id + Timestamp.now()
        val filename =
            MessageDigest.getInstance("SHA256").digest(uniqueString.toByteArray())
                .toString() + ".jpg"
        val myFile = File(imgPath, filename)
        val file = Uri.fromFile(myFile)
        val fileOutputStream = FileOutputStream(myFile)
        val carRef = Firebase.storage.reference.child("images_car/${file.lastPathSegment}")

        (ivCarPic.drawable).toBitmap()
            .compress(Bitmap.CompressFormat.JPEG, 90, fileOutputStream)
        fileOutputStream.close()

        // Register observers to listen for when the download is done or if it fails
        carRef.putFile(file).addOnFailureListener {
            // Handle unsuccessful uploads
        }.addOnSuccessListener {
            myFile.delete()
            currentPhotoPath = file.lastPathSegment
            val tmpFile = File(imgPath, TMP_FILENAME_IMG)
            tmpFile.delete()
            carRef.downloadUrl.addOnCompleteListener {
                trip.imageCarURL = it.result.toString()
                updateFirestoreTrips()
            }
        }

    }

    private fun validateSave(): Boolean {
        return etSeats.text.trim().isNotEmpty()
                && etPrice.text.trim().isNotEmpty()
                && ((isNew && currentPhotoPath != null) || !isNew)
                && TripUtils.checkStopsValidity(stops)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_trip_edit, menu)
        optionsMenu = menu
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.nav_trip_details -> {
                if (currentPhotoPath != null) {
                    saveCarImage()
                } else {
                    if (!validateSave()) {
                        Snackbar.make(
                            requireView(),
                            "Please fill all the fields",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    } else {
                        updateFirestoreTrips()
                    }
                }
                true
            }
            else -> item.onNavDestinationSelected(findNavController()) || super.onOptionsItemSelected(
                item
            )
        }
    }

    private fun updateFirestoreTrips() {
        val dateFormat = SimpleDateFormat("yyyy-MM-ddHH:mm", Locale.getDefault())
        val parsedDate = dateFormat.parse(stops[0].split(",")[2] + stops[0].split(",")[3])
        val timestamp: Timestamp = Timestamp(parsedDate!!)


        val userRef =
            FirebaseFirestore.getInstance()
                .document("users/${sharedViewModel.getCurrentUser().value?.uid}")
        val db = Firebase.firestore
        val newDocRef = db.collection("trips").document()

        trip.timestamp = timestamp
        trip.seats = etSeats.text.trim().toString().toInt()
        trip.price = etPrice.text.trim().toString().toFloat()
        trip.description = etDescription.text.trim().toString()
        trip.stops = stops
        trip.departure = stops[0].split(",")[0]
        trip.arrival = stops[stops.size - 1].split(",")[0]

        trip.id = if (isNew) {
            newDocRef.id
        } else {
            trip.id
        }

        if (isNew) {
            trip.owner = userRef
            newDocRef.set(trip).addOnSuccessListener {
                Snackbar.make(requireView(), "Trip created", Snackbar.LENGTH_SHORT).show()
                navigateToTripDetails(trip.id)
            }
        } else {
            if (!trip.visibility) {
                for (user in trip.interestedPeople!!) {
                    if (trip.acceptedPeople?.contains(user)!!) {
                        trip.seats++
                    }
                    db.collection("users").document(user).update(
                        "favTrips", FieldValue.arrayRemove(trip.id)
                    )
                }
                trip.acceptedPeople = ArrayList<String>()
                trip.interestedPeople = ArrayList<String>()
            }
            db.collection("trips").document(trip.id).set(trip).addOnSuccessListener {
                Snackbar.make(requireView(), "Trip updated", Snackbar.LENGTH_SHORT).show()
                navigateToTripDetails(trip.id)
            }
        }

    }

    private fun navigateToTripDetails(newId: String) {
        val action = TripEditFragmentDirections.actionNavTripEditToNavTripDetails(
            id = newId
        )
        findNavController().navigate(action)

        Snackbar.make(requireView(), "Trip saved", Snackbar.LENGTH_SHORT).show()
    }

    override fun onCreateContextMenu(
        menu: ContextMenu,
        v: View,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val inflater: MenuInflater? = activity?.menuInflater
        inflater?.inflate(R.menu.ctx_menu_camera, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.ctx_gallery -> {
                dispatchGalleryPickerIntent()
                true
            }
            R.id.ctx_camera -> {
                dispatchTakePictureIntent()
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            setPic()
        } else if (requestCode == REQUEST_IMAGE_FROM_GALLERY && resultCode == Activity.RESULT_OK) {
            val imageUri = data?.data
            updatePathFromGallery(imageUri)
            setPic()
        }
    }
}

class StopEditAdapter(val stops: ArrayList<String>) :
    RecyclerView.Adapter<StopEditAdapter.StopEditViewHolder>() {

    class StopEditViewHolder(v: View) : RecyclerView.ViewHolder(v) {

        private var stopCity: TextView = v.findViewById(R.id.et_stop_city)
        private var stopAddress: TextView = v.findViewById(R.id.et_stop_address)
        private var stopDate: EditText = v.findViewById(R.id.et_stop_date)
        private var stopTime: EditText = v.findViewById(R.id.et_stop_time)
        private var stopIcon: ImageView = v.findViewById(R.id.rv_stop_edit_icon)

        fun bind(stops: ArrayList<String>, position: Int) {
            Log.d("bind:", stops[position])
            val stringArray = stops[position].split(",")
            var stringCity = stringArray[0].trim()
            var stringAddress = stringArray[1].trim()
            var stringDate = stringArray[2].trim()
            var stringTime = stringArray[3].trim()
            when (position) {
                0 -> stopIcon.setImageDrawable(
                    ContextCompat.getDrawable(
                        this.itemView.context,
                        R.drawable.ic_twotone_stop_start
                    )
                )
                stops.size - 1 -> stopIcon.setImageDrawable(
                    ContextCompat.getDrawable(
                        this.itemView.context,
                        R.drawable.ic_twotone_stop_end
                    )
                )
                else -> stopIcon.setImageDrawable(
                    ContextCompat.getDrawable(
                        this.itemView.context,
                        R.drawable.ic_twotone_stop
                    )
                )
            }
            stopCity.text = stringCity
            stopAddress.text = stringAddress
            val localizedDate = if (stringDate.isNotEmpty()) LocalDate.parse(
                stringDate,
                DateTimeFormatter.ISO_LOCAL_DATE
            )
                .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)) else "Date"
            val localizedTime = if (stringTime.isNotEmpty()) LocalTime.parse(
                stringTime,
                DateTimeFormatter.ISO_LOCAL_TIME
            )
                .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)) else "Time"

            stopDate.setText(localizedDate)
            stopTime.setText(localizedTime)

            stopDate.addTextChangedListener(object : TextWatcher {
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    stringDate = s.toString()
                    stops[position] = formatStops(stringCity, stringAddress, stringDate, stringTime)
                    if (TripUtils.checkStopsValidity(stops)) tvDuration.text =
                        TripUtils.calcDuration(stops)
                }

                override fun afterTextChanged(s: Editable?) {

                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }
            })

            stopTime.addTextChangedListener(object : TextWatcher {
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    //TODO check time today: if earlier than now,
                    stringTime = s.toString()
                    stops[position] = formatStops(stringCity, stringAddress, stringDate, stringTime)
                    if (TripUtils.checkStopsValidity(stops)) tvDuration.text =
                        TripUtils.calcDuration(stops)
                }

                override fun afterTextChanged(s: Editable?) {

                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }
            })

            stopDate.setOnClickListener {
                showDatePickerDialog()
            }
            stopTime.setOnClickListener {
                showTimePickerDialog()
            }
        }

        private fun formatStops(
            stringCity: String,
            stringAddress: String,
            stringDate: String,
            stringTime: String
        ): String {

            val date = try {
                LocalDate.parse(
                    stringDate,
                    DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
                )
                    .format(
                        DateTimeFormatter.ISO_LOCAL_DATE
                    )
            } catch (e: DateTimeParseException) {
                if (stringDate.isEmpty()) "" else stringDate
            }
            val time: String = try {
                LocalTime.parse(
                    stringTime,
                    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                )
                    .format(DateTimeFormatter.ISO_LOCAL_TIME)

            } catch (e: DateTimeParseException) {
                if (stringTime.isEmpty()) "" else stringTime
            }
            return "${stringCity},${stringAddress},${date},${time}"
        }

        private fun showDatePickerDialog() {
            val dateFragment = DatePickerFragment(stopDate) //TODO
            dateFragment.show(
                (stopDate.context as AppCompatActivity).supportFragmentManager,
                "datePicker"
            )
        }

        private fun showTimePickerDialog() {
            val timeFragment = TimePickerFragment(stopTime) //TODO
            timeFragment.show(
                (stopTime.context as AppCompatActivity).supportFragmentManager,
                "timePicker"
            )
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StopEditViewHolder {
        val layout =
            LayoutInflater.from(parent.context).inflate(R.layout.rv_stop_edit, parent, false)
        return StopEditViewHolder(layout)
    }

    override fun onBindViewHolder(holder: StopEditViewHolder, position: Int) {
        holder.bind(stops, position)
    }

    override fun getItemCount(): Int {
        return stops.size
    }

    fun addEmpty(stop: String, position: Int) {
        stops.add(stop)
        notifyItemInserted(position - 1)
    }

}
