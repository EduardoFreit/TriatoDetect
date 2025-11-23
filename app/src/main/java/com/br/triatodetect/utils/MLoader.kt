package com.br.triatodetect.utils

import android.content.Context
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

// Utilitário para carregar o modelo TensorFlow Lite
object MLoader {
    @Throws(Exception::class)
    fun loadModelFile(context: Context): MappedByteBuffer {
        // Abre o arquivo do modelo dentro da pasta "assets"
        val fileDescriptor = context.assets.openFd(Constants.PATH_MODEL)

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
}