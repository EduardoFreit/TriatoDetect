package com.br.triatodetect.ui.camera

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.media.Image
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.br.triatodetect.databinding.ActivityCameraBinding
import com.br.triatodetect.models.Imagem
import com.br.triatodetect.models.User
import com.br.triatodetect.ui.home.HomeActivity
import com.br.triatodetect.utils.SessionManager
import com.br.triatodetect.utils.Utils
import com.google.android.gms.tasks.Task
import com.google.firebase.ml.modeldownloader.CustomModel
import com.google.firebase.ml.modeldownloader.CustomModelDownloadConditions
import com.google.firebase.ml.modeldownloader.DownloadType
import com.google.firebase.ml.modeldownloader.FirebaseModelDownloader
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class CameraActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityCameraBinding
    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var sessionManager: SessionManager
    private lateinit var bitmap: Bitmap
    private var user: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityCameraBinding.inflate(layoutInflater)

        sessionManager = SessionManager.getInstance(applicationContext)
        this.user = sessionManager.getUserData()

        supportActionBar?.hide()
        setContentView(viewBinding.root)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        viewBinding.floatButtonCamera.setOnClickListener { takePhoto() }

        viewBinding.floatCloseCamera.setOnClickListener { closeCamera() }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun takePhoto() {
        val imageCapture = imageCapture

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            @ExperimentalGetImage object : ImageCapture.OnImageCapturedCallback() {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    //Salvando Imagem CloudStore
                    imageProxy.image?.let { image: Image ->
                        //Utils.saveImage(image, user)
                        val buffer: ByteBuffer = image.planes[0].buffer
                        val bytes = ByteArray(buffer.remaining())
                        buffer.get(bytes)
                        bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        //bitmap.copyPixelsFromBuffer(buffer)
                        val conditions = CustomModelDownloadConditions.Builder()
                            .requireWifi()
                            .build()
                        FirebaseModelDownloader.getInstance()
                            .getModel("triatodetect", DownloadType.LOCAL_MODEL, conditions)
                            .addOnCompleteListener { model: Task<CustomModel> ->
                                // Download complete. Depending on your app, you could enable the ML
                                // feature, or switch from the local model to the remote model, etc.val modelFile = model?.file
                                val modelFile = model.result.file
                                var interpreter: Interpreter? = null;
                                if (modelFile != null) {
                                    interpreter = Interpreter(modelFile)
                                }
                                // val imageBitmap: Bitmap? = imageProxyToBitmap(imageProxy, image)
                                //Estou perdendo a referencia do Bitmap
                                val bitmap = Bitmap.createScaledBitmap(bitmap, 640, 640, true)
                                val input = ByteBuffer.allocateDirect(640*640*3*4).order(ByteOrder.nativeOrder())
                                for (y in 0 until 640) {
                                    for (x in 0 until 640) {
                                        val px = bitmap.getPixel(x, y)

                                        // Get channel values from the pixel value.
                                        val r = Color.red(px)
                                        val g = Color.green(px)
                                        val b = Color.blue(px)

                                        // Normalize channel values to [-1.0, 1.0]. This requirement depends on the model.
                                        // For example, some models might require values to be normalized to the range
                                        // [0.0, 1.0] instead.
                                        val rf = (r - 127) / 255f
                                        val gf = (g - 127) / 255f
                                        val bf = (b - 127) / 255f

                                        input.putFloat(rf)
                                        input.putFloat(gf)
                                        input.putFloat(bf)
                                    }
                                }

                                val bufferSize = 5 * java.lang.Float.SIZE / java.lang.Byte.SIZE
                                val modelOutput = ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.nativeOrder())

                                interpreter?.run(input, modelOutput)

                                modelOutput.rewind()
                                val probabilities = modelOutput.asFloatBuffer()
                                try {
                                    for (i in 0 until probabilities.capacity()) {
                                        val probability = probabilities.get(i)
                                        println("$probability")
                                    }
                                } catch (e: java.lang.Exception) {
                                    // File not found?
                                }

                            }
                            .addOnFailureListener {
                                println(it.message)
                            }



                        Utils.classify(imageProxy, image, bytes)
                    }
                    //Fazendo a classificação da imagem
                }
            }
        )
    }
    private fun closeCamera() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
    }

    private fun saveRegisterImageDB(imageName: String) {
        val rowImage = Imagem(imageName, user?.email);

        Utils.insertNewObject(rowImage, "Images")
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private const val IMAGE_EXTENSION = ".jpg"

        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }
}