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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.graphics.get
import androidx.core.graphics.scale

object Utils {

    private val db = Firebase.firestore
    private val storage = Firebase.storage
    lateinit var storageRef: StorageReference
    private const val IMAGE_EXTENSION = ".jpg"
    private var imageByteArray: ByteArray? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    var args: Bundle? = null
    private val functions = FirebaseFunctions.getInstance()
    // Constantes/Variaveis Importante
    private const val pathModel: String = "model/model_detection_triatominies_float32.tflite"
    private const val THRESHOLD: Float = 0.7f
    private const val IMAGE_RESIZE: Int = 224
    var result: MutableList<String> = ArrayList()

    private const val IMAGE_SAVE_SIZE: Int = 800 // Tamanho para salvar (menor que o original)
    private const val IMAGE_QUALITY: Int = 75 // Qualidade de compressão (0-100)
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

    private fun resizeAndCompressImage(bitmap: Bitmap): ByteArray {
        // Calcula o novo tamanho mantendo a proporção
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height
        val aspectRatio = originalWidth.toFloat() / originalHeight.toFloat()
        
        val newWidth: Int
        val newHeight: Int
        
        if (originalWidth > originalHeight) {
            newWidth = IMAGE_SAVE_SIZE
            newHeight = (IMAGE_SAVE_SIZE / aspectRatio).toInt()
        } else {
            newHeight = IMAGE_SAVE_SIZE
            newWidth = (IMAGE_SAVE_SIZE * aspectRatio).toInt()
        }
        
        // Redimensiona a imagem
        val resizedBitmap = bitmap.scale(newWidth, newHeight, true)
        
        // Comprime a imagem
        val outputStream = ByteArrayOutputStream()
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, IMAGE_QUALITY, outputStream)
        
