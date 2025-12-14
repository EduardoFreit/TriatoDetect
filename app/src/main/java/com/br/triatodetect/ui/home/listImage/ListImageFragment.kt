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
import com.br.triatodetect.service.interfaces.IBackendService
import org.koin.android.ext.android.inject

class ListImageFragment : Fragment() {
    private var _binding: FragmentListImageBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager
    private var user: User? = null
    private lateinit var adapter: ImageRecyclerAdapter
    private lateinit var listImageViewModel: ListImageViewModel
    private val backendService: IBackendService by inject()

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
        // Usa ListImageViewModelFactory para criar o ViewModel com parâmetros personalizados
        val viewModelFactory = ListImageViewModelFactory(user!!, backendService)
        listImageViewModel =
            ViewModelProvider(this, viewModelFactory)[ListImageViewModel::class.java]

        // Inicializa o adapter
        adapter = ImageRecyclerAdapter(backendService = backendService)
        binding.listView.layoutManager = LinearLayoutManager(binding.root.context)
        binding.listView.adapter = adapter

        // Observa LiveData e atualiza a lista de imagens
        listImageViewModel.listImage.observe(viewLifecycleOwner) { newList ->
            // No primeiro carregamento, usa updateData para eficiência
            // Em refreshs subsequentes, usa refreshData para garantir ordem correta
            if (adapter.itemCount == 0) {
                adapter.updateData(newList)
            } else {
                adapter.refreshData(newList)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Recarrega a lista quando o fragmento volta a ser visível
        // Isso garante que novas imagens sejam mostradas quando o usuário retorna da captura
        if (::listImageViewModel.isInitialized) {
            listImageViewModel.refreshListImages()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}