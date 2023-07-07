package com.br.triatodetect.models

import java.util.Date

class Img(
    val imageName: String? = null,
    val email: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val status: StatusImage = StatusImage.PENDENTE,
    val label: String? = null,
    val score: Double? = null,
)
