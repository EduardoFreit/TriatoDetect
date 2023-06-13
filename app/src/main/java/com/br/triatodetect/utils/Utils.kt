package com.br.triatodetect.utils

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.Image
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.core.content.ContextCompat
import com.br.triatodetect.models.Imagem
import com.br.triatodetect.models.User
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.google.firebase.storage.ktx.storage
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.core.vision.ImageProcessingOptions
import org.tensorflow.lite.task.vision.classifier.ImageClassifier
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

object Utils {

    private val db = Firebase.firestore
    private val storage = Firebase.storage
    private lateinit var storageRef: StorageReference
    private const val threshold: Float = 0.65f
    private const val maxResults: Int = 1
    private const val numThreads: Int = 5
    private const val pathModel: String = "model/model_detection_triatominies_float32.tflite"
    private const val IMAGE_EXTENSION = ".jpg"
    private var imageClassifier: ImageClassifier? = null

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

    fun imageToByteArray(image: Image): ByteArray {
        val buffer: ByteBuffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return compressImage(bytes, 25)
    }

    fun saveImage(image: ByteArray, user: User?) {
        val currentTime: String = System.currentTimeMillis().toString()
        val imageName = "${currentTime}${IMAGE_EXTENSION}"
        //Salvando imagem no CloudStore
        user?.email?.let {email: String ->
            storageRef = storage.reference
            val insectImagesRef:StorageReference = storageRef
                .child("Images/${email}/${imageName}")

            var uploadTask: UploadTask = insectImagesRef.putBytes(image)
            uploadTask.addOnFailureListener {e ->
                Log.e("Insert", "Error adding image", e)
            }.addOnSuccessListener { taskSnapshot ->
                taskSnapshot.metadata?.reference
                Log.d("Insert", "Image added with referece: ${taskSnapshot.metadata?.reference}")
            }
        }
        //salvando imagem no Firestore(DB)
        val rowImage = Imagem(imageName, user?.email);
        this.insertNewObject(rowImage, "Images")
    }
    private fun setupImageClassifier(context: Context) {
        val optionsBuilder = ImageClassifier.ImageClassifierOptions.builder()
            .setScoreThreshold(threshold)
            .setMaxResults(maxResults)

        val baseOptionsBuilder = BaseOptions.builder().setNumThreads(numThreads)

        optionsBuilder.setBaseOptions(baseOptionsBuilder.build())
        try {
            imageClassifier = ImageClassifier.createFromFileAndOptions(
                context,
                pathModel,
                optionsBuilder.build()
            )
        } catch (e: Exception) {
            e.stackTrace
        }
    }


    fun classify(context: Context, bytes: ByteArray) {
        if (imageClassifier == null) {
            setupImageClassifier(context)
        }
        val bitmap: Bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

        var inferenceTime = SystemClock.uptimeMillis()

        val imageProcessor =
            ImageProcessor.Builder()
                .build()

        val tensorImage = imageProcessor.process(TensorImage.fromBitmap(bitmap))

        val imageProcessingOptions = ImageProcessingOptions.builder().build()

        val results = imageClassifier?.classify(tensorImage, imageProcessingOptions)

        inferenceTime = SystemClock.uptimeMillis() - inferenceTime

        println("oi")
    }


}