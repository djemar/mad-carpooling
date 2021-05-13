package com.mad.carpooling.ui.trip_edit

import android.annotation.SuppressLint
import android.app.*
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.icu.util.Calendar
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.onNavDestinationSelected
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.mad.carpooling.MainActivity
import com.mad.carpooling.R
import com.mad.carpooling.data.Trip
import com.mad.carpooling.ui.SharedViewModel
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*


class TripEditFragment : Fragment(R.layout.fragment_trip_edit) {

    private val model: SharedViewModel by activityViewModels()
    private lateinit var optionsMenu: Menu
    private lateinit var ivCarPic: ImageView
    private lateinit var tvDate: TextView
    private lateinit var tvTime: TextView
    private lateinit var etDepartureLocation: EditText
    private lateinit var etArrivalLocation: EditText
    private lateinit var etDuration: EditText
    private lateinit var etSeats: EditText
    private lateinit var etPrice: EditText
    private lateinit var etDescription: EditText
    private lateinit var ibtnChattiness: ImageButton
    private lateinit var ibtnSmoking: ImageButton
    private lateinit var ibtnPets: ImageButton
    private lateinit var ibtnMusic: ImageButton
    private lateinit var stops: ArrayList<String>
    private lateinit var trip: Trip
    private var isNew = false
    private var tripMap: HashMap<String, Trip>? = null
    private var currentPhotoPath: String? = null
    private var REQUEST_IMAGE_CAPTURE = 1
    private var REQUEST_IMAGE_FROM_GALLERY = 2
    private var TMP_FILENAME_IMG = "temp_car_pic_img.jpg"
    private val viewModel: TripEditViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ivCarPic = view.findViewById(R.id.iv_tripEdit_car_pic)
        tvDate = view.findViewById(R.id.tv_tripEdit_date)
        tvTime = view.findViewById(R.id.tv_tripEdit_time)
        etDepartureLocation = view.findViewById(R.id.et_departure)
        etArrivalLocation = view.findViewById(R.id.et_arrival)
        etDuration = view.findViewById(R.id.et_duration)
        etSeats = view.findViewById(R.id.et_seats)
        etPrice = view.findViewById(R.id.et_price)
        etDescription = view.findViewById(R.id.et_description)
        ibtnChattiness = view.findViewById(R.id.btn_edit_chattiness)
        ibtnSmoking = view.findViewById(R.id.btn_edit_smoking)
        ibtnPets = view.findViewById(R.id.btn_edit_pets)
        ibtnMusic = view.findViewById(R.id.btn_edit_music)

        initTrip(view, viewModel, savedInstanceState)

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

        val btnDate = view.findViewById<MaterialButton>(R.id.edit_date)
        btnDate.setOnClickListener { showDatePickerDialog() }

        val btnTime = view.findViewById<MaterialButton>(R.id.edit_time)
        btnTime.setOnClickListener { showTimePickerDialog() }

