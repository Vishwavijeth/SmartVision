package com.example.application.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.application.R


class Settings : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        val view =  inflater.inflate(R.layout.fragment_settings, container, false)
        val modelbtn: CardView = view.findViewById(R.id.model)
        val backbutton: ImageView = view.findViewById(R.id.settingsBackButton)

        modelbtn.setOnClickListener{
            ModelBtnAction()
        }

        backbutton.setOnClickListener {
            NavigateToHomepage()
        }

        return view
    }

    private fun ModelBtnAction()
    {
        findNavController().navigate(R.id.action_settings_to_uploadModelFragment)
    }

    private fun NavigateToHomepage()
    {
        findNavController().popBackStack()
    }

    companion object
    {
        const val TAG = "Settings"
    }

}