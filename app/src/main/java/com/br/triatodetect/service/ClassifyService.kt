package com.br.triatodetect.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.core.graphics.get
import androidx.core.graphics.scale
import com.br.triatodetect.models.User
import com.br.triatodetect.service.interfaces.IClassifyService
import com.br.triatodetect.utils.Constants
import com.br.triatodetect.utils.ImageUtils
import com.br.triatodetect.utils.MLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter


// Serviço para classificação de imagens usando TensorFlow Lite
class ClassifyService: IClassifyService {

    private val firebaseService = FirebaseService()
    private var classifyResult: MutableList<String> = ArrayList()

    private fun preProcessImageClassify(bitmap: Bitmap): Array<Array<Array<FloatArray>>> {
        // Redimensiona a imagem para o tamanho esperado pelo modelo (IMAGE_RESIZE x IMAGE_RESIZE)
        val resizedBitmap = bitmap.scale(Constants.IMAGE_RESIZE, Constants.IMAGE_RESIZE, true)
        // Cria o array de entrada para o modelo:
        // Estrutura: [1 amostra][altura][largura][3 canais RGB]
        val input = Array(1) { Array(Constants.IMAGE_RESIZE) { Array(Constants.IMAGE_RESIZE) { FloatArray(3) } } }
        // Percorre cada pixel da imagem redimensionada
        for (i in 0 until Constants.IMAGE_RESIZE) {
            for (j in 0 until Constants.IMAGE_RESIZE) {
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

    private fun classifyBinary(context: Context, bitmap: Bitmap) {
        val model = MLoader.loadModelFile(context)
        val input = preProcessImageClassify(bitmap)
        val interpreter = Interpreter(model)
        val output = Array(1) { FloatArray(1) } // saída binária (sigmoid)

        interpreter.run(input, output)
        val prediction = output[0][0] // valor entre 0 e 1 do sigmoid


        if (prediction < Constants.LOWER_THRESHOLD) {
            classifyResult.add("u")      // classe negativa
        } else if (prediction > Constants.UPPER_THRESHOLD) {
            classifyResult.add("s")      // classe positiva
        } else {
            classifyResult.add("u")      // indefinido
        }

        classifyResult.add(prediction.toString())
    }

    private fun classifyMultiClass(context: Context, bitmap: Bitmap) {
        // Carrega o modelo previamente mapeado em memória
        val model = MLoader.loadModelFile(context)
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
        if(maxValue < Constants.THRESHOLD) {
            classifyResult.add("u")      // 'u' indicando "Não identificado" ou "unknown"
            classifyResult.add("1.0")    // valor padrão
            return
        }
        // Determina a classe correspondente ao valor máximo
        when (prediction.toList().indexOf(maxValue)) {
            0 -> classifyResult.add("n")  // Inseto não Transmissor
            1 -> classifyResult.add("s")  // Inseto Transmissor
            else -> {             // caso caia fora das classes conhecidas
                classifyResult.add("u")
                classifyResult.add("1.0")
            }
        }
        // Adiciona o valor da confiança (probabilidade) ao resultado
        classifyResult.add(maxValue.toString())
    }

    override suspend fun initClassify(context: Context, bytes: ByteArray, user: User?, callback: (Boolean) -> Unit) {
        // Executa a classificação em uma thread de background
            try {
                // Limpando resultados antigos
                classifyResult.clear()

                // Convertendo a imagem em bytes para Bitmap
                val bitmap: Bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                // Classificação executada em background thread
                classifyBinary(context, bitmap)

                // Redimensiona e comprime a imagem antes de salvar
                val compressedImageBytes = ImageUtils.resizeAndCompressImage(bitmap)

                // Volta para a main thread para executar operações de UI e Firebase
                firebaseService.saveImageStores(compressedImageBytes, user, context, classifyResult) { saveImageStoresResult ->
                    callback(saveImageStoresResult)
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