package com.br.triatodetect.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
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
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.util.Locale
import org.tensorflow.lite.Interpreter;
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

object Utils {

    private val db = Firebase.firestore
    private val storage = Firebase.storage
    lateinit var storageRef: StorageReference
    private const val pathModel: String = "model/model_detection_triatominies_float32.tflite"
    private const val IMAGE_EXTENSION = ".jpg"
    private var imageByteArray: ByteArray? = null
    var result: MutableList<String> = ArrayList()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    var args: Bundle? = null
    private val functions = FirebaseFunctions.getInstance()
    private const val IMAGE_SIZE = 224
    private const val IMAGE_RESIZE = 224
    private const val THRESHOLD: Float = 0.7f
    fun checkPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) != PackageManager.PERMISSION_GRANTED
    }

    private fun insertNewObject(obj: Any, collection: String = "Images", callback: (Boolean) -> Unit) {
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

    private fun rotateByteArrayImage(
        imageData: ByteArray,
        degrees: Int
    ): ByteArray {
        var bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
        bitmap = Bitmap.createScaledBitmap(
            bitmap,
            IMAGE_SIZE,
            IMAGE_SIZE,
            true
        )

        val matrix = Matrix()
        matrix.postRotate(degrees.toFloat())
        val rotatedBitmap =
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

        val outputStream = ByteArrayOutputStream()
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)

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
                        this.insertNewObject(rowImage) { result ->
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

    @Throws(Exception::class)
    private fun loadModelFile(context: Context): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(pathModel)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }


    @Throws(Exception::class)
    private fun classify(context: Context, bitmap: Bitmap) {
        val model = loadModelFile(context)
        val input = preProcessImageClassify(bitmap)
        val interpreter = Interpreter(model)
        val output = Array(1) { FloatArray(3) }

        interpreter.run(input, output)
        val prediction = output[0]
        val maxValue = prediction.maxOrNull() ?: throw IllegalArgumentException("Array is empty")

        if(maxValue < THRESHOLD) {
            result.add("u")
            result.add("1.0")
            return
        }

        when (prediction.toList().indexOf(maxValue)) {
            0 -> result.add("n")
            1 -> result.add("s")
            else -> {result.add("u"); result.add("1.0")}
        }
        result.add(maxValue.toString())
    }
    private fun preProcessImageClassify(bitmap: Bitmap): Array<Array<Array<FloatArray>>> {
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, IMAGE_RESIZE, IMAGE_RESIZE, true)
        val input = Array(1) { Array(IMAGE_RESIZE) { Array(IMAGE_RESIZE) { FloatArray(3) } } }

        for (i in 0 until IMAGE_RESIZE) {
            for (j in 0 until IMAGE_RESIZE) {
                val pixel = scaledBitmap.getPixel(i, j)
                val red = Color.red(pixel)
                val green = Color.green(pixel)
                val blue = Color.blue(pixel)

                val grayscaleValue = (red + green + blue) / 3.0f

                input[0][i][j][0] = grayscaleValue
                input[0][i][j][1] = grayscaleValue
                input[0][i][j][2] = grayscaleValue
            }
        }
        return input
    }


    fun classify(context: Context, bytes: ByteArray, user: User?, callback: (Boolean) -> Unit) {
        try {
            result.clear()

            val bitmap: Bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

            classify(context, bitmap)


            callback(true)
            this.saveImageStores(bytes, user, context) { result ->
                callback(result)
            }

        } catch (e: Exception) {
            e.printStackTrace()
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
        if(rowImage.label.equals("s")) {
            val cidadeEstado =
                rowImage.latitude?.let { latitude ->
                    rowImage.longitude?.let { longitude ->
                        this.getCityAndStateFromLocation(context,
                            latitude, longitude
                        )
                    }
                }
            val horaData = SimpleDateFormat("dd/MM/yyyy - HH:mm").format(rowImage.date)
            val imageBase64 = Base64.encodeToString(image, Base64.DEFAULT)
            val subject = "TriatoDetect - Novo Inseto Transmissor"
            val linkGoogleMap = "https://maps.google.com/?q=${rowImage.latitude},${rowImage.longitude}"
            val html = "<p>Foi identificado um novo inseto transmissor. Localização: <a href='$linkGoogleMap'>$cidadeEstado</a> / Horário: $horaData</p>"

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