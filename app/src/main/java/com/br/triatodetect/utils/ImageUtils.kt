package com.br.triatodetect.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.Image
import android.net.Uri
import androidx.core.graphics.scale
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

// Utilitários para manipulação de imagens
object ImageUtils {

    private var imageByteArray: ByteArray? = null

    fun setImageByteArray(image: Image, degrees: Int) {
        imageByteArray = this.processImage(image, degrees)
    }

    fun getImageByteArray(): ByteArray? {
        return imageByteArray
    }

    fun resetImageByteArray() {
        imageByteArray = null
    }

    private fun processImage(image: Image, degrees: Int): ByteArray {
        val buffer: ByteBuffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return this.rotateByteArrayImage(bytes, degrees)
    }

    private fun rotateByteArrayImage(
        imageData: ByteArray,
        degrees: Int
    ): ByteArray {
        val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)

        val matrix = Matrix()
        matrix.postRotate(degrees.toFloat())
        val rotatedBitmap =
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

        val outputStream = ByteArrayOutputStream()
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, Constants.IMAGE_QUALITY, outputStream)

        return outputStream.toByteArray()
    }

    fun resizeAndCompressImage(bitmap: Bitmap): ByteArray {
        // Calcula o novo tamanho mantendo a proporção
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height
        val aspectRatio = originalWidth.toFloat() / originalHeight.toFloat()

        val newWidth: Int
        val newHeight: Int

        if (originalWidth > originalHeight) {
            newWidth = Constants.IMAGE_SAVE_SIZE
            newHeight = (Constants.IMAGE_SAVE_SIZE / aspectRatio).toInt()
        } else {
            newHeight = Constants.IMAGE_SAVE_SIZE
            newWidth = (Constants.IMAGE_SAVE_SIZE * aspectRatio).toInt()
        }

        // Redimensiona a imagem
        val resizedBitmap = bitmap.scale(newWidth, newHeight, true)

        // Comprime a imagem
        val outputStream = ByteArrayOutputStream()
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, Constants.IMAGE_QUALITY, outputStream)

        return outputStream.toByteArray()
    }

    fun setUriByteArray(uri: Uri, context: Context) {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bitmapUri: Bitmap = BitmapFactory.decodeStream(inputStream)

        // Redimensiona e comprime a imagem antes de armazenar
        imageByteArray = resizeAndCompressImage(bitmapUri)
    }


}