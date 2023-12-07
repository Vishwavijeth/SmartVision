package com.example.application.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.application.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.application.camera.CameraSource
import com.example.application.data.Device
import com.example.application.ml.*


class CameraFragment_pe : Fragment() {
    companion object {
        private const val FRAGMENT_DIALOG = "dialog"
    }

    private lateinit var surfaceView: SurfaceView

    private var modelPos = 1

    private var device = Device.CPU

    private lateinit var tvScore: TextView
    private lateinit var tvFPS: TextView
    private lateinit var spnDevice: Spinner
    private lateinit var spnModel: Spinner
    private lateinit var spnTracker: Spinner
    private lateinit var vTrackerOption: View
    private lateinit var tvClassificationValue1: TextView
    private lateinit var tvClassificationValue2: TextView
    private lateinit var tvClassificationValue3: TextView
    private lateinit var swClassification: SwitchCompat
    private lateinit var vClassificationOption: View
    private var cameraSource: CameraSource? = null
    private var isClassifyPose = false
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                openCamera()
            } else {
            }
        }

    private var changeModelListener = object : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>?) {
            // do nothing
        }

        override fun onItemSelected(
            parent: AdapterView<*>?,
            view: View?,
            position: Int,
            id: Long
        ) {
            changeModel(position)
        }
    }

    private var changeDeviceListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            changeDevice(position)
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
            // do nothing
        }
    }

    private var changeTrackerListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            changeTracker(position)
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
            // do nothing
        }
    }

    private var setClassificationListener =
        CompoundButton.OnCheckedChangeListener { _, isChecked ->
            showClassificationResult(isChecked)
            isClassifyPose = isChecked
            isPoseClassifier()
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_camera_pe, container, false)
        // keep screen on while app is running
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        tvScore = rootView.findViewById(R.id.tvScore)
        tvFPS = rootView.findViewById(R.id.tvFps)
        spnModel = rootView.findViewById(R.id.spnModel)
        spnDevice = rootView.findViewById(R.id.spnDevice)
        spnTracker = rootView.findViewById(R.id.spnTracker)
        vTrackerOption = rootView.findViewById(R.id.vTrackerOption)
        surfaceView = rootView.findViewById(R.id.surfaceView)
        tvClassificationValue1 = rootView.findViewById(R.id.tvClassificationValue1)
        tvClassificationValue2 = rootView.findViewById(R.id.tvClassificationValue2)
        tvClassificationValue3 = rootView.findViewById(R.id.tvClassificationValue3)
        swClassification = rootView.findViewById(R.id.swPoseClassification)
        vClassificationOption = rootView.findViewById(R.id.vClassificationOption)
        initSpinner()
        spnModel.setSelection(modelPos)
        swClassification.setOnCheckedChangeListener(setClassificationListener)
        if (!isCameraPermissionGranted()) {
            requestPermission()
        }
        return rootView
    }

    override fun onStart() {
        super.onStart()
        openCamera()
    }

    override fun onResume() {
        cameraSource?.resume()
        super.onResume()
    }

    override fun onPause() {
        cameraSource?.close()
        cameraSource = null
        super.onPause()
    }

    // check if permission is granted or not.
    private fun isCameraPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    // open camera
    private fun openCamera() {
        if (isCameraPermissionGranted()) {
            if (cameraSource == null) {
                cameraSource =
                    CameraSource(surfaceView, object : CameraSource.CameraSourceListener {
                        override fun onFPSListener(fps: Int) {
                            tvFPS.text = getString(R.string.tfe_pe_tv_fps, fps)
                        }

                        override fun onDetectedInfo(
                            personScore: Float?,
                            poseLabels: List<Pair<String, Float>>?
                        ) {
                            tvScore.text = getString(R.string.tfe_pe_tv_score, personScore ?: 0f)
                            poseLabels?.sortedByDescending { it.second }?.let {
                                tvClassificationValue1.text = getString(
                                    R.string.tfe_pe_tv_classification_value,
                                    convertPoseLabels(if (it.isNotEmpty()) it[0] else null)
                                )
                                tvClassificationValue2.text = getString(
                                    R.string.tfe_pe_tv_classification_value,
                                    convertPoseLabels(if (it.size >= 2) it[1] else null)
                                )
                                tvClassificationValue3.text = getString(
                                    R.string.tfe_pe_tv_classification_value,
                                    convertPoseLabels(if (it.size >= 3) it[2] else null)
                                )
                            }
                        }

                    }).apply {
                        prepareCamera()
                    }
                isPoseClassifier()
                lifecycleScope.launch(Dispatchers.Main) {
                    cameraSource?.initCamera()
                }
            }
            createPoseEstimator()
        }
    }

    private fun convertPoseLabels(pair: Pair<String, Float>?): String {
        if (pair == null) return "empty"
        return "${pair.first} (${String.format("%.2f", pair.second)})"
    }

    private fun isPoseClassifier() {
        cameraSource?.setClassifier(if (isClassifyPose) PoseClassifier.create(requireContext()) else null)
    }

    // Initialize spinners to let user select model/accelerator/tracker.
    private fun initSpinner() {
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.tfe_pe_models_array,
            R.layout.spinner
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            spnModel.adapter = adapter
            spnModel.onItemSelectedListener = changeModelListener
        }

        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.tfe_pe_device_name,
            R.layout.spinner
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            spnDevice.adapter = adapter
            spnDevice.onItemSelectedListener = changeDeviceListener
        }

        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.tfe_pe_tracker_array,
            R.layout.spinner
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            spnTracker.adapter = adapter
            spnTracker.onItemSelectedListener = changeTrackerListener
        }
    }

    // Change model when the app is running
    private fun changeModel(position: Int) {
        if (modelPos == position) return
        modelPos = position
        createPoseEstimator()
    }

    // Change device (accelerator) type when the app is running
    private fun changeDevice(position: Int) {
        val targetDevice = when (position) {
            0 -> Device.CPU
            1 -> Device.GPU
            else -> Device.NNAPI
        }
        if (device == targetDevice) return
        device = targetDevice
        createPoseEstimator()
    }

    // Change tracker for MoveNet MultiPose model
    private fun changeTracker(position: Int) {
        cameraSource?.setTracker(
            when (position) {
                1 -> TrackerType.BOUNDING_BOX
                2 -> TrackerType.KEYPOINTS
                else -> TrackerType.OFF
            }
        )
    }

    private fun createPoseEstimator() {

        val poseDetector = when (modelPos) {
            0 -> {
                // MoveNet Lightning (SinglePose)
                showPoseClassifier(true)
                showDetectionScore(true)
                showTracker(false)
                MoveNet.create(requireContext(), device, ModelType.Lightning)
            }
            1 -> {
                // MoveNet Thunder (SinglePose)
                showPoseClassifier(true)
                showDetectionScore(true)
                showTracker(false)
                MoveNet.create(requireContext(), device, ModelType.Thunder)
            }
            2 -> {
                // MoveNet (Lightning) MultiPose
                showPoseClassifier(false)
                showDetectionScore(false)
                // Movenet MultiPose Dynamic does not support GPUDelegate
                if (device == Device.GPU) {
                    showToast(getString(R.string.tfe_pe_gpu_error))
                }
                showTracker(true)
                MoveNetMultiPose.create(
                    requireContext(),
                    device,
                    Type.Dynamic
                )
            }
            3 -> {
                // PoseNet (SinglePose)
                showPoseClassifier(true)
                showDetectionScore(true)
                showTracker(false)
                PoseNet.create(requireContext(), device)
            }
            else -> {
                null
            }
        }
        poseDetector?.let { detector ->
            cameraSource?.setDetector(detector)
        }
    }

    // Show/hide the pose classification option.
    private fun showPoseClassifier(isVisible: Boolean) {
        vClassificationOption.visibility = if (isVisible) View.VISIBLE else View.GONE
        if (!isVisible) {
            swClassification.isChecked = false
        }
    }

    // Show/hide the detection score.
    private fun showDetectionScore(isVisible: Boolean) {
        tvScore.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    // Show/hide classification result.
    private fun showClassificationResult(isVisible: Boolean) {
        val visibility = if (isVisible) View.VISIBLE else View.GONE
        tvClassificationValue1.visibility = visibility
        tvClassificationValue2.visibility = visibility
        tvClassificationValue3.visibility = visibility
    }

    // Show/hide the tracking options.
    private fun showTracker(isVisible: Boolean) {
        if (isVisible) {
            // Show tracker options and enable Bounding Box tracker.
            vTrackerOption.visibility = View.VISIBLE
            spnTracker.setSelection(1)
        } else {
            // Set tracker type to off and hide tracker option.
            vTrackerOption.visibility = View.GONE
            spnTracker.setSelection(0)
        }
    }

    private fun requestPermission() {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) -> {
                // You can use the API that requires the permission.
                openCamera()
            }
            else -> {
                requestPermissionLauncher.launch(
                    Manifest.permission.CAMERA
                )
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    class ErrorDialog : DialogFragment() {

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
            AlertDialog.Builder(requireActivity())
                .setMessage(requireArguments().getString(ARG_MESSAGE))
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    // do nothing
                }
                .create()

        companion object {

            @JvmStatic
            private val ARG_MESSAGE = "message"

            @JvmStatic
            fun newInstance(message: String): ErrorDialog = ErrorDialog().apply {
                arguments = Bundle().apply { putString(ARG_MESSAGE, message) }
            }
        }
    }
}
