package com.br.triatodetect.utils

import android.content.Context
import android.content.pm.PackageManager
import android.media.Image
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.google.firebase.storage.ktx.storage
import java.nio.ByteBuffer

object Utils {

    private val db = Firebase.firestore
    private val storage = Firebase.storage
    private lateinit var storageRef: StorageReference
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

    private fun imageToByteArray(image: Image): ByteArray {
        val buffer: ByteBuffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return bytes
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


}