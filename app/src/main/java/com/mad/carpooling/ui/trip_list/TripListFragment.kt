package com.mad.carpooling.ui.trip_list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.mad.carpooling.R

class TripListFragment : Fragment() {

    private lateinit var tripListViewModel: TripListViewModel

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        tripListViewModel =
                ViewModelProvider(this).get(TripListViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_trip_list, container, false)
        /*val textView: TextView = root.findViewById(R.id.text_home)
        tripListViewModel.text.observe(viewLifecycleOwner, Observer {
            textView.text = it
        })*/
        return root
    }
}