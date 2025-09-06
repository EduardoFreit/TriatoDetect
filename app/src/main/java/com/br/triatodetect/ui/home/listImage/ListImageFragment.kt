package com.br.triatodetect.ui.home.listImage

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.br.triatodetect.databinding.FragmentListImageBinding
import com.br.triatodetect.models.User
import com.br.triatodetect.utils.SessionManager

class ListImageFragment : Fragment() {
    private var _binding: FragmentListImageBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager
    private var user: User? = null
    private lateinit var adapter: ImageRecyclerAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListImageBinding.inflate(inflater, container, false)
        sessionManager = SessionManager.getInstance(binding.root.context)
        user = sessionManager.getUserData()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val listImageViewModelFactory = ListImageViewModelFactory(user!!)
        val listImageViewModel =
            ViewModelProvider(this, listImageViewModelFactory)[ListImageViewModel::class.java]

        // Inicializa o adapter
        adapter = ImageRecyclerAdapter()
        binding.listView.layoutManager = LinearLayoutManager(binding.root.context)
        binding.listView.adapter = adapter

        // Observa LiveData e adiciona novas imagens sem duplicar
        listImageViewModel.listImage.observe(viewLifecycleOwner) { newList ->
            adapter.updateData(newList)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}