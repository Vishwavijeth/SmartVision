package com.example.application.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.example.application.R
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.lang.reflect.Type
import kotlin.collections.ArrayList


class ModelManagementFragment : Fragment()
{
    private val STORAGE_PERMISSION_CODE = 1
    private val PICK_TFLITE_FILE_CODE = 2

    private lateinit var sharedPrefs: SharedPreferences
    private var deleteTrack: String = "NO"

    private lateinit var arrayList: ArrayList<ModelnameAlgo>

    //private lateInit var modelName: TextView
    private lateinit var scrollView: ScrollView

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
    }

    private fun logFileNames()
    {
        val cacheDir: File = requireContext().cacheDir
        val cacheFiles: Array<out File>? = cacheDir.listFiles()

        if (cacheFiles != null)
        {
            for (files in cacheFiles)
            {
                Log.d(TAG, "File name: ${files.name}")
            }
        }
        else {
            Log.d(TAG, "Cache is empty")
        }

    }

    private fun saveCustomView()
    {
        val sharedPreferences = requireContext().getSharedPreferences("model", Context.MODE_PRIVATE)

        val modelName = sharedPreferences.getString("modelNameKey", null).toString()
        val algo = sharedPreferences.getString("algoTypeKey", null).toString()

        arrayList = java.util.ArrayList()

        Log.d(TAG, "shared pref model name: $modelName")
        Log.d(TAG, "shared pref algo type: $algo")

        val customView = LayoutInflater.from(requireContext()).inflate(R.layout.custom_textview, null) as LinearLayout

        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        params.setMargins(0, 0, 0, 20) // Adjust the margin value as needed
        customView.layoutParams = params

        val filename: TextView = customView.findViewById(R.id.customModelTextview)
        filename.text = modelName

        val type: TextView = customView.findViewById(R.id.algotype)
        type.text = algo

        val model = ModelnameAlgo(modelName, algo)

        arrayList.add(model)

        val parentLayout: LinearLayout = scrollView.findViewById(R.id.scrollviewLinear)
        parentLayout.addView(customView)

        val deleteBtn: ImageView = customView.findViewById(R.id.modelfileDeleteBt)

        deleteBtn.setOnClickListener()
        {
            customView.removeAllViews()
            delete()
        }

//        val gson = Gson()
//        val json: String = sharedPreferences.getString("arraydetails", null).toString()

    }

    private fun delete()
    {
        val customView = LayoutInflater.from(requireContext()).inflate(R.layout.custom_textview, null) as LinearLayout
        customView.removeAllViews()

        val sharedPreferences: SharedPreferences = requireContext().getSharedPreferences("deleteTrack", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        deleteTrack = "YES"
        editor.putString("deleteItem", deleteTrack)
        editor.apply()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view =  inflater.inflate(R.layout.fragment_model_management, container, false)

        val backButton: ImageView = view.findViewById(R.id.modelManagerBackButton)

        backButton.setOnClickListener{
            NavigateToSettings()
        }

        val upload: CardView = view.findViewById(R.id.uploadModel)

        //linearLayout = view.findViewById(R.id.modelname)
        scrollView = view.findViewById(R.id.modelScrollView)

        upload.setOnClickListener {
            UploadModel()
        }

        //loadArrayList()

        sharedPrefs = requireContext().getSharedPreferences("custom_views", Context.MODE_PRIVATE)

        saveCustomView()

        logFileNames()

        //loadArrayList()

        return view
    }

    fun loadArrayList()
    {
        val sharedPreferences = requireContext().getSharedPreferences("model", Context.MODE_PRIVATE)
        val json: String? = sharedPreferences.getString("arraydetails", null)

        val gson = Gson()

        val type: Type = object : TypeToken<ArrayList<ModelnameAlgo?>?>() {}.type

        arrayList = gson.fromJson(json, type) as ArrayList<ModelnameAlgo>

        for (model in arrayList)
        {
            Log.d(TAG,"model name: ${model.modelname}, algo type: ${model.algotype}")
        }

//        if (json != null)
//        {
//            val gson = Gson()
//            val arrayListType = object : TypeToken<ArrayList<ModelnameAlgo>>() {}.type
//
//            val loadedArrayList: ArrayList<ModelnameAlgo> = gson.fromJson(json, arrayListType)
//
//            for (model in loadedArrayList) {
//                Log.d(TAG, "Loaded Modelname: ${model.modelname}, AlgoType: ${model.algotype}")
//            }
//
//        } else {
//            Log.d(TAG, "No data found in SharedPreferences.")
//        }
    }

    fun NavigateToSettings()
    {
        findNavController().popBackStack()
    }
     override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)
        //linearLayout = view.findViewById(R.id.modelname)
        scrollView = view.findViewById(R.id.modelScrollView)

    }

    override fun onSaveInstanceState(outState: Bundle)
    {
        super.onSaveInstanceState(outState)

        val customView = LayoutInflater.from(requireContext()).inflate(R.layout.custom_textview, null) as LinearLayout
        val filename: TextView = customView.findViewById(R.id.customModelTextview)

        Log.d(TAG, "save instance filename: ${filename.text}")

        val writtenData: CharSequence = "abcd"
        outState.putCharSequence("SavedData", writtenData)

    }

    override fun onViewStateRestored(savedInstanceState: Bundle?)
    {
        super.onViewStateRestored(savedInstanceState)

        val storedData: CharSequence? = savedInstanceState?.getCharSequence("SavedData")
        val txt = "abc"

        Log.d(TAG, "restored file name: $storedData")
    }

    private fun UploadModel()
    {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                STORAGE_PERMISSION_CODE
            )
        } else
        {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                //putExtra(DocumentsContract.EXTRA_INITIAL_URI, PICK_TFLITE_FILE_CODE)
            }
            startActivityForResult(intent, PICK_TFLITE_FILE_CODE)
        }
    }

    @SuppressLint("Recycle")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        super.onActivityResult(requestCode, resultCode, data)

        Log.d(TAG, "request code: $requestCode")
        Log.d(TAG, "result code: $resultCode")
        Log.d(TAG, "activity code: ${Activity.RESULT_OK}")
        if (requestCode == PICK_TFLITE_FILE_CODE && resultCode == Activity.RESULT_OK) {

            data?.data?.let { uri ->

                val selectedFile = GetFileName(uri)
                val fileUri = uri

                ShowCustomDialog(selectedFile, fileUri)

            }
        }
    }

    @SuppressLint("ResourceAsColor", "InflateParams")
    fun addTextView(text: String, algoTye: String)
    {
        val customView = LayoutInflater.from(requireContext()).inflate(R.layout.custom_textview, null) as LinearLayout

        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        params.setMargins(0, 0, 0, 20) // Adjust the margin value as needed
        customView.layoutParams = params

        val filename: TextView = customView.findViewById(R.id.customModelTextview)
        filename.text = text

        val type: TextView = customView.findViewById(R.id.algotype)
        type.text = algoTye

        val model = ModelnameAlgo(text, algoTye)

        arrayList.add(model)

        val parentLayout: LinearLayout = scrollView.findViewById(R.id.scrollviewLinear)

        val sharedPreferences = requireContext().getSharedPreferences("model", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("modelNameKey", text)
        editor.putString("algoTypeKey", algoTye)

        val gson = Gson()
        arrayList.add(ModelnameAlgo(text, algoTye))
        val json: String = gson.toJson(arrayList)
        editor.putString("arraydetails", json)

        editor.apply()

        parentLayout.addView(customView)
        //linearLayout.addView(customView)

        val deleteBtn: ImageView = customView.findViewById(R.id.modelfileDeleteBt)

        deleteBtn.setOnClickListener() {
            customView.removeAllViews()
            delete()
        }
    }

    @SuppressLint("Range")
    private fun GetFileName(uri: Uri): String
    {
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        cursor?.let {
            if (it.moveToFirst()) {
                val displayName = it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                it.close()
                Log.d(Settings.TAG,"fetched the display name $displayName - ${uri.path}")
                return displayName
            }
        }
        return uri.lastPathSegment ?: ""
    }

    @SuppressLint("ResourceAsColor")
    private fun ShowCustomDialog(selectedFile: String, fileUri: Uri)
    {
        val dialogFragment = ModelTypeFragment()
        val args = Bundle()

        dialogFragment.setFragment(this)

        args.putString("selectedFileName", selectedFile)
        args.putString("selectedFileUri", fileUri.toString())
        dialogFragment.arguments = args

        dialogFragment.show(childFragmentManager, "customDialog")
    }

    companion object
    {
        const val TAG = "ModelManagementFragment"
    }
}

data class ModelnameAlgo(val modelname: String, val algotype: String)