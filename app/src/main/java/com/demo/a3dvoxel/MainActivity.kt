package com.demo.a3dvoxel

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.demo.a3dvoxel.databinding.ActivityMainBinding
import com.google.ar.core.*
import com.google.ar.core.ArCoreApk.Availability
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter

class MainActivity : AppCompatActivity() {

    private lateinit var arFragment: ArFragment
    private var session: Session? = null
    private lateinit var binding: ActivityMainBinding
    private val CAMERA_PERMISSION_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (checkCameraPermission()) {
            checkARCoreAvailability()
        } else {
            requestCameraPermission()
        }

        binding.btnStartScan.setOnClickListener {
            startScanning()
        }

        binding.btnSaveModel.setOnClickListener {
            saveModel()
        }
    }

    private fun setupARFragment() {
        arFragment = supportFragmentManager.findFragmentById(R.id.ar_scene_view) as ArFragment
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkARCoreAvailability()
            } else {
                Toast.makeText(this, "Camera permission is required to use AR", Toast.LENGTH_LONG).show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun checkARCoreAvailability() {
        val availability = ArCoreApk.getInstance().checkAvailability(this)
        when (availability) {
            Availability.UNKNOWN_ERROR -> {
                Toast.makeText(this, "An internal error occurred while determining ARCore availability.", Toast.LENGTH_LONG).show()
            }
            Availability.UNKNOWN_CHECKING -> {
                Toast.makeText(this, "Checking ARCore availability, please wait...", Toast.LENGTH_LONG).show()
            }
            Availability.UNKNOWN_TIMED_OUT -> {
                Toast.makeText(this, "Checking ARCore availability timed out. Please check your internet connection.", Toast.LENGTH_LONG).show()
            }
            Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE -> {
                Toast.makeText(this, "ARCore is not supported on this device.", Toast.LENGTH_LONG).show()
            }
            Availability.SUPPORTED_NOT_INSTALLED -> {
                Toast.makeText(this, "ARCore needs to be installed.", Toast.LENGTH_LONG).show()
            }
            Availability.SUPPORTED_APK_TOO_OLD -> {
                Toast.makeText(this, "An older version of ARCore is installed. Please update ARCore.", Toast.LENGTH_LONG).show()
            }
            Availability.SUPPORTED_INSTALLED -> {
                setupARFragment()
            }
        }
    }


    private fun startScanning() {
        try {
            session = Session(this)
            session?.configure(
                Config(session).apply {
                    depthMode = Config.DepthMode.AUTOMATIC
                }
            )
            arFragment.arSceneView.setupSession(session)

            val scene: Scene = arFragment.arSceneView.scene
            scene.addOnUpdateListener {
                val frame = arFragment.arSceneView.arFrame
                if (frame != null) {
                    for (hit in frame.hitTest(0.5f, 0.5f)) {
                        if (hit.trackable is Plane && (hit.trackable as Plane).isPoseInPolygon(hit.hitPose)) {
                            val anchor = hit.createAnchor()
                            val anchorNode = AnchorNode(anchor)
                            anchorNode.setParent(scene)
                            val transformableNode = TransformableNode(arFragment.transformationSystem)
                            transformableNode.setParent(anchorNode)
                            transformableNode.select()
                        }
                    }
                }
            }
            binding.btnSaveModel.visibility = Button.VISIBLE
        } catch (e: UnavailableArcoreNotInstalledException) {
            Toast.makeText(this, "ARCore is not installed. Please install ARCore.", Toast.LENGTH_LONG).show()
        } catch (e: UnavailableDeviceNotCompatibleException) {
            Toast.makeText(this, "This device does not support AR", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to create AR session", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveModel() {
        val file = File(getExternalFilesDir(null), "scanned_model.obj")
        FileOutputStream(file).use { outputStream ->
            val writer = OutputStreamWriter(outputStream)
            writer.write("3D model data in OBJ format")
            writer.close()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        session?.close()
    }
}