        val btnCamera = view.findViewById<ImageButton>(R.id.btn_tripEdit_camera)
        registerForContextMenu(btnCamera)
        btnCamera.setOnClickListener { activity?.openContextMenu(btnCamera) }
    }

    private fun initTrip(view: View, viewModel: TripEditViewModel, savedInstanceState: Bundle?) {
        val args: TripEditFragmentArgs by navArgs()
        val rv = view.findViewById<RecyclerView>(R.id.rv_tripEdit_stops)
        rv.layoutManager = LinearLayoutManager(context)
        tripMap = model.getMyTrips().value
        isNew = args.isNew

        if (savedInstanceState == null) {
            if (!args.isNew) {  // navigating from any edit btn
                viewModel.setTrip(tripMap?.get(args.id)!!.copy())
            } else { // navigating from tripList FAB
                (activity as MainActivity).supportActionBar?.title = "Create New Trip"
                viewModel.setTrip(Trip())
            }
        } else {
            currentPhotoPath = savedInstanceState.getString("state_currentPhotoPath")
        }

        trip = viewModel.getTrip()

        if (currentPhotoPath != null) {
            BitmapFactory.decodeFile(currentPhotoPath)?.also { bitmap ->
                ivCarPic.setImageBitmap(bitmap)
            }
        } else {
            if (trip.imageCarURL != "" && trip.imageCarURL != null) {
                Glide.with(requireContext()).load(trip.imageCarURL).into(ivCarPic)
            }
        }

        stops = trip.stops!!.toMutableList() as ArrayList<String>
        val stopEditAdapter = StopEditAdapter(stops)
        tvDate.text =
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(trip.timestamp.toDate())
                .toString()
        tvTime.text =
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(trip.timestamp.toDate())
                .toString()
        etDepartureLocation.setText(trip.departure)
        etArrivalLocation.setText(trip.arrival)
        etDuration.setText(trip.duration)
        etSeats.setText(trip.seats.toString())
        etPrice.setText(trip.price.toString())
        etDescription.setText(trip.description)

        rv.adapter = stopEditAdapter
        initPreferences()

        val btnAddStop = view.findViewById<MaterialButton>(R.id.ib_add_stop)
        btnAddStop.setOnClickListener {
            stopEditAdapter.addEmpty(",,", stops.size + 1)
        }

        val fab = (activity as MainActivity).findViewById<ExtendedFloatingActionButton>(R.id.fab)
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

    private fun changeStateFab(fab: ExtendedFloatingActionButton) {
        if (trip.visibility) {
            fab.icon = ContextCompat.getDrawable(
                requireContext(),
                R.drawable.ic_sharp_visibility
            )
            fab.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.green_700))
        } else {
            fab.icon = ContextCompat.getDrawable(
                requireContext(),
                R.drawable.ic_baseline_visibility_off
            )
            fab.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.red_700))
        }
    }

    private fun initFab(viewModel: TripEditViewModel, fab: ExtendedFloatingActionButton) {

        fab.show()
        fab.shrink()

        changeStateFab(fab)

        fab.setOnClickListener {
            if (trip.visibility) {
                var hideVis = HideDialogFragment(viewModel, fab)
                hideVis.show(requireActivity().supportFragmentManager, "visibilityDialog")
                /* val alertDialog: AlertDialog? = activity?.let {
                    val builder = AlertDialog.Builder(it)
                    builder.apply {

                        setPositiveButton("Confirm",
                            DialogInterface.OnClickListener {
                                // User clicked confirm button
                            })
                        setNegativeButton("Cancel",
                            DialogInterface.OnClickListener {
                                // User cancelled the dialog
                            })
                    }
                    // Set other dialog properties
                    // Create the AlertDialog
                    builder.create()
                } */
            } else {
                var showVis = ShowDialogFragment(viewModel, fab)
                showVis.show(requireActivity().supportFragmentManager, "visibilityDialog")
            }

        }
    }

    class HideDialogFragment(viewModel: TripEditViewModel, fab: ExtendedFloatingActionButton) :
        DialogFragment() {
        var trip = viewModel.getTrip()
        var vm = viewModel
        var efab = fab

        private fun changeStateFab(fab: ExtendedFloatingActionButton) {
            if (trip.visibility) {
                fab.icon = ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.ic_sharp_visibility
                )
                fab.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.green_700))
            } else {
                fab.icon = ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.ic_baseline_visibility_off
                )
                fab.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.red_700))
            }
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            // Use the Builder class for convenient dialog construction
            val builder: AlertDialog.Builder = AlertDialog.Builder(activity)
            val db = Firebase.firestore
            builder.setMessage("Do you want to hide the trip?")
                .setPositiveButton("Confirm", DialogInterface.OnClickListener { dialog, id ->
                    // hide the trip
                    db.collection("trips").document(trip.id).update("visibility", false)
                    db.collection("trips").document(trip.id)
                        .update("seats", trip.seats + trip.acceptedPeople!!.size)
                        .addOnSuccessListener {
                            for (user in trip.interestedPeople!!) {
                                db.collection("users").document(user).update(
                                    "favTrips", FieldValue.arrayRemove(trip.id)
                                )
                                db.collection("trips").document(trip.id).update(
                                    "interestedPeople", FieldValue.arrayRemove(user)
                                )
                                db.collection("trips").document(trip.id).update(
                                    "acceptedPeople", FieldValue.arrayRemove(user)
                                )
                            }
                        }
                    trip.visibility = false
                    vm.setTrip(trip)
                    changeStateFab(efab)
                })
                .setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, id ->
                    // User cancelled the dialog

                })
            // Create the AlertDialog object and return it
            return builder.create()
        }
    }

    class ShowDialogFragment(viewModel: TripEditViewModel, fab: ExtendedFloatingActionButton) :
        DialogFragment() {
        var trip = viewModel.getTrip()
        var vm = viewModel
        var efab = fab

        private fun changeStateFab(fab: ExtendedFloatingActionButton) {
            if (trip.visibility) {
                fab.icon = ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.ic_sharp_visibility
                )
                fab.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.green_700))
            } else {
                fab.icon = ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.ic_baseline_visibility_off
                )
                fab.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.red_700))
            }
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            // Use the Builder class for convenient dialog construction
            val builder: AlertDialog.Builder = AlertDialog.Builder(activity)
            val db = Firebase.firestore
            builder.setMessage("Do you want to show the trip?")
                .setPositiveButton("Confirm", DialogInterface.OnClickListener { dialog, id ->
                    db.collection("trips").document(trip.id).update("visibility", true)
                    trip.visibility = true
                    vm.setTrip(trip)
                    changeStateFab(efab)
                })
                .setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, id ->
                })
            // Create the AlertDialog object and return it
            return builder.create()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        trip.stops = stops
        viewModel.setTrip(trip)
        outState.putString("state_currentPhotoPath", currentPhotoPath)
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

    private fun validateTripForm() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validateSave()
            }

            override fun afterTextChanged(s: Editable?) {
            }

        }

        etDepartureLocation.addTextChangedListener(textWatcher)
        etArrivalLocation.addTextChangedListener(textWatcher)
        etDuration.addTextChangedListener(textWatcher)
        etSeats.addTextChangedListener(textWatcher)
        etPrice.addTextChangedListener(textWatcher)
    }

    private fun validateSave() {
        optionsMenu.findItem(R.id.save_trip).isEnabled =
            etDepartureLocation.text.trim().isNotEmpty() && etArrivalLocation.text.trim()
                .isNotEmpty() && etDuration.text.trim().isNotEmpty() && etSeats.text.trim()
                .isNotEmpty() && etPrice.text.trim()
                .isNotEmpty() && ((isNew && currentPhotoPath != null) || !isNew)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_trip_edit, menu)
        optionsMenu = menu
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        validateSave()
        validateTripForm()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.save_trip -> {
                if (currentPhotoPath != null) {
                    saveCarImage()
                } else {
                    updateFirestoreTrips()
                }
                true
            }
            else -> item.onNavDestinationSelected(findNavController()) || super.onOptionsItemSelected(
                item
            )
        }
    }

    private fun updateFirestoreTrips() {
        val dateFormat = SimpleDateFormat("dd/MM/yyyyHH:mm")
        val parsedDate = dateFormat.parse(tvDate.text.toString() + tvTime.text.toString())
        val timestamp: Timestamp = Timestamp(parsedDate!!)


        // TODO take username from login
        val userRef =
            FirebaseFirestore.getInstance().document("users/${model.getCurrentUser().value?.uid}")
        val db = Firebase.firestore
        val newDocRef = db.collection("trips").document()

        trip.departure = etDepartureLocation.text.trim().toString()
        trip.arrival = etArrivalLocation.text.trim().toString()
        trip.duration = etDuration.text.trim().toString()
        trip.timestamp = timestamp
        trip.seats = etSeats.text.trim().toString().toInt()
        trip.price = etPrice.text.trim().toString().toFloat()
        trip.description = etDescription.text.trim().toString()
        trip.stops = stops

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

    class DatePickerFragment(tvDate: TextView) : DialogFragment(),
        DatePickerDialog.OnDateSetListener {

        private val tvDate = tvDate

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            // Use the current date as the default date in the picker
            val c = Calendar.getInstance()
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(requireContext(), this, year, month, day)
            datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000

            // Create a new instance of DatePickerDialog and return it
            return datePickerDialog
        }

        @SuppressLint("SetTextI18n")
        override fun onDateSet(view: DatePicker, year: Int, month: Int, day: Int) {
            tvDate.text = "${day}/${(month + 1)}/${year}"
        }

    }

    private fun showDatePickerDialog() {
        val dateFragment = DatePickerFragment(tvDate)
        dateFragment.show(requireActivity().supportFragmentManager, "datePicker")
    }

    class TimePickerFragment(tvTime: TextView) : DialogFragment(),
        TimePickerDialog.OnTimeSetListener {

        private val tvTime = tvTime

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            // Use the current time as the default values for the picker
            val c = Calendar.getInstance()
            val hour = c.get(Calendar.HOUR_OF_DAY)
            val minute = c.get(Calendar.MINUTE)

            // Create a new instance of TimePickerDialog and return it
            return TimePickerDialog(
                activity,
                this,
                hour,
                minute,
                DateFormat.is24HourFormat(activity)
            )
        }

        @SuppressLint("SetTextI18n")
        override fun onTimeSet(view: TimePicker, hourOfDay: Int, minute: Int) {
            if (minute < 10)
                tvTime.text = "$hourOfDay:0$minute"
            else
                tvTime.text = "$hourOfDay:$minute"
        }
    }

    private fun showTimePickerDialog() {
        val timeFragment = TimePickerFragment(tvTime)
        timeFragment.show(requireActivity().supportFragmentManager, "timePicker")
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

        private var stopName: EditText = v.findViewById(R.id.et_stop_location)
        private var stopDate: EditText = v.findViewById(R.id.et_stop_date)
        private var stopTime: EditText = v.findViewById(R.id.et_stop_time)
        var deleteBtn: ImageButton = v.findViewById(R.id.ib_edit_delete_stop)

        fun bind(stops: ArrayList<String>, position: Int) {
            Log.d("bind:", stops[position])
            val stringArray = stops[position].split(",")
            var stringName = stringArray[0].trim()
            var stringDate = stringArray[1].trim()
            var stringTime = stringArray[2].trim()
            stopName.setText(stringName)
            stopDate.setText(stringDate)
            stopTime.setText(stringTime)

            var stop: String

            stopName.addTextChangedListener(object : TextWatcher {
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    stringName = s.toString()
                    Log.d("NAME:", s.toString())
                    stop = "${stringName},${stringDate},${stringTime}"
                    stops[position] = stop
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

            stopDate.addTextChangedListener(object : TextWatcher {
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    stringDate = s.toString()
                    Log.d("DATE:", s.toString())
                    stop = "${stringName},${stringDate},${stringTime}"
                    stops[position] = stop
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
                    stringTime = s.toString()
                    Log.d("TIME:", s.toString())
                    stop = "${stringName},${stringDate},${stringTime}"
                    stops[position] = stop
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
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StopEditViewHolder {
        val layout =
            LayoutInflater.from(parent.context).inflate(R.layout.stop_edit_layout, parent, false)
        return StopEditViewHolder(layout)
    }

    override fun onBindViewHolder(holder: StopEditViewHolder, position: Int) {
        holder.bind(stops, position)
        holder.deleteBtn.setOnClickListener {
            stops.removeAt(position)
            notifyDataSetChanged()
        }
    }

    override fun getItemCount(): Int {
        return stops.size
    }

    fun addEmpty(stop: String, position: Int) {
        stops.add(stop)
        notifyItemInserted(position - 1)
    }

}