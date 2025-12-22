package com.br.triatodetect.di

import com.br.triatodetect.models.User
import com.br.triatodetect.service.interfaces.IAuthService
import com.br.triatodetect.service.interfaces.IBackendService
import com.br.triatodetect.service.interfaces.IClassifyService
import com.br.triatodetect.ui.home.listImage.ListImageViewModel
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class AppModuleTest : KoinTest {

    @Mock
    private lateinit var mockBackendService: IBackendService

    @Mock
    private lateinit var mockClassifyService: IClassifyService

    @Mock
    private lateinit var mockAuthService: IAuthService

    private val testModule = module {
        single<IBackendService> { mockBackendService }
        
        single<IClassifyService> { mockClassifyService }
        
        single<IAuthService>(qualifier = named("google")) { mockAuthService }
        
        viewModel { (user: User) ->
            ListImageViewModel(user, get())
        }
    }

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        startKoin {
            modules(testModule)
        }
    }

    @After
    fun tearDown() {
        // Para o Koin e limpa todas as dependências
        // Isso garante que cada teste é isolado
        stopKoin()
    }

    @Test
    fun `deve injetar IBackendService`() {
        val backendService: IBackendService by inject()

        assertNotNull(backendService)
    }

    @Test
    fun `deve injetar IClassifyService`() {
        val classifyService: IClassifyService by inject()

        assertNotNull(classifyService)
    }

    @Test
    fun `deve injetar IAuthService com qualifier google`() {
        val authService: IAuthService by inject(named("google"))

        assertNotNull(authService)
    }

    @Test
    fun `deve criar ListImageViewModel com parametros`() {
        // Cria um usuário de teste
        val usuarioTeste = User("Usuário teste", "test@example.com")
        
        // Injeta o backendService do Koin
        val backendService: IBackendService by inject()

        // Cria o ViewModel manualmente passando as dependências
        val viewModel = ListImageViewModel(usuarioTeste, backendService)

        // Verifica que o ViewModel foi criado corretamente
        assertNotNull(viewModel)
        assertNotNull(viewModel.listImage)
    }

    @Test
    fun `BackendService deve ser singleton`() {
        val service1: IBackendService by inject()
        val service2: IBackendService by inject()

        assertSame("BackendService deve retornar a mesma instância", service1, service2)
    }

    @Test
    fun `ClassifyService deve ser singleton`() {
        val service1: IClassifyService by inject()
        val service2: IClassifyService by inject()

        assertSame("ClassifyService deve retornar a mesma instância", service1, service2)
    }

    @Test
    fun `koin deve criar ListImageViewModel com parametros`() {
        val userEmail = "specific@test.com"
        val user = User("User Testes", userEmail)

        val viewModel: ListImageViewModel by inject {
            parametersOf(user)
        }

        assertNotNull(viewModel)
    }

}
