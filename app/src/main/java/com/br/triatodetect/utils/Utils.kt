package com.br.triatodetect.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.location.Location
import android.media.Image
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.br.triatodetect.models.Img
import com.br.triatodetect.models.StatusImage
import com.br.triatodetect.models.User
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
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
    private var imageByteArray: ByteArray? = null
    var result: MutableList<String> = ArrayList()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private const val reduceImage: Int = 2

    fun checkPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
    }

    private fun insertNewObject(obj: Any, collection: String) {
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

    fun listImagesUser(email: String?, collection: String, callback: (Array<Img>) -> Unit) {
        val result = mutableListOf<Img>()
        db.collection(collection)
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot) {
                    val image = document.toObject(Img::class.java)
                    result.add(image)
                }
                callback(result.toTypedArray())
            }
            .addOnFailureListener { exception ->
                Log.e("List", "Error getting documents.", exception)
                callback(emptyArray())
            }
    }


    private fun rotateByteArrayImage(imageData: ByteArray, degrees: Int, width: Int, height: Int): ByteArray {
        // Convert ByteArray to Bitmap
        var bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
        //Reduzir resolução da imagem - (melhorar desempenho)
        bitmap = Bitmap.createScaledBitmap(bitmap,
            width/reduceImage,
            height/reduceImage,
            true)
        // Perform rotation on the Bitmap
        val matrix = Matrix()
        matrix.postRotate(degrees.toFloat())
        val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

        // Convert Bitmap to ByteArray
        val outputStream = ByteArrayOutputStream()
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)

        return outputStream.toByteArray()
    }

    private fun processImage(image: Image, degrees: Int): ByteArray {
        val buffer: ByteBuffer = image.planes[0].buffer
        var bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return rotateByteArrayImage(bytes, degrees, image.width, image.height)
    }

    fun setImageByteArray(image: Image, degrees: Int) {
        imageByteArray = processImage(image, degrees)
    }

    fun getImageByteArray(): ByteArray? {
       return imageByteArray
    }

    fun resetImageByteArray() {
        imageByteArray = null
    }

    private fun saveImageStores(image: ByteArray, user: User?, context: Context) {
        val currentTime: String = System.currentTimeMillis().toString()
        val imageName = "${currentTime}${IMAGE_EXTENSION}"

        //salvando imagem no Firestore(DB)
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

        //Salvando imagem no CloudStore
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        if (!(ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
                    )) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location : Location? ->
                    if(location != null) {
                        val rowImage = Img(
                            imageName, user?.email,
                            location.latitude, location.longitude,
                            StatusImage.AGUARDANDO_CONFIRMACAO,
                            result[0],
                            result[1].toDouble()
                        )
                        this.insertNewObject(rowImage, "Images")
                    }
                }
        }
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


    fun classify(context: Context, bytes: ByteArray, user:User?) {
        result.clear()
        if (imageClassifier == null) {
            setupImageClassifier(context)
        }
        val bitmap: Bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

        val imageProcessor =
            ImageProcessor.Builder()
                .build()

        val tensorImage = imageProcessor.process(TensorImage.fromBitmap(bitmap))

        val imageProcessingOptions = ImageProcessingOptions.builder().build()

        val classifications = imageClassifier?.classify(tensorImage, imageProcessingOptions)

        if(!classifications.isNullOrEmpty()) {
            result.add(classifications[0].categories[0].label)
            result.add(classifications[0].categories[0].score.toString())
            this.saveImageStores(bytes, user, context)
        }
    }

}