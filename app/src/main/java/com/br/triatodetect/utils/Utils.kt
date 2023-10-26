package com.br.triatodetect.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.icu.text.SimpleDateFormat
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.media.Image
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.br.triatodetect.R
import com.br.triatodetect.models.Img
import com.br.triatodetect.models.StatusImage
import com.br.triatodetect.models.User
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.functions.FirebaseFunctions
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
import java.io.IOException
import java.nio.ByteBuffer
import java.util.Locale

object Utils {

    private val db = Firebase.firestore
    private val storage = Firebase.storage
    lateinit var storageRef: StorageReference
    private const val threshold: Float = 0.65f
    private const val maxResults: Int = 1
    private const val numThreads: Int = 5
    private const val pathModel: String = "model/model_detection_triatominies_float32.tflite"
    private const val IMAGE_EXTENSION = ".jpg"
    private var imageClassifier: ImageClassifier? = null
    private var imageByteArray: ByteArray? = null
    var result: MutableList<String> = ArrayList()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    var args: Bundle? = null
    private val functions = FirebaseFunctions.getInstance()
    fun checkPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) != PackageManager.PERMISSION_GRANTED
    }

    private fun insertNewObject(obj: Any, collection: String, callback: (Boolean) -> Unit) {
        storage.reference
        FirebaseAuth.getInstance()
        db.collection(collection)
            .add(obj)
            .addOnSuccessListener { documentReference ->
                callback(true)
                Log.d("Insert", "DocumentSnapshot added with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                callback(false)
                Log.e("Insert", "Error adding image", e)
            }
    }

    fun listImagesUser(email: String?, collection: String, callback: (Array<Img>) -> Unit) {
        val result = mutableListOf<Img>()
        db.collection(collection)
            .whereEqualTo("email", email)
            .orderBy("date", Query.Direction.DESCENDING)
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

    fun listDocuments(collection: String, callback: (Array<Img>) -> Unit) {
        val result = mutableListOf<Img>()
        db.collection(collection)
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

    private fun rotateByteArrayImage(
        imageData: ByteArray,
        degrees: Int
    ): ByteArray {
        // Convert ByteArray to Bitmap
        var bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
        //Reduzir resolução da imagem - (melhorar desempenho)
        bitmap = Bitmap.createScaledBitmap(
            bitmap,
            640,
            640,
            true
        )
        // Perform rotation on the Bitmap
        val matrix = Matrix()
        matrix.postRotate(degrees.toFloat())
        val rotatedBitmap =
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

        // Convert Bitmap to ByteArray
        val outputStream = ByteArrayOutputStream()
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)

        return outputStream.toByteArray()
    }

    private fun processImage(image: Image, degrees: Int): ByteArray {
        val buffer: ByteBuffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return this.rotateByteArrayImage(bytes, degrees)
    }

    fun setImageByteArray(image: Image, degrees: Int) {
        imageByteArray = this.processImage(image, degrees)
    }

    fun setUriByteArray(uri: Uri, context: Context) {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bitmapUri: Bitmap = BitmapFactory.decodeStream(inputStream)
        val outputStream = ByteArrayOutputStream()
        bitmapUri.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        imageByteArray = this.rotateByteArrayImage(outputStream.toByteArray(), 0)
    }

    fun getImageByteArray(): ByteArray? {
        return imageByteArray
    }

    fun resetImageByteArray() {
        imageByteArray = null
    }

    private fun saveImageFirestore(image: ByteArray, user: User?, imageName: String, context: Context, callback: (Boolean) -> Unit) {
        //Salvando imagem no Firestore(DB)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        if (!(ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
                    )
        ) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        val rowImage = Img(
                            imageName, user?.email,
                            location.latitude, location.longitude,
                            StatusImage.AGUARDANDO_CONFIRMACAO,
                            result[0],
                            result[1].toDouble()
                        )
                        this.insertNewObject(rowImage, "Images") { result ->
                            if(result) {
                                this.sendEmailClassification(context, rowImage, image)
                            }
                            callback(result)
                        }
                    } else {
                        callback(false)
                        Log.e("Insert", "User location not identified")
                    }
                }.addOnFailureListener { e ->
                    callback(false)
                    Log.e("Insert", "Error adding image", e)
                }
        }
    }

    private fun saveImageStores(image: ByteArray, user: User?, context: Context, callback: (Boolean) -> Unit) {
        val currentTime: String = System.currentTimeMillis().toString()
        val imageName = "${currentTime}${IMAGE_EXTENSION}"

        //salvando imagem no CloudStore
        user?.email?.let { email: String ->
            storageRef = storage.reference
            val insectImagesRef: StorageReference = storageRef
                .child("Images/${email}/${imageName}")

            val uploadTask: UploadTask = insectImagesRef.putBytes(image)
            uploadTask.addOnFailureListener { e ->
                callback(false)
                Log.e("Insert", "Error adding image", e)
            }.addOnSuccessListener { taskSnapshot ->
                taskSnapshot.metadata?.reference
                Log.d("Insert", "Image added with referece: ${taskSnapshot.metadata?.reference}")
                this.saveImageFirestore(image, user, imageName, context) { result ->
                    if(!result) {
                        val storageReference = taskSnapshot.metadata?.reference
                        storageReference?.delete()
                    }
                    callback(result)
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

    fun classify(context: Context, bytes: ByteArray, user: User?, callback: (Boolean) -> Unit) {
        result.clear()
        if (imageClassifier == null) {
            setupImageClassifier(context)
        }

        val byteArrayGrey = this.imageByteArrayGrey(bytes,
            -20f,1.55f)

        val bitmap: Bitmap = BitmapFactory.decodeByteArray(byteArrayGrey, 0, byteArrayGrey.size)

        val imageProcessor =
            ImageProcessor.Builder()
                .build()

        val tensorImage = imageProcessor.process(TensorImage.fromBitmap(bitmap))

        val imageProcessingOptions = ImageProcessingOptions.builder().build()

        val classifications = imageClassifier?.classify(tensorImage, imageProcessingOptions)

        if (!classifications.isNullOrEmpty() && !classifications[0].categories.isNullOrEmpty()) {
            result.add(classifications[0].categories[0].label.take(2))
            result.add(classifications[0].categories[0].score.toString())
            this.saveImageStores(bytes, user, context) { result ->
                callback(result)
            }
        } else if(!classifications.isNullOrEmpty() && classifications[0].categories.isNullOrEmpty()) {
            result.add("un")
            result.add("1.0")
            this.saveImageStores(bytes, user, context) { result ->
                callback(result)
            }
        } else {
            callback(false)
        }
    }

    fun retrieveImage(user: User, image: Img, callback: (ByteArray?) -> Unit) {
        user.email?.let { email: String ->
            storageRef = storage.reference
            val insectImagesRef: StorageReference = storageRef
                .child("Images/${email}/${image.imageName}")

            val oneMegabyteMax: Long = 1024 * 1024
            insectImagesRef.getBytes(oneMegabyteMax)
                .addOnSuccessListener { bytes ->
                    callback(bytes)
                }
                .addOnFailureListener { _ ->
                    callback(null)
                }
        }
    }


    @Suppress("DEPRECATION")
    fun getCityAndStateFromLocation(context: Context, latitude: Double, longitude: Double): String? {
        val geocoder = Geocoder(context, Locale.getDefault())
        var city = ""
        var state = ""

        try {
            val addresses: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                city = address.locality ?: ""
                if(city.isBlank()) {
                    city = address.subAdminArea ?: ""
                }
                state = address.adminArea ?: ""
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return if (city.isNotEmpty() && state.isNotEmpty()) {
            "$city - $state"
        } else {
            null
        }
    }

    private fun sendEmailClassification(context: Context, rowImage: Img, image: ByteArray) {
        if(!rowImage.label.equals("un")) {
            val cidadeEstado =
                rowImage.latitude?.let { latitude ->
                    rowImage.longitude?.let { longitude ->
                        this.getCityAndStateFromLocation(context,
                            latitude, longitude
                        )
                    }
                }
            val horaData = SimpleDateFormat("dd/MM/yyyy - HH:mm").format(rowImage.date)
            val classify: String = when (rowImage.label) {
                "tb" -> context.getString(R.string.tb)
                "tp" -> context.getString(R.string.tp)
                "pm" -> context.getString(R.string.pm)
                "pl" -> context.getString(R.string.pl)
                else -> context.getString(R.string.un)
            }
            val imageBase64 = Base64.encodeToString(image, Base64.DEFAULT)
            val subject = "TriatoDetect - Novo Triatomíneo Identificado"
            val linkGoogleMap = "https://maps.google.com/?q=${rowImage.latitude},${rowImage.longitude}"
            val html = "<p>Foi identificado um triatomineo da espécie <strong>$classify</strong>. Localização: <a href='$linkGoogleMap'>$cidadeEstado</a> / Horário: $horaData</p>"

            val data = hashMapOf(
                "subject" to subject,
                "html" to html,
                "imageBase64" to imageBase64
            )

            functions.getHttpsCallable("sendEmailWithAttachment").call(data)
            .addOnFailureListener {
                Log.e("Email", "Email not sent")
            }.addOnSuccessListener {
                Log.i("Email", "Email successfully sent")
            }
        }
    }

    private fun imageByteArrayGrey(byteArray: ByteArray, brightness: Float, contrast: Float): ByteArray {
        val originalBitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)

        val adjustedBitmap = imageBitmapGrey(originalBitmap, brightness, contrast)

        val outputStream = ByteArrayOutputStream()
        adjustedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        val adjustedByteArray = outputStream.toByteArray()

        originalBitmap.recycle()
        adjustedBitmap.recycle()

        return adjustedByteArray
    }

    private fun imageBitmapGrey(bitmap: Bitmap, brightness: Float, contrast: Float): Bitmap {
        val colorMatrix = ColorMatrix(
            floatArrayOf(
                contrast, 0.0f, 0f, 0f, brightness,
                0.0f, contrast, 0f, 0f, brightness,
                0.0f, 0.0f, contrast, 0f, brightness,
                0f, 0f, 0f, 1f, 0f
            )
        )
        val adjustedBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
        val canvas = Canvas(adjustedBitmap)
        val paint = Paint()
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0.0f, 0.0f, paint)

        colorMatrix.setSaturation(0f)
        val colorMatrixFilter = ColorMatrixColorFilter(colorMatrix)
        paint.colorFilter = colorMatrixFilter
        canvas.drawBitmap(adjustedBitmap, 0f, 0f, paint)

        return adjustedBitmap
    }

    fun showLoading(context: Context, layout: ViewGroup, hideViews: List<View>): ProgressBar {
        val progressBar = ProgressBar(context)
        progressBar.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )

        // Define o padding horizontal em pixels (150dp)
        val paddingInDp = 150f
        val scale = context.resources.displayMetrics.density
        val paddingInPixels = (paddingInDp * scale + 0.5f).toInt()
        progressBar.setPadding(paddingInPixels, 0, paddingInPixels, 0)

        progressBar.setBackgroundResource(R.drawable.loading)
        progressBar.elevation = 4f

        layout.addView(progressBar)

        hideViews.forEach { view: View ->
            view.visibility  = View.GONE
        }

        return progressBar
    }

    fun hideLoading(progressBar: ProgressBar, layout: ViewGroup, showViews: List<View>) {
        showViews.forEach { view: View ->
            view.visibility  = View.VISIBLE
        }
        layout.removeView(progressBar)
    }

}