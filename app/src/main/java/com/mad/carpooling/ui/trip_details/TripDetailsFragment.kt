package com.mad.carpooling.ui.trip_details

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.Image
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.card.MaterialCardView
import com.mad.carpooling.R
import org.json.JSONObject

class TripDetailsFragment : Fragment(R.layout.fragment_trip_details) {

    private lateinit var tripDetailsViewModel: TripDetailsViewModel

    private lateinit var tvDepartureLocation: TextView
    private lateinit var tvDepartureDate: TextView
    private lateinit var tvDepartureTime: TextView
    private lateinit var tvArrivalLocation: TextView
    private lateinit var tvDuration: TextView
    private lateinit var tvSeats: TextView
    private lateinit var tvPrice: TextView
    private lateinit var tvDescription: TextView
    private lateinit var mcvChattiness: MaterialCardView
    private lateinit var mcvSmoking: MaterialCardView
    private lateinit var mcvPets: MaterialCardView
    private lateinit var mcvMusic: MaterialCardView
    private lateinit var ivChattiness: ImageView
    private lateinit var ivSmoking: ImageView
    private lateinit var ivPets: ImageView
    private lateinit var ivMusic: ImageView
    var chattiness = false
    var smoking = false
    var pets = false
    var music = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        tvDepartureLocation = view.findViewById(R.id.tv_tripDetails_departureLocation)
        tvDepartureDate = view.findViewById(R.id.tv_tripDetails_departureDate)
        tvDepartureTime = view.findViewById(R.id.tv_tripDetails_departureTime)
        tvArrivalLocation = view.findViewById(R.id.tv_tripDetails_arrivalLocation)
        tvDuration = view.findViewById(R.id.tv_tripDetails_duration)
        tvSeats = view.findViewById(R.id.tv_tripDetails_seats)
        tvPrice = view.findViewById(R.id.tv_tripDetails_price)
        tvDescription = view.findViewById(R.id.tv_tripDetails_description_text)
        mcvChattiness = view.findViewById(R.id.mcv_tripDetails_chattiness)
        mcvSmoking = view.findViewById(R.id.mcv_tripDetails_smoking)
        mcvPets = view.findViewById(R.id.mcv_tripDetails_pets)
        mcvMusic = view.findViewById(R.id.mcv_tripDetails_music)
        ivChattiness = view.findViewById(R.id.iv_tripDetails_chattiness)
        ivSmoking = view.findViewById(R.id.iv_tripDetails_smoking)
        ivPets = view.findViewById(R.id.iv_tripDetails_pets)
        ivMusic = view.findViewById(R.id.iv_tripDetails_music)

        val btn_profile = view.findViewById<Button>(R.id.btn_tripDetails_showProfile)
        btn_profile.setOnClickListener {
            // TODO: pass correct information about profile
            findNavController().navigate(R.id.action_nav_trip_details_to_nav_show_profile)
        }
    }

    private fun editTrip() {

        // TODO: enable button only if matching profile

        val bundle = Bundle()

        bundle.putString("departureLocation.group05.lab2", tvDepartureLocation.text.toString())
        bundle.putString("departureDate.group05.lab2", tvDepartureDate.text.toString())
        bundle.putString("departureTime.group05.lab2", tvDepartureTime.text.toString())
        bundle.putString("arrivalLocation.group05.lab2", tvArrivalLocation.text.toString())
        bundle.putString("duration.group05.lab2", tvDuration.text.toString())
        bundle.putString("seats.group05.lab2", tvSeats.text.toString())
        bundle.putString("price.group05.lab2", tvPrice.text.toString())
        bundle.putString("description.group05.lab2", tvDescription.text.toString())

        // TODO: use ColorStateList and substitute R.color.blue_700 with ?attr/colorPrimary
        if (mcvChattiness.strokeColor == R.color.blue_700) chattiness = true
        if (mcvSmoking.strokeColor == R.color.blue_700) smoking = true
        if (mcvPets.strokeColor == R.color.blue_700) pets = true
        if (mcvMusic.strokeColor == R.color.blue_700) music = true

        bundle.putBoolean("chattiness.group05.lab2", chattiness)
        bundle.putBoolean("smoking.group05.lab2", smoking)
        bundle.putBoolean("pets.group05.lab2", pets)
        bundle.putBoolean("music.group05.lab2", music)

        findNavController().navigate(R.id.action_nav_trip_details_to_nav_trip_edit, bundle)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
//            val sharedPref =
//                context?.getSharedPreferences("profile_pref.group05.lab1", Context.MODE_PRIVATE) ?: return
//            val jsonString = sharedPref.getString(getString(R.string.saved_profile_data), null)
//            if (jsonString != null) {
//                val jsonObject = JSONObject(jsonString)

            tvDepartureLocation.text = (data?.getStringExtra("save_departureLocation.group05.lab2"))
            tvDepartureDate.text = (data?.getStringExtra("save_departureDate.group05.lab2"))
            tvDepartureTime.text = (data?.getStringExtra("save_departureTime.group05.lab2"))
            tvArrivalLocation.text = (data?.getStringExtra("save_arrivalLocation.group05.lab2"))
            tvDuration.text = (data?.getStringExtra("save_duration.group05.lab2"))
            tvSeats.text = (data?.getStringExtra("save_seats.group05.lab2"))
            tvPrice.text = (data?.getStringExtra("save_price.group05.lab2"))
            tvDescription.text = (data?.getStringExtra("save_description.group05.lab2"))

            chattiness = (data!!.getBooleanExtra("save_chattiness.group05.lab2", false))
            changeStatePreference(chattiness, mcvChattiness, ivChattiness)

            smoking = (data!!.getBooleanExtra("save_smoking.group05.lab2", false))
            changeStatePreference(smoking, mcvSmoking, ivSmoking)

            pets = (data!!.getBooleanExtra("save_pets.group05.lab2", false))
            changeStatePreference(pets, mcvPets, ivPets)

            music = (data!!.getBooleanExtra("save_music.group05.lab2", false))
            changeStatePreference(music, mcvMusic, ivMusic)

//                ivProfilePic.setImageBitmap(
//                    BitmapFactory.decodeStream(
//                        activity?.openFileInput(
//                            jsonObject.getString(
//                                "json_profilePic.group05.lab1"
//                            )
//                        )
//                    )
//                )
//            }

        } /*else if (requestCode == 1 && resultCode == Activity.RESULT_CANCELED) { //debug
            Toast.makeText(this, "RESULT_CANCELED", Toast.LENGTH_SHORT).show()
        }*/
    }

    private fun changeStatePreference(state: Boolean, mcv: MaterialCardView, iv: ImageView): Boolean {

        // TODO: substitute R.color.blue_700 with ?attr/colorPrimary

        if (state) {
            mcv.strokeColor = ContextCompat.getColor(requireContext(), R.color.blue_700)
            iv.setColorFilter(ContextCompat.getColor(requireContext(), R.color.blue_700))
        } else {
            mcv.strokeColor = ContextCompat.getColor(requireContext(), R.color.pref_disabled)
            iv.setColorFilter(ContextCompat.getColor(requireContext(), R.color.pref_disabled))
        }
        return !state
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.edit_trip -> {
                editTrip()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_trip_details, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }
}