        return outputStream.toByteArray()
    }

    private fun rotateByteArrayImage(
        imageData: ByteArray,
        degrees: Int
    ): ByteArray {
        var bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)

        val matrix = Matrix()
        matrix.postRotate(degrees.toFloat())
        val rotatedBitmap =
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

        val outputStream = ByteArrayOutputStream()
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, IMAGE_QUALITY, outputStream)

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
        
        // Redimensiona e comprime a imagem antes de armazenar
        imageByteArray = resizeAndCompressImage(bitmapUri)
    }

    fun getImageByteArray(): ByteArray? {
        return imageByteArray
    }

    fun resetImageByteArray() {
        imageByteArray = null
    }

    // Carrega o modelo TFLite a partir dos assets e mapeia em memória para uso pelo TensorFlow Lite
    @Throws(Exception::class)
    private fun loadModelFile(context: Context): MappedByteBuffer {
        // Abre o arquivo do modelo dentro da pasta "assets"
        val fileDescriptor = context.assets.openFd(pathModel)

        // Cria um FileInputStream a partir do descritor do arquivo
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)

        // Obtém o canal do arquivo, necessário para mapear o arquivo em memória
        val fileChannel = inputStream.channel

        // Obtém o offset inicial do arquivo (posição onde o modelo começa)
        val startOffset = fileDescriptor.startOffset

        // Obtém o tamanho declarado do arquivo
        val declaredLength = fileDescriptor.declaredLength

        // Mapeia o arquivo em memória (somente leitura) e retorna o MappedByteBuffer
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
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

    // Classifica uma imagem bitmap em múltiplas classes usando o modelo TFLite
    private fun classifyMultiClass(context: Context, bitmap: Bitmap) {
        // Carrega o modelo previamente mapeado em memória
        val model = loadModelFile(context)
        // Pré-processa a imagem para transformar no formato aceito pelo modelo (normalização, redimensionamento etc.)
        val input = preProcessImageClassify(bitmap)
        // Cria o interpretador do TensorFlow Lite com o modelo carregado
        val interpreter = Interpreter(model)
        // Cria um array de saída para armazenar as previsões (1 linha, 3 classes)
        val output = Array(1) { FloatArray(3) }
        // Executa a inferência do modelo com o input e preenche o array de saída
        interpreter.run(input, output)
        // Obtém o array de previsões da primeira (e única) amostra
        val prediction = output[0]
        // Encontra o valor máximo da previsão, que representa a classe mais provável
        val maxValue = prediction.maxOrNull() ?: throw IllegalArgumentException("Array is empty")
        // Se o valor máximo for menor que o limiar definido, considera que não há previsão confiável
        if(maxValue < THRESHOLD) {
            result.add("u")      // 'u' indicando "Não identificado" ou "unknown"
            result.add("1.0")    // valor padrão
            return
        }
        // Determina a classe correspondente ao valor máximo
        when (prediction.toList().indexOf(maxValue)) {
            0 -> result.add("n")  // Inseto não Transmissor
            1 -> result.add("s")  // Inseto Transmissor
            else -> {             // caso caia fora das classes conhecidas
                result.add("u")
                result.add("1.0")
            }
        }
        // Adiciona o valor da confiança (probabilidade) ao resultado
        result.add(maxValue.toString())
    }

    private fun classifyBinary(context: Context, bitmap: Bitmap) {
        val model = loadModelFile(context)
        val input = preProcessImageClassify(bitmap)
        val interpreter = Interpreter(model)
        val output = Array(1) { FloatArray(1) } // saída binária (sigmoid)

        interpreter.run(input, output)
        val prediction = output[0][0] // valor entre 0 e 1 do sigmoid


        if (prediction < LOWER_THRESHOLD) {
            result.add("u")      // classe negativa
        } else if (prediction > UPPER_THRESHOLD) {
            result.add("s")      // classe positiva
        } else {
            result.add("u")      // indefinido
        }

        result.add(prediction.toString())
    }


    private fun preProcessImageClassify(bitmap: Bitmap): Array<Array<Array<FloatArray>>> {
        // Redimensiona a imagem para o tamanho esperado pelo modelo (IMAGE_RESIZE x IMAGE_RESIZE)
        val resizedBitmap = bitmap.scale(IMAGE_RESIZE, IMAGE_RESIZE, true)
        // Cria o array de entrada para o modelo:
        // Estrutura: [1 amostra][altura][largura][3 canais RGB]
        val input = Array(1) { Array(IMAGE_RESIZE) { Array(IMAGE_RESIZE) { FloatArray(3) } } }
        // Percorre cada pixel da imagem redimensionada
        for (i in 0 until IMAGE_RESIZE) {
            for (j in 0 until IMAGE_RESIZE) {
                // Obtém o valor do pixel na posição (i, j)
                val pixel = resizedBitmap[i, j]
                // Extrai os valores de vermelho, verde e azul do pixel
                // e armazena no array de entrada do modelo
                input[0][i][j][0] = Color.red(pixel).toFloat()    // canal vermelho
                input[0][i][j][1] = Color.green(pixel).toFloat()  // canal verde
                input[0][i][j][2] = Color.blue(pixel).toFloat()   // canal azul
            }
        }
        // Retorna o array tridimensional pronto para passar ao interpretador TFLite
        return input
    }

    fun initClassify(context: Context, bytes: ByteArray, user: User?, callback: (Boolean) -> Unit) {
        // Executa a classificação em uma thread de background
        GlobalScope.launch(Dispatchers.IO) {
            try {
                // Limpando resultados antigos
                result.clear()

                // Convertendo a imagem em bytes para Bitmap
                val bitmap: Bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                // Classificação executada em background thread
                classifyMultiClass(context, bitmap)

                // Redimensiona e comprime a imagem antes de salvar
                val compressedImageBytes = resizeAndCompressImage(bitmap)

                // Volta para a main thread para executar operações de UI e Firebase
                withContext(Dispatchers.Main) {
                    saveImageStores(compressedImageBytes, user, context) { result ->
                        callback(result)
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                // Volta para a main thread para executar o callback
                withContext(Dispatchers.Main) {
                    callback(false)
                }
            }
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
        /*if(rowImage.label.equals("s")) {
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
        }*/
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