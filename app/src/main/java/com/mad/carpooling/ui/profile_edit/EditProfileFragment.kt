package com.mad.carpooling.ui.profile_edit

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.onNavDestinationSelected
import com.google.android.material.snackbar.Snackbar
import com.mad.carpooling.R
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class EditProfileFragment : Fragment(R.layout.fragment_edit_profile) {
    private lateinit var etFullName: EditText
    private lateinit var etNickname: EditText
    private lateinit var etEmail: EditText
    private lateinit var etLocation: EditText
    private lateinit var ivEditProfilePic: ImageView
    private lateinit var optionsMenu: Menu
    private var currentPhotoPath: String? = null
    private var REQUEST_IMAGE_CAPTURE = 1
    private var TMP_FILENAME_IMG = "temp_profile_pic_img.jpg"
    private var FILENAME_IMG = "profile_pic_img.jpg"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)

        val callback = requireActivity().onBackPressedDispatcher.addCallback(this) {
            activity?.setResult(Activity.RESULT_CANCELED)
            findNavController().navigate(
                R.id.action_nav_edit_profile_to_nav_show_profile
            )
        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etFullName = view.findViewById<EditText>(R.id.et_fullName)
        etNickname = view.findViewById<EditText>(R.id.et_nickname)
        etEmail = view.findViewById<EditText>(R.id.et_email)
        etLocation = view.findViewById<EditText>(R.id.et_location)
        ivEditProfilePic = view.findViewById<ImageView>(R.id.et_profile_pic)

        initProfile(savedInstanceState)

        val btnCamera = view.findViewById<ImageButton>(R.id.btn_camera)
        registerForContextMenu(btnCamera)
        btnCamera.setOnClickListener { activity?.openContextMenu(btnCamera) }
    }

    private fun initProfile(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {    // view created navigating from ShowProfileFragment
            val args: EditProfileFragmentArgs by navArgs()
            etFullName.setText(args.fullname)
            etNickname.setText(args.nickname)
            etEmail.setText(args.email)
            etLocation.setText(args.location)
        } else {
            currentPhotoPath = savedInstanceState.getString("state_currentPhoto")
        }
        if (currentPhotoPath != null) {
            BitmapFactory.decodeFile(currentPhotoPath)?.also { bitmap ->
                ivEditProfilePic.setImageBitmap(bitmap)
            }
        } else {
            val sharedPref =
                context?.getSharedPreferences("profile_pref.group05.lab1", Context.MODE_PRIVATE)
                    ?: return
            val jsonString = sharedPref.getString(getString(R.string.saved_profile_data), null)
            if (jsonString != null) {
                val jsonObject = JSONObject(jsonString)
                BitmapFactory.decodeFile(
                    jsonObject.getString(
                        "json_profilePic.group05.lab1"
                    )
                )?.also { bitmap ->
                    ivEditProfilePic.setImageBitmap(bitmap)
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
        val targetW: Int = 512
        val targetH: Int = 512

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

            ivEditProfilePic.setImageBitmap(validateBitmapOrientation(bitmap))
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

        (ivEditProfilePic.drawable).toBitmap()
            .compress(Bitmap.CompressFormat.JPEG, 90, fileOutputStream)
        fileOutputStream?.close()

        val tmpFile = File(imgPath, TMP_FILENAME_IMG)
        tmpFile.delete()

        currentPhotoPath = (File(imgPath, filename)).absolutePath
    }

    private fun saveToSharedPref() {
        saveProfileImg()

        val jsonObj = JSONObject()
        jsonObj.put("json_fullName.group05.lab1", etFullName.text.trim().toString())
        jsonObj.put("json_nickname.group05.lab1", etNickname.text.trim().toString())
        jsonObj.put("json_email.group05.lab1", etEmail.text.trim().toString())
        jsonObj.put("json_location.group05.lab1", etLocation.text.trim().toString())
        jsonObj.put("json_profilePic.group05.lab1", currentPhotoPath)

        val sharedPref =
            context?.getSharedPreferences("profile_pref.group05.lab1", Context.MODE_PRIVATE)
                ?: return
        with(sharedPref.edit()) {
            putString(getString(R.string.saved_profile_data), jsonObj.toString())
            apply()
        }
    }

    private fun checkFullName() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                optionsMenu.findItem(R.id.nav_show_profile).isEnabled =
                    etFullName.text.trim().length > 0
            }

            override fun afterTextChanged(s: Editable?) {
            }

        }

        etFullName.addTextChangedListener(textWatcher)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_edit_profile, menu)
        optionsMenu = menu
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        checkFullName()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.nav_show_profile -> { //save
                saveToSharedPref()

                val action = EditProfileFragmentDirections.actionNavEditProfileToNavShowProfile(
                    etFullName.text.trim().toString(),
                    etNickname.text.trim().toString(),
                    etEmail.text.trim().toString(),
                    etLocation.text.trim().toString(),
                    currentPhotoPath
                )
                findNavController().navigate(action)

                Snackbar.make(requireView(), "Profile saved", Snackbar.LENGTH_SHORT).show()
                true
            }
            else -> item.onNavDestinationSelected(findNavController()) || super.onOptionsItemSelected(
                item
            )
        }

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
                Toast.makeText(context, "Open Gallery", Toast.LENGTH_SHORT).show()
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
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            setPic()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (currentPhotoPath != null) outState.putString("state_currentPhoto", currentPhotoPath)
    }

}