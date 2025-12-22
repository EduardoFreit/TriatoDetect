package com.br.triatodetect.ui.home.listImage

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.br.triatodetect.models.Img
import com.br.triatodetect.models.User
import com.br.triatodetect.service.interfaces.IBackendService
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify

class ListImageViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var mockBackendService: IBackendService

    @Mock
    private lateinit var mockObserver: Observer<Array<Img>>

    private lateinit var viewModel: ListImageViewModel
    
    private lateinit var testUser: User

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        testUser = User("Uusário Teste", "test@example.com")
    }

    @Test
    fun `init deve carregar lista de imagens`() {
        val mockImages = arrayOf(
            Img("image1.jpg", "url1", 111.1),
            Img("image2.jpg", "url2", 222.2)
        )

        val callbackCaptor = argumentCaptor<(Array<Img>) -> Unit>()

        viewModel = ListImageViewModel(testUser, mockBackendService)
        
        viewModel.listImage.observeForever(mockObserver)
        verify(mockBackendService).listImagesUser(
            eq(testUser.email),      // Verifica se passou o email correto
            eq("Images"),             // Verifica se passou o nome da pasta correta
            callbackCaptor.capture() // Captura o callback para execução manual
        )
        
        callbackCaptor.firstValue.invoke(mockImages)

        verify(mockObserver).onChanged(mockImages)
    }

    @Test
    fun `refreshListImages deve recarregar as imagens`() {
        val initialImages = arrayOf(Img("image1.jpg", "url1", 111.1))
        val refreshedImages = arrayOf(
            Img("image1.jpg", "url1", 111.1),
            Img("image2.jpg", "url2", 222.2),
            Img("image3.jpg", "url3", 333.3)
        )

        viewModel = ListImageViewModel(testUser, mockBackendService)
        viewModel.listImage.observeForever(mockObserver)

        val callbackCaptor = argumentCaptor<(Array<Img>) -> Unit>()
        
        verify(mockBackendService).listImagesUser(
            eq(testUser.email),
            eq("Images"),
            callbackCaptor.capture()
        )
        
        callbackCaptor.firstValue.invoke(initialImages)
        
        verify(mockObserver).onChanged(initialImages)

        org.mockito.kotlin.clearInvocations(mockObserver)
        
        viewModel.refreshListImages()

        verify(mockBackendService, org.mockito.kotlin.times(2))
            .listImagesUser(eq(testUser.email), eq("Images"), callbackCaptor.capture())
        
        callbackCaptor.lastValue.invoke(refreshedImages)
        
        verify(mockObserver).onChanged(refreshedImages)
    }

    @Test
    fun `listImage LiveData deve estar vazio inicialmente`() {
        viewModel = ListImageViewModel(testUser, mockBackendService)

        assertNull(viewModel.listImage.value)
    }

    @Test
    fun `deve usar o email correto do usuario`() {
        viewModel = ListImageViewModel(testUser, mockBackendService)

        verify(mockBackendService).listImagesUser(
            eq(testUser.email),
            eq("Images"),
            any()
        )
    }

    @Test
    fun `deve atualizar LiveData quando callback retornar lista vazia`() {
        val emptyImages = emptyArray<Img>()
        val callbackCaptor = argumentCaptor<(Array<Img>) -> Unit>()

        viewModel = ListImageViewModel(testUser, mockBackendService)
        viewModel.listImage.observeForever(mockObserver)

        verify(mockBackendService).listImagesUser(
            eq(testUser.email),
            eq("Images"),
            callbackCaptor.capture()
        )
        callbackCaptor.firstValue.invoke(emptyImages)

        verify(mockObserver).onChanged(emptyImages)


        assertEquals(0, viewModel.listImage.value?.size)
    }
}
