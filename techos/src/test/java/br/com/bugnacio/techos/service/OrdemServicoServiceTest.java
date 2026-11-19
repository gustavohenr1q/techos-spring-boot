
package br.com.bugnacio.techos.service;

import br.com.bugnacio.techos.dto.AbrirOrdemServicoRequest;

import br.com.bugnacio.techos.model.Cliente;
import br.com.bugnacio.techos.model.Equipamento;
import br.com.bugnacio.techos.model.OrdemServico;
import br.com.bugnacio.techos.model.StatusOrdemServico;

import br.com.bugnacio.techos.repository.OrdemServicoRepository;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrdemServicoServiceTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @Mock
    private OrdemServicoRepository repository;

    @Mock
    private EquipamentoService equipamentoService;

    private OrdemServicoService service;

    @BeforeAll
    static void iniciarValidator() {

        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void encerrarValidator() {

        factory.close();
    }

    @BeforeEach
    void preparar() {

        service = new OrdemServicoService(
                repository,
                equipamentoService,
                validator
        );
    }

    private Cliente clienteValido() {

        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNome("Cliente Teste");
        cliente.setTelefone("11999999999");

        return cliente;
    }

    private Equipamento equipamentoValido() {

        Equipamento equipamento = new Equipamento();

        equipamento.setId(2L);
        equipamento.setCliente(clienteValido());

        return equipamento;
    }

    private AbrirOrdemServicoRequest requestValido() {

        return new AbrirOrdemServicoRequest(
                2L,
                "Notebook não liga.",
                "Carregador",
                "Marcas de uso",
                LocalDate.now().plusDays(5)
        );
    }

    private void prepararCadastroValido() {

        when(equipamentoService.buscarPorId(2L))
                .thenReturn(equipamentoValido());

        when(repository.save(any(OrdemServico.class)))
                .thenAnswer(
                        invocacao -> invocacao.getArgument(0)
                );
    }

    @Test
    void deveAbrirOrdemValida() {

        prepararCadastroValido();

        OrdemServico ordem = service.abrir(
                requestValido()
        );

        assertNotNull(ordem);
        assertEquals(
                StatusOrdemServico.ABERTA,
                ordem.getStatus()
        );

        assertEquals(1L, ordem.getCliente().getId());
        assertEquals(2L, ordem.getEquipamento().getId());

        assertEquals(
                "Notebook não liga.",
                ordem.getDefeitoRelatado()
        );

        verify(repository).save(ordem);
    }

    @Test
    void deveNormalizarCamposNaAbertura() {

        prepararCadastroValido();

        AbrirOrdemServicoRequest request =
                new AbrirOrdemServicoRequest(
                        2L,
                        "  Não liga  ",
                        "   ",
                        "  Arranhões  ",
                        null
                );

        OrdemServico ordem = service.abrir(request);

        assertEquals(
                "Não liga",
                ordem.getDefeitoRelatado()
        );

        assertNull(ordem.getAcessoriosRecebidos());

        assertEquals(
                "Arranhões",
                ordem.getEstadoConservacao()
        );
    }

    @Test
    void naoDeveAbrirComRequestNulo() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.abrir(null)
        );

        verifyNoInteractions(
                equipamentoService,
                repository
        );
    }

    @Test
    void naoDeveAbrirSemEquipamento() {

        AbrirOrdemServicoRequest request =
                new AbrirOrdemServicoRequest(
                        null,
                        "Não liga",
                        null,
                        null,
                        null
                );

        assertThrows(
                ConstraintViolationException.class,
                () -> service.abrir(request)
        );

        verifyNoInteractions(
                equipamentoService,
                repository
        );
    }

    @Test
    void naoDeveAbrirComEquipamentoIdInvalido() {

        AbrirOrdemServicoRequest request =
                new AbrirOrdemServicoRequest(
                        0L,
                        "Não liga",
                        null,
                        null,
                        null
                );

        assertThrows(
                ConstraintViolationException.class,
                () -> service.abrir(request)
        );
    }

    @Test
    void naoDeveAbrirSemDefeito() {

        AbrirOrdemServicoRequest request =
                new AbrirOrdemServicoRequest(
                        2L,
                        "   ",
                        null,
                        null,
                        null
                );

        assertThrows(
                ConstraintViolationException.class,
                () -> service.abrir(request)
        );
    }

    @Test
    void naoDeveAbrirComDefeitoMuitoLongo() {

        AbrirOrdemServicoRequest request =
                new AbrirOrdemServicoRequest(
                        2L,
                        "A".repeat(4001),
                        null,
                        null,
                        null
                );

        assertThrows(
                ConstraintViolationException.class,
                () -> service.abrir(request)
        );
    }

    @Test
    void naoDeveAbrirComAcessoriosMuitoLongos() {

        AbrirOrdemServicoRequest request =
                new AbrirOrdemServicoRequest(
                        2L,
                        "Não liga",
                        "A".repeat(2001),
                        null,
                        null
                );

        assertThrows(
                ConstraintViolationException.class,
                () -> service.abrir(request)
        );
    }

    @Test
    void naoDeveAbrirComPrevisaoPassada() {

        AbrirOrdemServicoRequest request =
                new AbrirOrdemServicoRequest(
                        2L,
                        "Não liga",
                        null,
                        null,
                        LocalDate.now().minusDays(2)
                );

        assertThrows(
                ConstraintViolationException.class,
                () -> service.abrir(request)
        );
    }

    @Test
    void naoDeveAbrirParaEquipamentoInexistente() {

        when(equipamentoService.buscarPorId(99L))
                .thenThrow(
                        new EquipamentoService
                                .EquipamentoNaoEncontradoException(99L)
                );

        AbrirOrdemServicoRequest request =
                new AbrirOrdemServicoRequest(
                        99L,
                        "Não liga",
                        null,
                        null,
                        null
                );

        assertThrows(
                EquipamentoService
                        .EquipamentoNaoEncontradoException.class,
                () -> service.abrir(request)
        );

        verify(repository, never()).save(any());
    }

    @Test
    void naoDeveAbrirSemClienteVinculado() {

        Equipamento equipamento = new Equipamento();
        equipamento.setId(2L);

        when(equipamentoService.buscarPorId(2L))
                .thenReturn(equipamento);

        assertThrows(
                IllegalStateException.class,
                () -> service.abrir(requestValido())
        );

        verify(repository, never()).save(any());
    }

    @Test
    void deveBuscarOrdemPorId() {

        OrdemServico ordem = new OrdemServico();
        ordem.setId(10L);

        when(repository.findById(10L))
                .thenReturn(Optional.of(ordem));

        OrdemServico encontrada = service.buscarPorId(10L);

        assertSame(ordem, encontrada);
    }

    @Test
    void deveInformarOrdemInexistente() {

        when(repository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                OrdemServicoService
                        .OrdemServicoNaoEncontradaException.class,
                () -> service.buscarPorId(99L)
        );
    }

    @Test
    void naoDeveBuscarOrdemComIdNulo() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.buscarPorId(null)
        );
    }

    @Test
    void devePesquisarComPaginacaoEStatus() {

        when(repository.pesquisar(
                eq("Dell"),
                eq(StatusOrdemServico.ABERTA),
                any(Pageable.class)
        )).thenReturn(Page.empty());

        Page<OrdemServico> pagina = service.listar(
                "  Dell  ",
                StatusOrdemServico.ABERTA,
                0,
                10
        );

        assertTrue(pagina.isEmpty());

        ArgumentCaptor<Pageable> captor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(repository).pesquisar(
                eq("Dell"),
                eq(StatusOrdemServico.ABERTA),
                captor.capture()
        );

        Pageable pageable = captor.getValue();

        assertEquals(0, pageable.getPageNumber());
        assertEquals(10, pageable.getPageSize());

        assertEquals(
                Sort.Direction.DESC,
                pageable.getSort()
                        .getOrderFor("criadaEm")
                        .getDirection()
        );
    }

    @Test
    void deveNormalizarBuscaEmBranco() {

        when(repository.pesquisar(
                isNull(),
                isNull(),
                any(Pageable.class)
        )).thenReturn(Page.empty());

        service.listar("   ", null, 0, 20);

        verify(repository).pesquisar(
                isNull(),
                isNull(),
                any(Pageable.class)
        );
    }

    @Test
    void naoDeveAceitarPaginaNegativa() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.listar(
                        null,
                        null,
                        -1,
                        10
                )
        );

        verifyNoInteractions(repository);
    }

    @Test
    void naoDeveAceitarTamanhoDePaginaInvalido() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.listar(
                        null,
                        null,
                        0,
                        51
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.listar(
                        null,
                        null,
                        0,
                        0
                )
        );
    }
}
