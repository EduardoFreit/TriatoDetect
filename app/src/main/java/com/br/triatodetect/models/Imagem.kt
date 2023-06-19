package com.br.triatodetect.models

class Imagem(
    val imageName: String? = null,
    val email: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val status: StatusImagem = StatusImagem.PENDENTE,
    val label: String? = null,
    val score: String? = null
)
