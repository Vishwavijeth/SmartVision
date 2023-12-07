package com.example.application

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.io.File

class SharedModelOD() : ViewModel() {

    private val sharedPreferenceName = "CustomModelPreference"

    private val customModelListKey = "customModelListKey"
    private val customModelFileKey = "customModelFileKey"

    //private val sharedPreferences: SharedPreferences = context.getSharedPreferences(sharedPreferenceName, Context.MODE_PRIVATE)

    val customModelList: MutableList<String> = mutableListOf()

    var customModelFile: File? = null


    var modelName: String? = null
        set(value) {
            field = value
            value?.let {
                customModelList.add(it)

//                saveCustomModelListToSharedPreferences()
            }
        }

//    init {
//        loadCustomModelListFromSharedPreferences()
//        loadCustomModelFileFromSharedPreferences()
//    }

//    private fun loadCustomModelListFromSharedPreferences()
//    {
//        val customModelListJson = sharedPreferences.getString(customModelListKey, null)
//        customModelListJson?.let {
//            customModelList.clear()
//            customModelList.addAll(it.split(","))
//        }
//    }
//
//    private fun saveCustomModelListToSharedPreferences() {
//        val editor = sharedPreferences.edit()
//        val customModelListJson = customModelList.joinToString(separator = ",")
//        editor.putString(customModelListKey, customModelListJson)
//        editor.apply()
//    }


//    private fun loadCustomModelFileFromSharedPreferences()
//    {
//        val customModelFilePath = sharedPreferences.getString(customModelFileKey, null)
//        customModelFile = customModelFilePath?.let { File(it) }
//    }

}

class SharedModelIC: ViewModel() {
    val customModelList: MutableList<String> = mutableListOf()

    var customModelFile: File? = null

    var modelName: String? = null
        set(value) {
            field = value
            value?.let {
                customModelList.add(it)
            }
        }
}

class SharedModelIS: ViewModel() {
    val customModelList: MutableList<String> = mutableListOf()

    var customModelFile: File? = null

    var modelName: String? = null
        set(value) {
            field = value
            value?.let {
                customModelList.add(it)
            }
        }
}

class SharedModelOCR: ViewModel(){
    val ocrResults = MutableLiveData<HashMap<String, Int>>()
    val texts: MutableList<String> = mutableListOf()

}
