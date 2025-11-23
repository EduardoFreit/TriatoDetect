package com.br.triatodetect.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.app.ActivityCompat
import com.br.triatodetect.models.Img
import com.br.triatodetect.models.StatusImage
import com.br.triatodetect.models.User
import com.br.triatodetect.utils.Constants
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.google.firebase.storage.ktx.storage


// Serviço para interagir com Firebase (Firestore e Storage)
class FirebaseService {

    private val db = Firebase.firestore
    private val storage = Firebase.storage
    lateinit var storageRef: StorageReference
    private lateinit var fusedLocationClient: FusedLocationProviderClient

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

    fun listImages(collection: String, callback: (Array<Img>) -> Unit) {
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

    fun saveImageStores(image: ByteArray, user: User?, context: Context, classifyResult: MutableList<String>, callback: (Boolean) -> Unit) {
        val currentTime: String = System.currentTimeMillis().toString()
        val imageName = "${currentTime}${Constants.IMAGE_EXTENSION}"

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
                saveImageFirestore(image, user, imageName, context, classifyResult) { saveImageFirestoreResult ->
                    if(!saveImageFirestoreResult) {
                        val storageReference = taskSnapshot.metadata?.reference
                        storageReference?.delete()
                    }
                    callback(saveImageFirestoreResult)
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

    private fun saveImageFirestore(image: ByteArray, user: User?, imageName: String, context: Context, classifyResult: MutableList<String>, callback: (Boolean) -> Unit) {
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
                            classifyResult[0],
                            classifyResult[1].toDouble()
                        )
                        insertNewObject(rowImage) { insertNewObjectResult ->
                            if(insertNewObjectResult) {
                                sendEmailClassification(context, rowImage, image)
                            }
                            callback(insertNewObjectResult)
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
}