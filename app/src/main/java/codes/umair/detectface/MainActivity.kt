package codes.umair.detectface

import  android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.SparseIntArray
import android.view.Surface
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout.*
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import com.otaliastudios.cameraview.CameraView
import com.otaliastudios.cameraview.Facing
import com.otaliastudios.cameraview.SizeSelector
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    private val ORIENTATIONS = SparseIntArray()
    private var frameCount = 0L
    private lateinit var metadata: FirebaseVisionImageMetadata.Builder
    var cameraId = 1
    private lateinit var cameraV : CameraView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ORIENTATIONS.append(Surface.ROTATION_0, 90)
        ORIENTATIONS.append(Surface.ROTATION_90, 0)
        ORIENTATIONS.append(Surface.ROTATION_180, 270)
        ORIENTATIONS.append(Surface.ROTATION_270, 180)

        cameraV = CameraView(this)

        var params: LayoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        )
        cameraV.layoutParams = params
        cameraV.facing = Facing.FRONT

        val options = FirebaseVisionFaceDetectorOptions.Builder()
            .setPerformanceMode(FirebaseVisionFaceDetectorOptions.FAST)
            .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
            .setMinFaceSize(0.15f)
            .build()
        val faceDetector = FirebaseVision.getInstance().getVisionFaceDetector(options)

        metadata = FirebaseVisionImageMetadata.Builder()
            .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)

        cameraV.addFrameProcessor {
            if (frameCount++ % 24 != 0L) {
                return@addFrameProcessor
            }
            try {
                val size = cameraV.previewSize
                if (size != null) {
                    metadata
                        .setHeight(size.height)
                        .setWidth(size.width)
                }

                var rotationCompensation =
                    ORIENTATIONS.get(this@MainActivity.windowManager.defaultDisplay.rotation)
                rotationCompensation = (rotationCompensation + it.rotation + 270) % 360
                val result: Int
                when (rotationCompensation) {
                    0 -> result = FirebaseVisionImageMetadata.ROTATION_0
                    90 -> result = FirebaseVisionImageMetadata.ROTATION_90
                    180 -> result = FirebaseVisionImageMetadata.ROTATION_180
                    270 -> result = FirebaseVisionImageMetadata.ROTATION_270
                    else -> {
                        result = FirebaseVisionImageMetadata.ROTATION_0
                        Log.e("rot", "Bad rotation value: $rotationCompensation")
                    }
                }

                metadata.setRotation(result)

                val image = FirebaseVisionImage.fromByteArray(it.data, metadata.build())

                faceDetector.detectInImage(image)
                    .addOnSuccessListener { faces ->
                        if (faces.isNotEmpty()) {
                            Toast.makeText(this@MainActivity, "Face Detected", Toast.LENGTH_SHORT)
                                .show()

                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                "Face Not Detected",
                                Toast.LENGTH_SHORT
                            ).show()
                            launchApp("codes.umair.findthatbook")
                        }
                    }
                    .addOnFailureListener {
                        it.printStackTrace()
                        Toast.makeText(
                            this@MainActivity,
                            "Failure " + it.localizedMessage,
                            Toast.LENGTH_SHORT
                        ).show()

                    }
            } catch (e: Exception) {
            }
        }
    }

    override fun onStart() {
        super.onStart()
        cameraV.start()
    }

    override fun onStop() {
        super.onStop()
        cameraV.stop()
    }

    private fun launchApp(packageName: String) {

        val pm = applicationContext.packageManager

        val intent: Intent? = pm.getLaunchIntentForPackage(packageName)

        intent?.addCategory(Intent.CATEGORY_LAUNCHER)

        if (intent != null) {
            applicationContext.startActivity(intent)
        } else {
            Log.i("INTENT", "INTENT is null")
        }
    }
}
