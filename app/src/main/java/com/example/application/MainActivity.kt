package com.example.application

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.example.application.databinding.ActivityMainBinding
import com.example.application.fragments.CameraFragment_ic


class MainActivity : AppCompatActivity(){

    private lateinit var activityMainBinding: ActivityMainBinding
    private lateinit var navController: NavController


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityMainBinding.root)

        // Get the NavHostFragment
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
        // Get the NavController from the NavHostFragment
        navController = navHostFragment.findNavController()

    }

}
