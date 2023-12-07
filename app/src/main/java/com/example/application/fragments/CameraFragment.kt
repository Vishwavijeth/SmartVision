package com.example.application.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.example.application.ObjectDetectorHelper
import com.example.application.R
import com.example.application.SharedModelOD
import com.example.application.databinding.FragmentCameraBinding
import org.tensorflow.lite.task.vision.detector.Detection
import java.io.File
import java.util.LinkedList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragment : Fragment(), ObjectDetectorHelper.DetectorListener
{

    private var sharedViewModel: SharedModelOD? = null

    companion object
    {
        private const val TAG = "ObjectDetection"
    }

    private var _fragmentCameraBinding: FragmentCameraBinding? = null

    private val fragmentCameraBinding: FragmentCameraBinding
        get() = _fragmentCameraBinding ?: throw IllegalStateException("FragmentCameraBinding is null.")

    private lateinit var objectDetectorHelper: ObjectDetectorHelper
    private lateinit var bitmapBuffer: Bitmap
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null

    private var modelList: MutableList<String> = mutableListOf()

    private lateinit var cameraExecutor: ExecutorService
    private var modelNamme: String? = null

    override fun onResume()
    {
        super.onResume()

        if (!PermissionsFragment.hasPermissions(requireContext()))
        {
            Navigation.findNavController(requireActivity(), R.id.fragment_container)
                .navigate(CameraFragmentDirections.actionCameraToPermissions())
        }
    }

    override fun onDestroyView()
    {
        cameraExecutor.shutdown()
        //_fragmentCameraBinding = null
        Log.d(TAG, "CameraFragment is closed")
        super.onDestroyView()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentCameraBinding = FragmentCameraBinding.inflate(inflater, container, false)

        return fragmentCameraBinding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        _fragmentCameraBinding = FragmentCameraBinding.bind(view)

        objectDetectorHelper = ObjectDetectorHelper(
            context = requireContext(),
            cameraFragment = this,
            objectDetectorListener = this)

        cameraExecutor = Executors.newSingleThreadExecutor()

        fragmentCameraBinding.viewFinder.post {
            setUpCamera()
        }

        addModelToModelList()

        // edited here

//        sharedPreferences = requireContext().getSharedPreferences("sharedModelList", Context.MODE_PRIVATE)
//
//        val modelListJson = sharedPreferences.getString("modelList", null)
//        if (modelListJson != null) {
//            val gson = Gson()
//            modelList = gson.fromJson(modelListJson, object : TypeToken<List<String>>() {}.type)
//        } else {
//            // Handle the case where the model list hasn't been saved in SharedPreferences yet.
//            // You might want to initialize the modelList in this case.
//            modelList = mutableListOf()
//        }

        //end

        sharedViewModel = ViewModelProvider(requireActivity())[SharedModelOD::class.java]
        setUpDynamicSpinner()
        initBottomSheetControls()

        val callback = object : OnBackPressedCallback(true)
        {
            override fun handleOnBackPressed() {
                findNavController().navigateUp()
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)

    }

    fun addModelToModelList()
    {
        val sharedPreferences: SharedPreferences = requireContext().getSharedPreferences("modelname", Context.MODE_PRIVATE)

        val modelName = sharedPreferences.getString("modelnameString", null).toString()
        modelList.add(modelName)

        Log.d(TAG, "custom model in the shared pref: $modelName")
    }

    fun getValue(): String?
    {
        modelNamme = sharedViewModel?.modelName
        Log.d(TAG, "getData function name: $modelNamme")
        return modelNamme
    }

    fun getCustomModelList(): MutableList<String>
    {
        Log.d(TAG, "log inside getting the customModelList: ${sharedViewModel!!.customModelList}")
        return sharedViewModel!!.customModelList
    }

    private fun setUpDynamicSpinner()
    {
        modelList = mutableListOf("mobilenetv1.tflite", "efficientdet-lite0.tflite", "efficientdet-lite1.tflite", "efficientdet-lite2.tflite")
        val addModel = getValue()
        Log.d(TAG, "model name from getValue function: $addModel")
        val customModelList = getCustomModelList()

        var customList: MutableList<String> = mutableListOf()

        Log.d(TAG, "customModelList has files")

        //var name: String
        val sharedPreferences = requireContext().getSharedPreferences("modelname", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        customModelList.forEach { customModel ->
            modelList.add(customModel)
            editor.putString("modelnameString", customModel)

            //addModelToModelList(customModel)
            //name = customModel
            Log.d(TAG, "Model name: $customModel")

        }
        editor.apply()

        val customname: String? = sharedPreferences.getString("modelnameString", null)
        Log.d(TAG, "inside setup spinner function, customModelName: $customname")

//        if (!modelList.contains(customname))
//        {
//            modelList.add(customname!!)
//        }

        val customFile = sharedViewModel!!.customModelFile

        if (customFile != null)
        {
            if(customFile.exists())
            {
                Log.d(TAG, "Custom file exists")
                Log.d(TAG, "File name: ${customFile.name}")
                Log.d(TAG, "File path: ${customFile.path}")
            }
        }else
        {
            Log.d(TAG, "No custom model sent to camera fragment")
        }

        if (addModel != null)
        {
            objectDetectorHelper.getData(addModel)
            Log.d(TAG, "model name sent to ODHelper")
        }

        val spinner = fragmentCameraBinding.bottomSheetLayout.spinnerModel
        val adapter = ArrayAdapter(requireContext(), R.layout.spinner, modelList)
        adapter.setDropDownViewResource(R.layout.dropdown_view)
        adapter.notifyDataSetChanged()
        spinner.adapter = adapter
    }

    private fun initBottomSheetControls()
    {

        val slider = fragmentCameraBinding.bottomSheetLayout.odSlider

        slider.addOnChangeListener {slider, value, fromUser ->
            objectDetectorHelper.threshold = value
            updateControlsUi()
        }

        val maxSlider = fragmentCameraBinding.bottomSheetLayout.odmaxResultSlider

        maxSlider.addOnChangeListener {slider, value, fromUser ->
            objectDetectorHelper.maxResults = value.toInt()
            updateControlsUi()
        }

        val threadsSlider = fragmentCameraBinding.bottomSheetLayout.odthredsSlider

        threadsSlider.addOnChangeListener {slider, value, fromUser ->
            objectDetectorHelper.numThreads = value.toInt()
            updateControlsUi()
        }

        fragmentCameraBinding.bottomSheetLayout.spinnerDelegate.setSelection(0, false)
        fragmentCameraBinding.bottomSheetLayout.spinnerDelegate.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long)
                {
                    objectDetectorHelper.currentDelegate = p2
                    updateControlsUi()
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    /* no op */
                }
            }

        fragmentCameraBinding.bottomSheetLayout.spinnerModel.setSelection(0, false)
        fragmentCameraBinding.bottomSheetLayout.spinnerModel.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    objectDetectorHelper.currentModel = p2
                    updateControlsUi()
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    /* no op */
                }
            }
    }

    private fun updateControlsUi()
    {
        fragmentCameraBinding.bottomSheetLayout.maxResultValue.text =
            objectDetectorHelper.maxResults.toString()
        fragmentCameraBinding.bottomSheetLayout.thresholdValue.text =
            String.format("%.2f", objectDetectorHelper.threshold)
        fragmentCameraBinding.bottomSheetLayout.threadsValue.text =
            objectDetectorHelper.numThreads.toString()

        objectDetectorHelper.clearObjectDetector()
        fragmentCameraBinding.overlay.clear()

    }

    private fun setUpCamera()
    {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(
            {
                cameraProvider = cameraProviderFuture.get()

                bindCameraUseCases()
            },
            ContextCompat.getMainExecutor(requireContext())
        )
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases()
    {

        val cameraProvider =
            cameraProvider ?: throw IllegalStateException("Camera initialization failed.")

        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

        preview =
            Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
                .build()

        imageAnalyzer =
            ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()

                .also {
                    it.setAnalyzer(cameraExecutor) { image ->
                        if (!::bitmapBuffer.isInitialized) {
                            bitmapBuffer = Bitmap.createBitmap(
                                image.width,
                                image.height,
                                Bitmap.Config.ARGB_8888
                            )
                        }

                        detectObjects(image)
                    }
                }

        cameraProvider.unbindAll()

        try
        {
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)

            preview?.setSurfaceProvider(fragmentCameraBinding.viewFinder.surfaceProvider)
        } catch (exc: Exception)
        {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun detectObjects(image: ImageProxy)
    {
        image.use { bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer) }

        val imageRotation = image.imageInfo.rotationDegrees
        val customModelFile: File? = sharedViewModel!!.customModelFile
        objectDetectorHelper.detect(bitmapBuffer, imageRotation)
    }

    override fun onConfigurationChanged(newConfig: Configuration)
    {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation = fragmentCameraBinding.viewFinder.display.rotation
    }

    override fun onResults(
        results: MutableList<Detection>?,
        inferenceTime: Long,
        imageHeight: Int,
        imageWidth: Int
    ) {
        activity?.runOnUiThread {
            fragmentCameraBinding?.bottomSheetLayout?.inferenceTimeVal?.text =
                String.format("%d ms", inferenceTime)

            fragmentCameraBinding.mstext.msdata.text = inferenceTime.toString()

            val fps = inferenceTime.toFloat() * 3.280

            //fragmentCameraBinding.fpstext.fpsdata.text = fps.toString()

            fragmentCameraBinding.overlay.setResults(
                results ?: LinkedList<Detection>(),
                imageHeight,
                imageWidth
            )

            fragmentCameraBinding.overlay.invalidate()
        }
    }

    override fun onError(error: String)
    {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
        }
    }
}