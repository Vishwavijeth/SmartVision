package com.example.application.fragments

import android.annotation.SuppressLint
import android.app.Application
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.application.ImageClassifierHelper
import com.example.application.R
import com.example.application.SharedModelIC
import com.example.application.databinding.FragmentCameraIcBinding
import org.tensorflow.lite.task.vision.classifier.Classifications
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragment_ic : Fragment(), ImageClassifierHelper.ClassifierListener{

    private var sharedModelIC: SharedModelIC? = null

    companion object {
        private const val TAG = "ImageClassifier"
    }

    private var _fragmentCameraBinding: FragmentCameraIcBinding? = null
    private val fragmentCameraBinding: FragmentCameraIcBinding
        get() {
            if (_fragmentCameraBinding == null) {
                throw IllegalStateException("FragmentCameraIcBinding is not initialized properly.")
            }
            return _fragmentCameraBinding!!
        }


    private lateinit var imageClassifierHelper: ImageClassifierHelper
    private lateinit var bitmapBuffer: Bitmap
    private val classificationResultsAdapter by lazy {
        ClassificationResultsAdapter().apply {
            updateAdapterSize(imageClassifierHelper.maxResults)
        }
    }
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null

    private var modelList: MutableList<String> = mutableListOf()
    private var modelNamme: String? = null
    private var modelFile: File? = null

    private lateinit var cameraExecutor: ExecutorService

    override fun onResume() {
        super.onResume()

        if (!PermissionsFragment.hasPermissions(requireContext())) {
            Navigation.findNavController(requireActivity(), R.id.fragment_container)
                .navigate(CameraFragmentDirections.actionCameraToPermissions())
        }
    }


    override fun onDestroyView() {
        //_fragmentCameraBinding = null
        super.onDestroyView()

        cameraExecutor.shutdown()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentCameraBinding = FragmentCameraIcBinding.inflate(inflater, container, false)

        return fragmentCameraBinding!!.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        imageClassifierHelper =
            ImageClassifierHelper(context = requireContext(), imageClassifierListener = this)

        with(fragmentCameraBinding.recyclerviewResults) {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = classificationResultsAdapter
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        fragmentCameraBinding.viewFinder.post {
            setUpCamera()
        }

        sharedModelIC = ViewModelProvider(requireActivity()).get(SharedModelIC::class.java)
        setupDynamicSpinner()
        initBottomSheetControls()
    }

    fun getValue(): File? {
        modelNamme = sharedModelIC?.modelName
        modelFile = sharedModelIC?.customModelFile
        Log.d(TAG, "getData function name: $modelNamme")
        return modelFile
    }

    fun getCustomModelList(): MutableList<String> {
        return sharedModelIC!!.customModelList
    }

    private fun setupDynamicSpinner(){
        modelList = mutableListOf("MobileNet V1", "EfficientNet Lite0", "EfficientNet Lite1", "EfficientNet Lite2")
        val addModel = getValue()
        val file: File? = getValue()
        Log.d(TAG, "model name from getValue function: $addModel")

        val customModelList = getCustomModelList()
        customModelList.forEach {customModel ->
            modelList.add(customModel)
            Log.d(TAG, "Model name: $customModel")
        }

        val customFile = sharedModelIC!!.customModelFile

        val cacheDirectory = requireContext().cacheDir
        val cacheFiles = cacheDirectory.listFiles()

        for (i in cacheFiles!!){
            Log.d(TAG, i.name)
        }

        if (customFile != null) {
            if(customFile.exists()){
                Log.d(TAG, "Custom file exists")
                Log.d(TAG, "File name: ${customFile.name}")
                Log.d(TAG, "File path: ${customFile.path}")

            }
        }else{
            Log.d(TAG, "No custom model sent to camera fragment for IC")
        }

        if (addModel != null){
            //imageClassifierHelper.getData(file?.name)
            imageClassifierHelper.getData(file)
            Log.d(TAG, "model name sent to ICHelper")
        }

        val spinner = fragmentCameraBinding.bottomSheetLayout.spinnerModel
        val adapter = ArrayAdapter(requireContext(), R.layout.spinner, modelList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        adapter.notifyDataSetChanged()
        spinner.adapter = adapter
    }

    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(
            {
                cameraProvider = cameraProviderFuture.get()

                bindCameraUseCases()
            },
            ContextCompat.getMainExecutor(requireContext())
        )
    }

    private fun initBottomSheetControls() {


        fragmentCameraBinding.bottomSheetLayout.spinnerDelegate.setSelection(0, false)
        fragmentCameraBinding.bottomSheetLayout.spinnerDelegate.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    imageClassifierHelper.currentDelegate = position
                    updateControlsUi()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    /* no op */
                }
            }

        fragmentCameraBinding.bottomSheetLayout.spinnerModel.setSelection(0, false)
        fragmentCameraBinding.bottomSheetLayout.spinnerModel.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    imageClassifierHelper.currentModel = position
                    updateControlsUi()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    /* no op */
                }
            }
    }

    private fun updateControlsUi() {
//        fragmentCameraBinding.bottomSheetLayout.maxResultsValue.text =
//            imageClassifierHelper.maxResults.toString()

        fragmentCameraBinding.bottomSheetLayout.thresholdValue.text =
            String.format("%.2f", imageClassifierHelper.threshold)
        fragmentCameraBinding.bottomSheetLayout.threadsValue.text =
            imageClassifierHelper.numThreads.toString()

        imageClassifierHelper.clearImageClassifier()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation = fragmentCameraBinding.viewFinder.display.rotation
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {

        // CameraProvider
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
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
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

                        classifyImage(image)
                    }
                }

        cameraProvider.unbindAll()

        try {
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)

            preview?.setSurfaceProvider(fragmentCameraBinding.viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun getScreenOrientation() : Int {
        val outMetrics = DisplayMetrics()

        val display: Display?
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            display = requireActivity().display
            display?.getRealMetrics(outMetrics)
        } else {
            @Suppress("DEPRECATION")
            display = requireActivity().windowManager.defaultDisplay
            @Suppress("DEPRECATION")
            display.getMetrics(outMetrics)
        }

        return display?.rotation ?: 0
    }

    private fun classifyImage(image: ImageProxy) {

        image.use { bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer) }

        imageClassifierHelper.classify(bitmapBuffer, getScreenOrientation())


    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onError(error: String) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            classificationResultsAdapter.updateResults(null)
            classificationResultsAdapter.notifyDataSetChanged()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onResults(
        results: List<Classifications>?,
        inferenceTime: Long
    ) {
        activity?.runOnUiThread {

            classificationResultsAdapter.updateResults(results)
            classificationResultsAdapter.notifyDataSetChanged()
            fragmentCameraBinding.bottomSheetLayout.inferenceTimeVal.text =
                String.format("%d ms", inferenceTime)

        }
    }
}