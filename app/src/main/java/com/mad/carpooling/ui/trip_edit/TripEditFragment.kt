package com.mad.carpooling.ui.trip_edit

import android.app.Activity
import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
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
import android.text.format.DateFormat
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.widget.*
import androidx.activity.addCallback
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.onNavDestinationSelected
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.mad.carpooling.R
import com.mad.carpooling.TripUtil
import com.mad.carpooling.ui.profile_edit.EditProfileFragmentDirections
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class TripEditFragment : Fragment(R.layout.fragment_trip_edit) {

    private lateinit var tripEditViewModel: TripEditViewModel
    private lateinit var trip: TripUtil.Trip

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
    private lateinit var bundleStops: Bundle
    private lateinit var stops: HashMap<Int,String>
    private var chattiness = false
    private var smoking = false
    private var pets = false
    private var music = false
    private var currentPhotoPath: String? = null
    private var REQUEST_IMAGE_CAPTURE = 1
    private var REQUEST_IMAGE_FROM_GALLERY = 2
    private var TMP_FILENAME_IMG = "temp_car_pic_img.jpg"
    private var FILENAME_IMG = "car_pic_img.jpg"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)

        val callback = requireActivity().onBackPressedDispatcher.addCallback(this) {
            activity?.setResult(Activity.RESULT_CANCELED)
            findNavController().navigate(
                R.id.action_nav_trip_edit_to_nav_trip_details
            )
        }
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

        val args: TripEditFragmentArgs by navArgs()

        if(!args.isNew) {

            initTrip(view)

            ibtnChattiness.setOnClickListener {
                chattiness = changeStatePreference(!chattiness, ibtnChattiness)
            }
            ibtnSmoking.setOnClickListener {
                smoking = changeStatePreference(!smoking, ibtnSmoking)
            }
            ibtnPets.setOnClickListener {
                pets = changeStatePreference(!pets, ibtnPets)
            }
            ibtnMusic.setOnClickListener {
                music = changeStatePreference(!music, ibtnMusic)
            }

            if (savedInstanceState != null) {
                chattiness = savedInstanceState.getBoolean("chattiness")
                smoking = savedInstanceState.getBoolean("smoking")
                pets = savedInstanceState.getBoolean("pets")
                music = savedInstanceState.getBoolean("music")
                chattiness = changeStatePreference(chattiness, ibtnChattiness)
                smoking = changeStatePreference(smoking, ibtnSmoking)
                pets = changeStatePreference(pets, ibtnPets)
                music = changeStatePreference(music, ibtnMusic)
            } else {
                initPreferences()
            }
        }
        else{
            val rv = view.findViewById<RecyclerView>(R.id.rv_tripEdit_stops)
            rv.layoutManager = LinearLayoutManager(context);
            val stopAdapter = StopAdapter(HashMap<Int,String>())
            rv.adapter = stopAdapter
        }

        val btnDate = view.findViewById<MaterialButton>(R.id.edit_date)
        btnDate.setOnClickListener { showDatePickerDialog(view) }

        val btnTime = view.findViewById<MaterialButton>(R.id.edit_time)
        btnTime.setOnClickListener { showTimePickerDialog(view) }

        var btnCamera = view.findViewById<ImageButton>(R.id.btn_tripEdit_camera)
        registerForContextMenu(btnCamera)
        btnCamera.setOnClickListener { activity?.openContextMenu(btnCamera) }

    }

    private fun initTrip(view: View) {
        val args: TripEditFragmentArgs by navArgs()

        val tripId = args.id
        val bundleStops = args.stops!!
        stops = bundleStops.getSerializable("stops") as HashMap<Int, String>

        trip = TripUtil.Trip(
            args.id,
            "args.nickname",
            args.departure,
            args.arrival,
            args.duration,
            args.depDate,
            args.depTime,
            args.seats,
            args.price,
            args.chattiness,
            args.smoking,
            args.pets,
            args.music,
            args.description,
            stops
        )

        tvDate.text = trip.depDate
        tvTime.text = trip.depTime
        etDepartureLocation.setText(trip.departure)
        etArrivalLocation.setText(trip.arrival)
        etDuration.setText(trip.duration)
        etSeats.setText(trip.seats.toString())
        etPrice.setText(trip.price.toString())
        etDescription.setText(trip.description)

        val rv = view.findViewById<RecyclerView>(R.id.rv_tripEdit_stops)
        rv.layoutManager = LinearLayoutManager(context);
        val stopAdapter = StopAdapter(trip.stops!!)
        rv.adapter = stopAdapter

    }

    private fun initPreferences(){
        chattiness = changeStatePreference(trip.chattiness, ibtnChattiness)
        smoking = changeStatePreference(trip.smoking, ibtnSmoking)
        pets = changeStatePreference(trip.pets, ibtnPets)
        music = changeStatePreference(trip.music, ibtnMusic)
    }

    private fun changeStatePreference(state: Boolean, btn: ImageButton) : Boolean{
        val typedValue = TypedValue()
        val theme = requireContext().theme
        var color = 0

        if (state) {
            theme.resolveAttribute(R.attr.colorControlActivated, typedValue, true)
            color = typedValue.data
        } else {
            theme.resolveAttribute(R.attr.colorControlNormal, typedValue, true)
            color = typedValue.data //2298478592.toInt()
        }
        btn.isSelected = state
        btn.setColorFilter(color)
        return state
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_trip_edit, menu)
        optionsMenu = menu
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.save_trip -> {
                val action = TripEditFragmentDirections.actionNavTripEditToNavTripDetails(
                    trip.id,
                    etDepartureLocation.text.trim().toString(),
                    etArrivalLocation.text.trim().toString(),
                    etDuration.text.trim().toString(),
                    etPrice.text.trim().toString().toFloat(),
                    etSeats.text.trim().toString().toInt(),
                    tvDate.text.trim().toString(),
                    tvTime.text.trim().toString(),
                    chattiness,
                    smoking,
                    pets,
                    music,
                    etDescription.text.trim().toString(),
                    bundleStops
                )
                findNavController().navigate(action)

                Snackbar.make(requireView(), "Trip saved", Snackbar.LENGTH_SHORT).show()
                true
            }
            else -> item.onNavDestinationSelected(findNavController()) || super.onOptionsItemSelected(
                item
            )
        }
    }

    class DatePickerFragment(tvDate: TextView) : DialogFragment(),
        DatePickerDialog.OnDateSetListener {

        val tvDate = tvDate

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            // Use the current date as the default date in the picker
            val c = Calendar.getInstance()
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)

            // Create a new instance of DatePickerDialog and return it
            return DatePickerDialog(requireContext(), this, year, month, day)
        }

        override fun onDateSet(view: DatePicker, year: Int, month: Int, day: Int) {
            tvDate.text = "${day.toString()}/${month.toString()}/${year.toString()}"
        }

    }

    private fun showDatePickerDialog(v: View) {
        val dateFragment = DatePickerFragment(tvDate)
        dateFragment.show(requireActivity().supportFragmentManager, "datePicker")
        // should wait for fragment to close and then update the textView?
        //tvDate.text = dateFragment.date //TODO: too fast
    }

    class TimePickerFragment(tvTime: TextView) : DialogFragment(),
        TimePickerDialog.OnTimeSetListener {

        val tvTime = tvTime

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

        override fun onTimeSet(view: TimePicker, hourOfDay: Int, minute: Int) {
            tvTime.text = "${hourOfDay.toString()} : ${minute.toString()}"
        }
    }

    private fun showTimePickerDialog(v: View) {
        val timeFragment = TimePickerFragment(tvTime)
        timeFragment.show(requireActivity().supportFragmentManager, "timePicker")
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
        val targetW: Int = 720
        val targetH: Int = 405

        val bmOptions = BitmapFactory.Options().apply {
            // Get the dimensions of the bitmap
            inJustDecodeBounds = true


            BitmapFactory.decodeFile(currentPhotoPath, this)

            val photoW: Int = outWidth
            val photoH: Int = outHeight

            // Determine how much to scale down the image
            val scaleFactor: Int = Math.max(1, Math.min(photoW / targetW, photoH / targetH))

            // Decode the image file into a Bitmap sized to fill the View
            inJustDecodeBounds = false
            inSampleSize = scaleFactor
            inPurgeable = true
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

        var rotatedBitmap: Bitmap? = null
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotatedBitmap = rotateImage(bitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotatedBitmap = rotateImage(bitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotatedBitmap = rotateImage(bitmap, 270f)
            ExifInterface.ORIENTATION_NORMAL -> rotatedBitmap = bitmap
            else -> rotatedBitmap = bitmap
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
        val fileOutputStream = FileOutputStream(myFile)

        rotatedBitmap?.compress(Bitmap.CompressFormat.JPEG, 90, fileOutputStream)
        fileOutputStream.close()
        oldFile.delete()    // delete old img with wrong orientation, maybe is better to add a check if successful?
        currentPhotoPath = myFile.absolutePath  // update the path to point to the new fixed img
    }

    private fun saveProfileImg() {
        val imgPath = activity?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val filename = FILENAME_IMG
        val myFile = File(imgPath, filename)
        val fileOutputStream = FileOutputStream(myFile)

        (ivCarPic.drawable).toBitmap()
            .compress(Bitmap.CompressFormat.JPEG, 90, fileOutputStream)
        fileOutputStream?.close()

        val tmpFile = File(imgPath, TMP_FILENAME_IMG)
        tmpFile.delete()

        currentPhotoPath = (File(imgPath, filename)).absolutePath
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
        }else if (requestCode == REQUEST_IMAGE_FROM_GALLERY && resultCode == Activity.RESULT_OK) {
            val imageUri = data?.data
            updatePathFromGallery(imageUri)
            setPic()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("chattiness", chattiness)
        outState.putBoolean("smoking", smoking)
        outState.putBoolean("pets", pets)
        outState.putBoolean("music", music)
    }

}

class StopEditAdapter(val stops: HashMap<Int, String>) :
    RecyclerView.Adapter<StopEditAdapter.StopEditViewHolder>() {

    class StopEditViewHolder(v: View) : RecyclerView.ViewHolder(v) {

       var stopName: EditText = v.findViewById(R.id.et_stop_location)
        var stopDate: EditText = v.findViewById(R.id.et_stop_date)
        var stopTime: EditText = v.findViewById(R.id.et_stop_time)

        fun bind(stop: String?) {
            Log.d("bind:", stop!!)
            val stringArray = stop!!.split(",")
            stopName.setText(stringArray[0].trim())
            stopDate.setText(stringArray[1].trim())
            stopTime.setText(stringArray[2].trim())
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StopEditViewHolder {
        val layout =
            LayoutInflater.from(parent.context).inflate(R.layout.stop_edit_layout, parent, false)
        return StopEditViewHolder(layout)
    }

    override fun onBindViewHolder(holder: StopEditViewHolder, key: Int) {
        holder.bind(stops[key])
    }

    override fun getItemCount(): Int {
        return stops.size
    }

}