package com.example.application.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.application.R

class HomeFragment : Fragment()
{
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        val card1: CardView = view.findViewById(R.id.card1)
        card1.setBackgroundResource(R.drawable.gradient1)

        val card2: CardView = view.findViewById(R.id.card2)
        card2.setBackgroundResource(R.drawable.gradient2)

        val card3: CardView = view.findViewById(R.id.card3)
        card3.setBackgroundResource(R.drawable.gradient3)

        val card4: CardView = view.findViewById(R.id.card4)
        card4.setBackgroundResource(R.drawable.gradient4)

        val setButton: ImageView = view.findViewById(R.id.settings)

        card1.setOnClickListener{
            Navigate()
        }

        card2.setOnClickListener{
            Navigate_PE()
        }

        card3.setOnClickListener{
            Navigate_IS()
        }

        card4.setOnClickListener{
            Navigate_OCR()
        }

        setButton.setOnClickListener {
            findNavController().navigate(R.id.action_fragment_home_to_settings)
        }

        return view
    }

    private fun Navigate()
    {
        findNavController().navigate(R.id.action_fragment_home_to_camera_fragment)
        Log.d(TAG, "Navigated to Object detection")

        try
        {
            val cacheDirectory = requireContext().cacheDir
            val cacheFiles = cacheDirectory.listFiles()
            cacheFiles?.forEach { file ->
                Log.d(ModelTypeFragment.TAG, "Cache model file name: ${file.name}")
                //Log.d(ModelTypeFragment.TAG, "Custom model file path: ${file.absolutePath}")
            }
        }catch (e: Exception)
        {
            Log.d(TAG, "Cache model file name error: $e" )
        }

    }

    private fun Navigate_PE()
    {
        findNavController().navigate(R.id.action_fragment_home_to_cameraPE_fragment)
    }

    private fun Navigate_IC()
    {
        findNavController().navigate(R.id.action_fragment_home_to_cameraIC_fragment)
    }

    private fun Navigate_IS()
    {
        findNavController().navigate(R.id.action_fragment_home_to_cameraIS_fragment)
    }

    private fun Navigate_GD()
    {
        findNavController().navigate(R.id.action_fragment_home_to_cameraGD_fragment)
    }

    private fun Navigate_OCR()
    {
        findNavController().navigate(R.id.action_home_to_ocr_fragment)
    }

    companion object
    {
        const val TAG = "HomeFragment"
    }
}