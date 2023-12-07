package com.example.application.fragments

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.example.application.R
import com.example.application.SharedModelIC
import com.example.application.SharedModelOD
import java.io.File
import java.io.FileOutputStream

class ModelTypeFragment : DialogFragment()
{

    private lateinit var sharedViewModel: SharedModelOD
    private lateinit var sharedModelIC: SharedModelIC

    private var modelManagementFragment: ModelManagementFragment? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_model_type, container, false)

        val selectedFileName = arguments?.getString("selectedFileName")
        val fileUriAsString = arguments?.getString("selectedFileUri")
        val fileUri = Uri.parse(fileUriAsString)

        val submit: Button = view.findViewById(R.id.submit)

        sharedViewModel = ViewModelProvider(requireActivity())[SharedModelOD::class.java]
        sharedModelIC = ViewModelProvider(requireActivity())[SharedModelIC::class.java]

        submit.setOnClickListener{
            SubmitAction(fileUri, selectedFileName!!)
        }

        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        //modelManagementFragment = ModelManagementFragment()
    }

    fun setFragment(fragment: ModelManagementFragment)
    {
        modelManagementFragment = fragment
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog
    {
        return super.onCreateDialog(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
    }


    private fun SubmitAction(fileUri: Uri, fileName: String)
    {
        val radioGroup = view?.findViewById<RadioGroup>(R.id.radio)
        val selectedRadioButtonId = radioGroup?.checkedRadioButtonId

        val selectedModelType = when (selectedRadioButtonId) {
            R.id.radioOD -> {
                LoadODModel(fileUri, fileName)
                "Object Detection"
            }
//            R.id.radioIC -> {
//                LoadICModel(fileUri, fileName)
//                "Image Classification"
//            }
            R.id.radioIS -> {
                LoadISModel()
                "Image Segmentation"
            }
//            R.id.radioOCR -> {
//                LoadOCRModel()
//                "OCR"
//            }
            else -> "Unknown"
        }

        dismiss()

        val toastMessage = "Selected Model Type: $selectedModelType"
        Toast.makeText(requireContext(), toastMessage, Toast.LENGTH_SHORT).show()
    }

    private fun CopyModelFile(fileUri: Uri, destinationFileName: String, destinationDir: File): Boolean
    {
        val contentResolver = requireContext().contentResolver
        val inputStream = contentResolver.openInputStream(fileUri)

        try
        {
            val outputFile = File(destinationDir, destinationFileName)

            inputStream?.use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            Toast.makeText(requireContext(), "Model file copied to app storage", Toast.LENGTH_SHORT).show()

            return true
        } catch (e: Exception)
        {
            e.printStackTrace()
            return false
        }
    }

    private fun LoadODModel(fileUri: Uri, fileName: String)
    {
        //modelManagementFragment = ModelManagementFragment()
        modelManagementFragment?.addTextView(fileName, "Object detection")

        val cacheDirectory = requireContext().cacheDir
        val destinationFile = File(cacheDirectory, fileName)

        val inputStream = requireContext().contentResolver.openInputStream(fileUri)

        inputStream?.use { input ->
            FileOutputStream(destinationFile).use { output ->
                input.copyTo(output)
            }
        }

        val copyFile = CopyModelFile(fileUri, fileName, requireContext().cacheDir)

        if (copyFile)
        {

            try
            {
                sharedViewModel.modelName = destinationFile.name
                sharedViewModel.customModelFile = destinationFile

                Log.d(TAG, "Model file Sent")

                val cacheFiles = cacheDirectory.listFiles()
                cacheFiles?.forEach { file ->
                    Log.d(TAG, "Custom model file name: ${file.name}")
                    Log.d(TAG, "Custom model file path: ${file.absolutePath}")
                }

            }catch (e: Exception)
            {
                Log.d(TAG, "Error : $e")
            }
        }
        else
        {
            Log.d(TAG, "Could not send model file")
        }
    }

    private fun LoadICModel(fileUri: Uri, fileName: String)
    {

        modelManagementFragment?.addTextView(fileName, "Image classification")

        val cacheDirectory = requireContext().cacheDir
        val destinationFile = File(cacheDirectory, fileName)

        val inputStream = requireContext().contentResolver.openInputStream(fileUri)

        inputStream?.use { input ->
            FileOutputStream(destinationFile).use { output ->
                input.copyTo(output)
            }
        }

        val copyFile = CopyModelFile(fileUri, fileName, requireContext().cacheDir)

        if (copyFile){

            try {
                sharedModelIC.modelName = destinationFile.name
                sharedModelIC.customModelFile = destinationFile

                Log.d(TAG, "Model file Sent")

                val cacheFiles = cacheDirectory.listFiles()
                cacheFiles?.forEach { file ->
                    Log.d(TAG, "Custom model file name: ${file.name}")
                    Log.d(TAG, "Custom model file path: ${file.absolutePath}")
                }

            }catch (e: Exception){
                Log.d(TAG, "Error : $e")
            }
        }else{
            Log.d(TAG, "Could not send model file")
        }

    }

    private fun LoadISModel()
    {

    }

    private fun LoadOCRModel() {}

    companion object
    {
        val TAG = "ModelType"
    }
}