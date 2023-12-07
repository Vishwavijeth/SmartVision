package com.example.application.fragments

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import androidx.navigation.Navigation
import androidx.recyclerview.widget.GridLayoutManager
import com.example.application.ImageSegmentationHelper
import com.example.application.OverlayView_is
import com.example.application.R
import com.example.application.SharedModelIS
import com.example.application.databinding.FragmentCameraIsBinding
import org.tensorflow.lite.task.vision.segmenter.Segmentation
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class CameraFragment_is : Fragment(), ImageSegmentationHelper.SegmentationListener {

    private var sharedModelIS: SharedModelIS? = null

    companion object {
        private const val TAG = "ImageSegmentation"

    }

    private var _fragmentCameraBinding: FragmentCameraIsBinding? = null

    private val fragmentCameraBinding
        get() = _fragmentCameraBinding!!

    private lateinit var imageSegmentationHelper: ImageSegmentationHelper
    private lateinit var bitmapBuffer: Bitmap
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private val labelsAdapter: ColorLabelsAdapter by lazy { ColorLabelsAdapter() }

    private var modelList: MutableList<String> = mutableListOf()
    private var modelName: String? = null
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
        _fragmentCameraBinding = FragmentCameraIsBinding.inflate(inflater, container, false)

        return fragmentCameraBinding.root
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        imageSegmentationHelper = ImageSegmentationHelper(
            context = requireContext(),
            imageSegmentationListener = this
        )

        cameraExecutor = Executors.newSingleThreadExecutor()

        fragmentCameraBinding.viewFinder.post {
            setUpCamera()
        }

        sharedModelIS = ViewModelProvider(requireActivity()).get(SharedModelIS::class.java)
        setupDynamicSpinner()
        initBottomSheetControls()

        with(fragmentCameraBinding.recyclerviewResults) {
            adapter = labelsAdapter
            layoutManager = GridLayoutManager(requireContext(), 3)
        }

        fragmentCameraBinding.overlay.setOnOverlayViewListener(object :
            OverlayView_is.OverlayView_isListener{
            override fun onLabels(colorLabels: List<OverlayView_is.ColorLabel>) {
                labelsAdapter.updateResultLabels(colorLabels)
            }
        })
    }

    fun getValue(): File? {
        modelName = sharedModelIS?.modelName
        modelFile = sharedModelIS?.customModelFile
        Log.d(TAG, "getData function name: $modelName")
        return modelFile
    }

    fun getCustomModelList(): MutableList<String> {
        return sharedModelIS!!.customModelList
    }

    private fun setupDynamicSpinner() {
        modelList = mutableListOf("ismodel")
        val addModel = getValue()
        val file: File? = getValue()
        Log.d(TAG, "model name from getValue function: $addModel")

        val customModelList = getCustomModelList()
        customModelList.forEach {customModel ->
            modelList.add(customModel)
            Log.d(TAG, "Model name: $customModel")
        }

        val customFile = sharedModelIS!!.customModelFile

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
            imageSegmentationHelper.getData(file)
            Log.d(TAG, "model name sent to ICHelper")
        }

        val spinner = fragmentCameraBinding.bottomSheetLayout.spinnerModel
        val adapter = ArrayAdapter(requireContext(), R.layout.spinner, modelList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        adapter.notifyDataSetChanged()
        spinner.adapter = adapter

    }

    private fun initBottomSheetControls() {

        val isSpinner = fragmentCameraBinding.bottomSheetLayout.isthredsSlider

        isSpinner.addOnChangeListener {slider, value, fromUser ->
            imageSegmentationHelper.numThreads = value.toInt()
            updateControlsUi()
        }

        fragmentCameraBinding.bottomSheetLayout.spinnerDelegate.setSelection(0, false)
        fragmentCameraBinding.bottomSheetLayout.spinnerDelegate.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    imageSegmentationHelper.currentDelegate = position
                    updateControlsUi()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    /* no op */
                }
            }

        fragmentCameraBinding.bottomSheetLayout.spinnerModel.setSelection(0, false)
        fragmentCameraBinding.bottomSheetLayout.spinnerModel.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    imageSegmentationHelper.currentModel = p2
                    updateControlsUi()
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    /* nothing */
                }

            }
    }

    private fun updateControlsUi() {
        fragmentCameraBinding.bottomSheetLayout.isThreadsValue.text =
            imageSegmentationHelper.numThreads.toString()

        imageSegmentationHelper.clearImageSegmenter()
        fragmentCameraBinding.overlay.clear()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
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

    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {

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

                        segmentImage(image)
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

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun segmentImage(image: ImageProxy) {
        image.use { bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer) }

        val imageRotation = image.imageInfo.rotationDegrees
        imageSegmentationHelper.segment(bitmapBuffer, imageRotation)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation = fragmentCameraBinding.viewFinder.display.rotation
    }

    override fun onResults(
        results: List<Segmentation>?,
        inferenceTime: Long,
        imageHeight: Int,
        imageWidth: Int
    ) {
        activity?.runOnUiThread {
            fragmentCameraBinding.bottomSheetLayout.inferenceTimeVal.text =
                String.format("%d ms", inferenceTime)

            fragmentCameraBinding.mstextIs.msdataIs.text = inferenceTime.toString()

            fragmentCameraBinding.overlay.setResults(
                results,
                imageHeight,
                imageWidth
            )

            fragmentCameraBinding.overlay.invalidate()
        }
    }

    override fun onError(error: String) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
        }
    }
}