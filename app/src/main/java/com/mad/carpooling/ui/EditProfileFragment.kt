package com.mad.carpooling.ui

import android.app.Activity.RESULT_OK
import android.content.ActivityNotFoundException
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
import android.view.*
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.onNavDestinationSelected
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.mad.carpooling.R
import com.mad.carpooling.model.User
import com.mad.carpooling.repository.TripRepository
import com.mad.carpooling.repository.UserRepository
import com.mad.carpooling.viewmodel.EditProfileViewModel
import com.mad.carpooling.viewmodel.EditProfileViewModelFactory
import com.mad.carpooling.viewmodel.SharedViewModel
import com.mad.carpooling.viewmodel.SharedViewModelFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*


class EditProfileFragment : Fragment(R.layout.fragment_edit_profile) {
    private lateinit var etFullName: EditText
    private lateinit var etNickname: EditText
    private lateinit var etEmail: EditText
    private lateinit var etLocation: EditText
    private lateinit var ivEditProfilePic: ImageView
    private lateinit var optionsMenu: Menu
    private lateinit var user: User
    private var currentPhotoPath: String? = null
    private val viewModel: EditProfileViewModel by viewModels {
        EditProfileViewModelFactory(
            UserRepository()
        )
    }
    private val sharedViewModel: SharedViewModel by activityViewModels {
        SharedViewModelFactory(
            TripRepository(),
            UserRepository()
        )
    }
    private var REQUEST_IMAGE_CAPTURE = 1
    private var REQUEST_IMAGE_FROM_GALLERY = 2
    private var TMP_FILENAME_IMG = "temp_profile_pic_img.jpg"
    private var FILENAME_IMG = "profile_pic_img.jpg"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
    }

    @ExperimentalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etFullName = view.findViewById<EditText>(R.id.et_fullName)
        etNickname = view.findViewById<EditText>(R.id.et_nickname)
        etEmail = view.findViewById<EditText>(R.id.et_email)
        etLocation = view.findViewById<EditText>(R.id.et_location)
        ivEditProfilePic = view.findViewById<ImageView>(R.id.et_profile_pic)

        initProfile(sharedViewModel.getCurrentUser().value!!, viewModel, savedInstanceState)

        val btnCamera = view.findViewById<ImageButton>(R.id.btn_camera)
        registerForContextMenu(btnCamera)
        btnCamera.setOnClickListener { activity?.openContextMenu(btnCamera) }
    }

    private fun initProfile(
        currentUser: User,
        viewModel: EditProfileViewModel,
        savedInstanceState: Bundle?
    ) {

        if (savedInstanceState == null) {
            viewModel.setUser(currentUser)
        } else {
            currentPhotoPath = savedInstanceState.getString("state_currentPhotoPath")
        }

        user = viewModel.getUser()

        if (currentPhotoPath != null) {
            BitmapFactory.decodeFile(currentPhotoPath)?.also { bitmap ->
                ivEditProfilePic.setImageBitmap(bitmap)
            }
        } else {
            Glide.with(requireContext()).load(user.imageUserRef).into(ivEditProfilePic)
        }

        etFullName.setText(user.fullname)
        etNickname.setText(user.nickname)
        etEmail.setText(user.email)
        etLocation.setText(user.location)

    }

    override fun onSaveInstanceState(outState: Bundle) {
        viewModel.setUser(user)
        outState.putString("state_currentPhotoPath", currentPhotoPath)
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
        val targetW = 512
        val targetH = 512

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

            ivEditProfilePic.setImageBitmap(validateBitmapOrientation(bitmap))
        }
    }

    private fun validateBitmapOrientation(bitmap: Bitmap): Bitmap? {
        val ei = currentPhotoPath?.let { ExifInterface(it) }
        val orientation: Int? = ei?.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED
        )

        var rotatedBitmap: Bitmap?
        rotatedBitmap = when (orientation) {
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

    private fun saveProfileImg() {
        val imgPath = activity?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val uniqueString = user.uid + Timestamp.now()
        val filename = MessageDigest.getInstance("SHA256").digest(uniqueString.toByteArray())
            .toString() + ".jpg"
        val myFile = File(imgPath, filename)
        val file = Uri.fromFile(myFile)
        val fileOutputStream = FileOutputStream(myFile)
        val userRef = Firebase.storage.reference.child("images_user/${file.lastPathSegment}")

        (ivEditProfilePic.drawable).toBitmap()
            .compress(Bitmap.CompressFormat.JPEG, 90, fileOutputStream)
        fileOutputStream.close()

        userRef.putFile(file).addOnFailureListener {

        }.addOnSuccessListener {
            myFile.delete()
            currentPhotoPath = file.lastPathSegment
            val tmpFile = File(imgPath, TMP_FILENAME_IMG)
            tmpFile.delete()
            userRef.downloadUrl.addOnCompleteListener {
                user.imageUserRef = it.result.toString()
                updateFirestoreUser()
            }
        }

    }

    private fun validateProfileForm() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                optionsMenu.findItem(R.id.nav_show_profile).isEnabled =
                    etFullName.text.trim().isNotEmpty() && etNickname.text.trim()
                        .isNotEmpty() && etEmail.text.trim()
                        .isNotEmpty() && etLocation.text.trim().isNotEmpty()
            }

            override fun afterTextChanged(s: Editable?) {
            }

        }

        etFullName.addTextChangedListener(textWatcher)
        etNickname.addTextChangedListener(textWatcher)
        etEmail.addTextChangedListener(textWatcher)
        etLocation.addTextChangedListener(textWatcher)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_edit_profile, menu)
        optionsMenu = menu
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        validateProfileForm()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.nav_show_profile -> { //save
                if (currentPhotoPath != null) {
                    saveProfileImg()
                } else {
                    updateFirestoreUser()
                }
                true
            }
            else -> item.onNavDestinationSelected(findNavController()) || super.onOptionsItemSelected(
                item
            )
        }
    }

    private fun updateFirestoreUser() {
        user.fullname = etFullName.text.trim().toString()
        user.nickname = etNickname.text.trim().toString()
        user.email = etEmail.text.trim().toString()
        user.location = etLocation.text.trim().toString()

        viewModel.updateUser(user).observe(viewLifecycleOwner, Observer{ success ->
            findNavController().navigate(EditProfileFragmentDirections.actionNavEditProfileToNavShowProfile())
            if(success)
                Toast.makeText(requireContext(), "Profile saved!", Toast.LENGTH_SHORT).show()

            else
                Toast.makeText(requireContext(), "Error while saving your profile! Try again", Toast.LENGTH_SHORT).show()

        })
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
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            setPic()
        } else if (requestCode == REQUEST_IMAGE_FROM_GALLERY && resultCode == RESULT_OK) {
            val imageUri = data?.data
            updatePathFromGallery(imageUri)
            setPic()
        }
    }

}