package com.br.triatodetect.ui.classify

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.br.triatodetect.databinding.ActivityClassifyBinding
import com.br.triatodetect.utils.Utils

class ClassifyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityClassifyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClassifyBinding.inflate(layoutInflater)

        setContentView(binding.root)

        if(!Utils.result.isNullOrEmpty()) {
            binding.textClassificacaoLabel.text = Utils.result[0]
            binding.textClassificacaoPercent.text = Utils.result[1]
        }

        binding.updateClass.setOnClickListener { updateClassification() }
    }

    private fun updateClassification() {
        if(!Utils.result.isNullOrEmpty()) {
            binding.textClassificacaoLabel.text = Utils.result[0]
            binding.textClassificacaoPercent.text = Utils.result[1]
        }
    }
}