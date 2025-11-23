package com.br.triatodetect.utils

// Constantes usadas no aplicativo
object Constants {
    const val IMAGE_SAVE_SIZE: Int = 800 // Tamanho para salvar (menor que o original)
    const val IMAGE_QUALITY: Int = 75 // Qualidade de compressão (0-100)
    const val LOWER_THRESHOLD: Float = 0.4f
    const val UPPER_THRESHOLD: Float = 0.75f
    const val PATH_MODEL: String = "model/model_detection_triatominies_float32.tflite"
    const val THRESHOLD: Float = 0.7f
    const val IMAGE_EXTENSION = ".jpg"
    const val IMAGE_RESIZE: Int = 224
}