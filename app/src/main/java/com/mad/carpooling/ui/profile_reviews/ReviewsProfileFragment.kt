package com.mad.carpooling.ui.profile_reviews

import android.app.ActionBar
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.mad.carpooling.R
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mad.carpooling.MainActivity
import com.mad.carpooling.ui.SharedViewModel

class ReviewsProfileFragment : Fragment(R.layout.fragment_reviews_profile) {
    private lateinit var rv: RecyclerView
    private val model: SharedViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val role = arguments?.getString("role")
        (requireActivity() as AppCompatActivity).supportActionBar?.title = "Reviews as ${role}"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initReviewsList(view)
    }

    private fun initReviewsList(view: View) {
        val emptyView = view.findViewById<TextView>(R.id.no_reviews_available)
        val db = Firebase.firestore
        val uid = arguments?.getString("uid")
        val role = arguments?.getString("role")
        rv = view.findViewById<RecyclerView>(R.id.reviews_list)
        rv.layoutManager = LinearLayoutManager(context)
        rv.isNestedScrollingEnabled = false; //prevent toolbar to expand on scroll
        lateinit var userList: Array<String>
        if (role == "driver") {
            db.collection("ratings").document(uid!!).get()
                .addOnSuccessListener { res ->
                    val mapRatingDriver: Map<String, ArrayList<Any>> =
                        res.get("driverRatings") as Map<String, ArrayList<Any>>
                    userList = mapRatingDriver.keys.toTypedArray()
                    Log.d("keys",userList[0].toString())
                    val reviewAdapter = ReviewAdapter(userList, mapRatingDriver)
                    rv.adapter = reviewAdapter
                    if (reviewAdapter.itemCount == 0) //from getItemCount
                        emptyView.isVisible = true
                }
        } else {
            db.collection("ratings").document(uid!!).get()
                .addOnSuccessListener { res ->
                    val mapRatingPassenger: Map<String, ArrayList<Any>> =
                        res.get("passengerRatings") as Map<String, ArrayList<Any>>
                    userList = mapRatingPassenger.keys.toTypedArray()
                    val reviewAdapter = ReviewAdapter(userList, mapRatingPassenger)
                    rv.adapter = reviewAdapter
                    if (reviewAdapter.itemCount == 0) //from getItemCount
                        emptyView.isVisible = true
                }
        }

        val fab = (activity as MainActivity).findViewById<FloatingActionButton>(R.id.fab)
        fab.hide()
    }

    class ReviewAdapter(
        private val userList: Array<String>,
        private val map: Map<String, ArrayList<Any>>
    ) :
        RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {

        class ReviewViewHolder(v: View) : RecyclerView.ViewHolder(v) {

            val ivProfile = v.findViewById<ImageView>(R.id.iv_ratings_profile_pic)
            val tvNickname = v.findViewById<TextView>(R.id.tv_ratings_nickname)
            val rb = v.findViewById<RatingBar>(R.id.rb_ratings)
            val comment = v.findViewById<TextView>(R.id.tv_comment)
            val db = Firebase.firestore

            fun bind(userId: String, userReview: ArrayList<Any>, holder: ReviewViewHolder) {
                db.collection("users").document(userId).get().addOnSuccessListener {
                    tvNickname.text = it.get("nickname").toString()
                    Glide.with(holder.itemView).load(it?.get("imageUserRef"))
                        .into(ivProfile)
                }
                rb.rating = userReview[0].toString().toFloat()
                comment.text = userReview[1].toString()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
            val layout =
                LayoutInflater.from(parent.context).inflate(R.layout.ratings_layout, parent, false)
            return ReviewViewHolder(layout)
        }

        override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
            val userId = userList[position]
            val userReview = map[userId]
            holder.bind(userId, userReview!!, holder)
        }

        override fun getItemCount(): Int {
            return userList.size
        }

    }

}
