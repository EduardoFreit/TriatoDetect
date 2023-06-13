package com.br.triatodetect.utils

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.camera.core.ImageProxy
import androidx.core.content.ContextCompat
import com.br.triatodetect.models.User
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.ml.modeldownloader.CustomModel
import com.google.firebase.ml.modeldownloader.CustomModelDownloadConditions
import com.google.firebase.ml.modeldownloader.DownloadType
import com.google.firebase.ml.modeldownloader.FirebaseModelDownloader
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.google.firebase.storage.ktx.storage
import org.tensorflow.lite.Interpreter
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object Utils {

    private val db = Firebase.firestore
    private val storage = Firebase.storage
    private lateinit var storageRef: StorageReference
    private const val threshold: Float = 0.65f
    private const val maxResults: Int = 1
    private const val numThreads: Int = 2
    private const val pathModel: String = "model/best_full_integer_quant.tflite"
    private const val IMAGE_EXTENSION = ".jpg"
    fun checkPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
    }

    fun insertNewObject(obj: Any, collection: String) {
        storage.reference
        db.collection(collection)
            .add(obj)
            .addOnSuccessListener { documentReference ->
                Log.d("Insert", "DocumentSnapshot added with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.e("Insert", "Error adding document", e)
            }
    }

    private fun compressImage(imageData: ByteArray, quality: Int): ByteArray {
        val bitmap: Bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        return outputStream.toByteArray()
    }

    private fun imageToByteArray(image: Image): ByteArray {
        val buffer: ByteBuffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return compressImage(bytes, 25)
    }

    fun saveImage(email: String, imageName: String, image: Image) {
        val data: ByteArray = this.imageToByteArray(image)
        storageRef = storage.reference
        val insectImagesRef:StorageReference = storageRef
            .child("Images/${email}/${imageName}")

        var uploadTask: UploadTask = insectImagesRef.putBytes(data)

        uploadTask.addOnFailureListener {e ->
            Log.e("Insert", "Error adding image", e)
        }.addOnSuccessListener { taskSnapshot ->
            taskSnapshot.metadata?.reference
            Log.d("Insert", "Image added with referece: ${taskSnapshot.metadata?.reference}")
        }
    }

    fun saveImage(image: Image, user: User?) {
        val currentTime: String = System.currentTimeMillis().toString()
        val imageName = "${currentTime}${IMAGE_EXTENSION}"
        //salvando imagem no CloudStore
        user?.email?.let {email: String ->
            image?.let { image: Image ->
                val data: ByteArray = this.imageToByteArray(image)
                storageRef = storage.reference
                val insectImagesRef:StorageReference = storageRef
                    .child("Images/${email}/${imageName}")

                var uploadTask: UploadTask = insectImagesRef.putBytes(data)

                uploadTask.addOnFailureListener {e ->
                    Log.e("Insert", "Error adding image", e)
                }.addOnSuccessListener { taskSnapshot ->
                    taskSnapshot.metadata?.reference
                    Log.d("Insert", "Image added with referece: ${taskSnapshot.metadata?.reference}")
                }
            }
        }


    }

    private fun inputStreamToByteBuffer(inputStream: InputStream): ByteBuffer {
        val byteArray = inputStream.readBytes()
        val byteBuffer = ByteBuffer.allocateDirect(byteArray.size)
        byteBuffer.order(ByteOrder.nativeOrder())
        byteBuffer.put(byteArray)
        byteBuffer.flip()
        return byteBuffer
    }

    private fun setupImageClassifier(context: Context) { // Inicializando modelo
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
                println("oi")
            }
            .addOnFailureListener {
                println(it.message)
            }
    }

    fun imageProxyToBitmap(imageProxy: ImageProxy, image: Image?): Bitmap? {
        if(image != null) {
            val buffer = image.planes[0].buffer
            val pixelStride = image.planes[0].pixelStride
            val rowStride = image.planes[0].rowStride
            val width = image.width
            val height = image.height
            val rowPadding = rowStride - pixelStride * width

            val bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888)
            bitmap.copyPixelsFromBuffer(buffer)

            return Bitmap.createBitmap(bitmap, 0, 0, width, height)
        }
        return null;
    }

    fun classify(imageProxy: ImageProxy, image: Image, bytes: ByteArray) {

    }


}