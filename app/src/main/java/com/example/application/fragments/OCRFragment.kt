package com.example.application.fragments

import android.app.Application
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.*
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.TextView
import androidx.camera.core.*
import androidx.camera.core.Camera
import androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.example.application.*
import com.example.application.R
import com.example.application.databinding.FragmentOCRBinding
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import java.lang.IllegalStateException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class OCRFragment : Fragment(), OCRModelExecutor.OCRResult
{

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var modelExecutor: OCRModelExecutor
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var viewModel: MLExecutionViewModel
    private lateinit var recognizedText: TextView
    private lateinit var bitmapBuffer: Bitmap
    private var imageAnalyzer: ImageAnalysis? = null
    private var preview: Preview? = null
    private var camera: Camera? = null
    private lateinit var ocrViewModel: SharedModelOCR
    private lateinit var overlayOCR: OverlayOCR
    private lateinit var chipsGroup: ChipGroup
    private lateinit var modelExecutionResult: ModelExecutionResult

    private var _fragmentCameraOCRBinding: FragmentOCRBinding? = null

    private val fragmentOCRBinding
        get() = _fragmentCameraOCRBinding!!


    override fun onResume()
    {
        super.onResume()
        if (!PermissionsFragment.hasPermissions(requireContext())) {
            Navigation.findNavController(requireActivity(), R.id.fragment_container)
                .navigate(CameraFragmentDirections.actionCameraToPermissions())
        }
    }

    override fun onDestroyView()
    {
        //_fragmentCameraOCRBinding = null
        super.onDestroyView()

        cameraExecutor.shutdown()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentCameraOCRBinding = FragmentOCRBinding.inflate(inflater, container, false)

        ocrViewModel = ViewModelProvider(requireActivity()).get(SharedModelOCR::class.java)
        overlayOCR = fragmentOCRBinding.overlayOCR
        overlayOCR = OverlayOCR(requireContext(), null)
        //chipsGroup = fragmentOCRBinding.ocrSurface
        //modelExecutionResult = ModelExecutionResult()

        chipsGroup = fragmentOCRBinding.bottomSheet.chipsGroup
        ocrViewModel = ViewModelProvider(requireActivity()).get(SharedModelOCR::class.java)
        viewModel = ViewModelProvider.AndroidViewModelFactory(Application()).create(MLExecutionViewModel::class.java)

        viewModel.resultingBitmap.observe(
            viewLifecycleOwner
        ) { resultImage ->
            if (resultImage != null) {
                updateUIWithResults(resultImage)
            }
        }


        //setChipToLogView(HashMap<String, Int>())
        return fragmentOCRBinding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        ocrViewModel = ViewModelProvider(requireActivity()).get(SharedModelOCR::class.java)


        modelExecutor = OCRModelExecutor(
            context = requireContext()
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        chipsGroup.removeAllViews()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()
        //modelExecutor = OCRModelExecutor(requireContext(), viewModel = ocrViewModel)
        //viewModel = ViewModelProvider(requireActivity()).get(MLExecutionViewModel::class.java)

        //viewModel = AndroidViewModelFactory().create(MLExecutionViewModel::class.java)

//        viewModel.resultingBitmap.observe(
//            viewLifecycleOwner,
//            {resultImage ->
//                if(resultImage != null) {
//                    updateUIWithResults(resultImage)
//                }
//            }
//        )

        fragmentOCRBinding.ocrSurface.post{
            SetupCamera()
        }

        ocrViewModel.ocrResults.observe(viewLifecycleOwner, Observer { ocrResults ->
            // Handle the OCR results here, update the text view or perform any actions you need
            // Example:
            // textView.text = "OCR Results: $ocrResults"
            Log.d(TAG, "texts: $ocrResults")
        })

    }

    private fun SetupCamera()
    {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(
            {
                cameraProvider = cameraProviderFuture.get()
                BindCameraUseCases()
            },
            ContextCompat.getMainExecutor(requireContext())
        )
    }

    private fun BindCameraUseCases()
    {

        val cameraProvider = cameraProvider ?: throw IllegalStateException("camera initialization failed")

        val cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

        preview =
            Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(fragmentOCRBinding.ocrSurface.display.rotation)
                .build()

        imageAnalyzer =
            ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(fragmentOCRBinding.ocrSurface.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()

                .also {
                    it.setAnalyzer(cameraExecutor) {image ->
                        if (!::bitmapBuffer.isInitialized) {
                            bitmapBuffer = Bitmap.createBitmap(
                                image.width,
                                image.height,
                                Bitmap.Config.ARGB_8888
                            )
                        }

                        val metadata = image.imageInfo
//                        Log.d(TAG, "frameImage : $metadata")
//                        Log.d(TAG, "imageHeight:${image.height}, imageWidth:${image.width}")
                        processImage(image)
                    }
                }

        cameraProvider.unbindAll()

        try {
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)

            preview?.setSurfaceProvider(fragmentOCRBinding.ocrSurface.surfaceProvider)
        } catch (e: Exception){
            Log.e(TAG, "use case binding failed", e)
        }
    }

    private fun processImage(image: ImageProxy)
    {
        image.use {
            bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer)

            val imageRotation = image.imageInfo.rotationDegrees

            //execute(bitmapBuffer, imageRotation)

            modelExecutor.execute(bitmapBuffer)

            //Log.d(TAG, "test print: $test")
            //overlayOCR.setBoundingBoxInfo(boundingBox)
        }
    }

    private fun execute(image: Bitmap, imageRotation: Int)
    {
//        Log.d(TAG, "image : ${image}")
//        Log.d(TAG, "${image.height.toFloat()}")
        val framesToProcess = listOf(image)

        viewModel.processFramesForOCR(framesToProcess, modelExecutor, cameraExecutor)
    }

    private fun getOptions(): Interpreter.Options
    {
        val gpuDelegate = GpuDelegate()
        val options = Interpreter.Options()
        options.addDelegate(gpuDelegate)
        return options
    }

    private fun getColorStateListForChip(color: Int): ColorStateList {
        val states =
            arrayOf(
                intArrayOf(android.R.attr.state_enabled), // enabled
                intArrayOf(android.R.attr.state_pressed) // pressed
            )

        val colors = intArrayOf(color, color)
        return ColorStateList(states, colors)
    }

    private fun setChipToLogView(itemsFound: Map<String, Int>){
        chipsGroup.removeAllViews()

        for ((word, color) in itemsFound) {
            val chip = Chip(requireContext())
            chip.text = word
            chip.chipBackgroundColor = getColorStateListForChip(color)
            chip.isClickable = false
            chipsGroup.addView(chip)
        }

        val labelsFoundTextView: TextView = fragmentOCRBinding.bottomSheet.tfeIsLabelsFound
        if (chipsGroup.childCount == 0) {
            labelsFoundTextView.text = getString(R.string.tfe_ocr_no_text_found)
        } else {
            labelsFoundTextView.text = getString(R.string.tfe_ocr_texts_found)
        }
        chipsGroup.parent.requestLayout()
    }

    private fun updateUIWithResults(modelExecutionResult: ModelExecutionResult){
        val logText: TextView = fragmentOCRBinding.ocrSurface.findViewById(R.id.log_view)
        logText.text = modelExecutionResult.executionLog

        setChipToLogView(modelExecutionResult.itemsFound)
    }

    private fun updateChipGroup(ocrResults: HashMap<String, Int>) {
        // Clear existing chips
        chipsGroup.removeAllViews()

        // Add new chips based on OCR results
        ocrResults.forEach { (text, _) ->
            val chip = Chip(chipsGroup.context)
            chip.text = text
            chip.isClickable = false
            chip.isCheckable = false
            chipsGroup.addView(chip)
        }
    }

    companion object
    {
        private const val TAG = "OCRFragment"
    }

    override fun onOCRResult(ocrResults: HashMap<String, Int>) {
        updateChipGroup(ocrResults)

        Log.d(TAG, "ocrResults: $ocrResults")
    }

}



