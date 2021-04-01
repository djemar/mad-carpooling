package com.mad.carpooling

import android.R.attr.bitmap
import android.app.Activity
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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import org.json.JSONObject
import java.io.File
import java.io.IOException


class EditProfileActivity : AppCompatActivity() {
    private lateinit var etFullName: EditText
    private lateinit var etNickname: EditText
    private lateinit var etEmail: EditText
    private lateinit var etLocation: EditText
    private lateinit var ivEditProfilePic: ImageView
    private lateinit var optionsMenu: Menu
    private var currentPhotoPath: String? = null
    private var REQUEST_IMAGE_CAPTURE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        etFullName = findViewById<EditText>(R.id.et_fullName)
        etNickname = findViewById<EditText>(R.id.et_nickname)
        etEmail = findViewById<EditText>(R.id.et_email)
        etLocation = findViewById<EditText>(R.id.et_location)
        ivEditProfilePic = findViewById<ImageView>(R.id.et_profile_pic)

        //savedInstanceState check?
        initProfile()


        val btnCamera = findViewById<ImageButton>(R.id.btn_camera)
        registerForContextMenu(btnCamera)
        btnCamera.setOnClickListener { openContextMenu(btnCamera) }
    }

    private fun initProfile() {
        etFullName.setText(intent.getStringExtra("fullName"))
        etNickname.setText(intent.getStringExtra("nickname"))
        etEmail.setText(intent.getStringExtra("email"))
        etLocation.setText(intent.getStringExtra("location"))

        if (currentPhotoPath != null) {
            BitmapFactory.decodeFile(currentPhotoPath)?.also { bitmap ->
                ivEditProfilePic.setImageBitmap(bitmap)
            }
        } else {
            val sharedPref =
                this.getSharedPreferences("profile_pref", Context.MODE_PRIVATE) ?: return
            val jsonString = sharedPref.getString(getString(R.string.saved_profile_data), null)
            if (jsonString != null) {
                val jsonObject = JSONObject(jsonString)
                ivEditProfilePic.setImageBitmap(
                    BitmapFactory.decodeStream(
                        openFileInput(
                            jsonObject.getString(
                                "json_profilePic"
                            )
                        )
                    )
                )
            }
        }
    }

    private fun checkFullName() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (optionsMenu != null) {
                    optionsMenu.findItem(R.id.save).isEnabled = etFullName.text.length > 0
                }
            }

            override fun afterTextChanged(s: Editable?) {
            }

        }

        etFullName.addTextChangedListener(textWatcher)
    }

    override fun onCreateContextMenu(
        menu: ContextMenu, v: View,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.ctx_menu_camera, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.ctx_gallery -> {
                Toast.makeText(this, "Open Gallery", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.ctx_camera -> {
                dispatchTakePictureIntent()
                true
            }
            else -> super.onContextItemSelected(item)
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
                        this,
                        "com.example.android.fileprovider",
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

            ivEditProfilePic.setImageBitmap(rotatedBitmap)
        }
    }

    fun rotateImage(source: Bitmap, angle: Float): Bitmap? {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(
            source, 0, 0, source.width, source.height,
            matrix, true
        )
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.save -> {
                setResult(Activity.RESULT_OK, Intent().also {
                    it.putExtra("save_fullName", etFullName.text.toString())
                    it.putExtra("save_nickname", etNickname.text.toString())
                    it.putExtra("save_email", etEmail.text.toString())
                    it.putExtra("save_location", etLocation.text.toString())
                    if (currentPhotoPath != null) {
                        it.putExtra("save_profilePic", currentPhotoPath)
                    }
                })

                saveToSharedPref()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    }

    private fun saveToSharedPref() {
        val filename = "profile_pic_img"
        val fileContents = (ivEditProfilePic.drawable).toBitmap()
        val fileOutputStream = openFileOutput(filename, Context.MODE_PRIVATE)
        fileContents.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
        fileOutputStream.close()

        val jsonObj = JSONObject()
        jsonObj.put("json_fullName", etFullName.text.toString())
        jsonObj.put("json_nickname", etNickname.text.toString())
        jsonObj.put("json_email", etEmail.text.toString())
        jsonObj.put("json_location", etLocation.text.toString())
        jsonObj.put("json_profilePic", filename)

        val sharedPref = this.getSharedPreferences("profile_pref", Context.MODE_PRIVATE) ?: return
        with(sharedPref.edit()) {
            putString(getString(R.string.saved_profile_data), jsonObj.toString())
            apply()
        }
        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)

        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_edit_profile, menu)
        if (menu != null) {
            optionsMenu = menu
        }
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        checkFullName()
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            setPic()
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        //val timeStamp: String = SimpleDateFormat("new-photo", Locale.ITALY).format(Date())
        val timeStamp: String = "new-photo"
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("state_fullName", etFullName.text.toString())
        outState.putString("state_nickname", etNickname.text.toString())
        outState.putString("state_email", etEmail.text.toString())
        outState.putString("state_location", etLocation.text.toString())
        if (currentPhotoPath != null) {
            outState.putString("state_profilePic", currentPhotoPath)
        }

    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        etFullName.setText(savedInstanceState.getString("state_fullName"))
        etNickname.setText(savedInstanceState.getString("state_nickname"))
        etEmail.setText(savedInstanceState.getString("state_email"))
        etLocation.setText(savedInstanceState.getString("state_location"))
        currentPhotoPath = savedInstanceState.getString("state_profilePic")
        if (currentPhotoPath != null) {
            BitmapFactory.decodeFile(currentPhotoPath)?.also { bitmap ->
                ivEditProfilePic.setImageBitmap(bitmap)
            }
        }

    }

}