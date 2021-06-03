package com.mad.carpooling.ui

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.onNavDestinationSelected
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mad.carpooling.R
import com.mad.carpooling.model.User
import com.mad.carpooling.repository.TripRepository
import com.mad.carpooling.repository.UserRepository
import com.mad.carpooling.viewmodel.SharedViewModel
import com.mad.carpooling.viewmodel.SharedViewModelFactory


class ShowProfileFragment : Fragment(R.layout.fragment_show_profile) {
    private lateinit var tvFullName: TextView
    private lateinit var tvNickname: TextView
    private lateinit var llEmail: LinearLayout
    private lateinit var tvEmail: TextView
    private lateinit var tvLocation: TextView
    private lateinit var ivProfilePic: ImageView
    private lateinit var rbDriver: RatingBar
    private lateinit var numStarsDriver: TextView
    private lateinit var numReviewsDriver: TextView
    private lateinit var rbPassenger: RatingBar
    private lateinit var numStarsPassenger: TextView
    private lateinit var numReviewsPassenger: TextView
    private lateinit var user: User
    private var uid: String = ""
    private lateinit var cvDriver: MaterialCardView
    private lateinit var cvPassenger: MaterialCardView
    private lateinit var optionsMenu: Menu
    private val sharedViewModel: SharedViewModel by activityViewModels {
        SharedViewModelFactory(
            TripRepository(),
            UserRepository()
        )
    }
    private val args: ShowProfileFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        tvFullName = view.findViewById(R.id.tv_fullName)
        tvNickname = view.findViewById(R.id.tv_nickname)
        llEmail = view.findViewById(R.id.title_email)
        tvEmail = view.findViewById(R.id.tv_email)
        tvLocation = view.findViewById(R.id.tv_location)
        ivProfilePic = view.findViewById(R.id.iv_profile_pic)
        rbDriver = view.findViewById(R.id.rb_showProfile_driver)
        numStarsDriver = view.findViewById(R.id.tv_showProfile_numStars_driver)
        numReviewsDriver = view.findViewById(R.id.tv_showProfile_numReviews_driver)
        rbPassenger = view.findViewById(R.id.rb_showProfile_passenger)
        numStarsPassenger = view.findViewById(R.id.tv_showProfile_numStars_passenger)
        numReviewsPassenger = view.findViewById(R.id.tv_showProfile_numReviews_passenger)
        cvDriver = view.findViewById(R.id.ll_rating_driver)
        cvPassenger = view.findViewById(R.id.ll_rating_passenger)

        if (args.uid == "uid") {
            sharedViewModel.getCurrentUser()
                .observe(viewLifecycleOwner, Observer { currentUser ->
                    // Update the UI
                    if (currentUser != null) {
                        uid = currentUser.uid
                        initProfile(currentUser, view)
                    }
                })
        } else {
            sharedViewModel.getUserDoc(args.uid).observe(viewLifecycleOwner, Observer { user ->
                if (user != null) {
                    uid = user.uid
                    initProfile(user, view)
                }
            })
        }
    }

    private fun initProfile(user: User, view: View) {
        this.user = user
        tvFullName.text = user.fullname
        tvNickname.text = user.nickname
        tvEmail.text = user.email
        tvLocation.text = user.location
        Glide.with(view).load(user.imageUserRef).into(ivProfilePic)
        llEmail.isVisible = uid == sharedViewModel.getCurrentUser().value?.uid
        val div = view.findViewById<View>(R.id.divider4)
        div.isVisible = uid == sharedViewModel.getCurrentUser().value?.uid

        sharedViewModel.getRatings(user.uid,"driverRatings").observe(viewLifecycleOwner, Observer { map ->
            if (map != null) {
                if(map.isNotEmpty()){
                    var vote: Float = 0f
                    for (array in map.values)
                        vote += array[0].toString().toFloat()
                    Log.d("ratings:", vote.toString())
                    rbDriver.rating = (vote) / (map.size.toFloat())
                    Log.d("ratings:", rbDriver.rating.toString())
                    numStarsDriver.text = "${("%.1f".format(rbDriver.rating))}/5"
                    numReviewsDriver.text = "${map.size} reviews"
                }
                else {
                    rbDriver.rating = 0f
                    numStarsDriver.text = "-/5"
                    numReviewsDriver.text = "0 reviews"
                }
            } else {
                rbDriver.rating = 0f
                numStarsDriver.text = "-/5"
                numReviewsDriver.text = "0 reviews"
            }
        })

        sharedViewModel.getRatings(user.uid,"passengerRatings").observe(viewLifecycleOwner, Observer { map ->
            if (map != null) {
                if(map.isNotEmpty()){
                    var vote: Float = 0f
                    for (array in map.values)
                        vote += array[0].toString().toFloat()
                    rbPassenger.rating = (vote) / (map.size.toFloat())
                    numStarsPassenger.text = "${("%.1f".format(rbPassenger.rating))}/5"
                    numReviewsPassenger.text = "${map.size} reviews"
                }
                else {
                    rbPassenger.rating = 0f
                    numStarsPassenger.text = "-/5"
                    numReviewsPassenger.text = "0 reviews"
                }
            } else {
                rbPassenger.rating = 0f
                numStarsPassenger.text = "-/5"
                numReviewsPassenger.text = "0 reviews"
            }
        })

        cvDriver.setOnClickListener {
            val bundle: Bundle = bundleOf()
            bundle.putString("uid", uid)
            bundle.putString("role", "driver")

            if (uid != sharedViewModel.getCurrentUser().value?.uid!!) {
                findNavController().navigate(
                    R.id.action_nav_show_profile_others_to_nav_reviews_profile,
                    bundle
                )
            } else {
                findNavController().navigate(
                    R.id.action_nav_show_profile_to_nav_reviews_profile,
                    bundle
                )
            }
        }

        cvPassenger.setOnClickListener {
            val bundle: Bundle = bundleOf()
            bundle.putString("uid", uid)
            bundle.putString("role", "passenger")

            if (uid != sharedViewModel.getCurrentUser().value?.uid!!) {
                findNavController().navigate(
                    R.id.action_nav_show_profile_others_to_nav_reviews_profile,
                    bundle
                )
            } else {
                findNavController().navigate(
                    R.id.action_nav_show_profile_to_nav_reviews_profile,
                    bundle
                )
            }
        }
    }

    private fun editProfile() {
        findNavController().navigate(R.id.action_nav_show_profile_to_nav_edit_profile)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.nav_edit_profile -> {
                editProfile()
                true
            }
            else -> item.onNavDestinationSelected(findNavController()) || super.onOptionsItemSelected(
                item
            )
        }

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_profile, menu)
        optionsMenu = menu
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        optionsMenu.findItem(R.id.nav_edit_profile).isVisible =
            uid == sharedViewModel.getCurrentUser().value?.uid
    }

}