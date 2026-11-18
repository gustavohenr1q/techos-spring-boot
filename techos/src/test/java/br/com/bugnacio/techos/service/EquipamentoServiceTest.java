
package br.com.bugnacio.techos.service;

import br.com.bugnacio.techos.model.Cliente;
import br.com.bugnacio.techos.model.Equipamento;
import br.com.bugnacio.techos.model.TipoEquipamento;

import br.com.bugnacio.techos.repository.ClienteRepository;
import br.com.bugnacio.techos.repository.EquipamentoRepository;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class EquipamentoServiceTest {

    @Mock
    private EquipamentoRepository repo;

    @Mock
    private ClienteRepository clienteRepo;

    private static ValidatorFactory factory;
    private static Validator validator;

    private EquipamentoService service;

    @BeforeAll
    static void iniciarValidador() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void fecharValidador() {
        factory.close();
    }

    @BeforeEach
    void iniciarService() {
        service = new EquipamentoService(
                repo,
                clienteRepo,
                validator
        );
    }

    private Cliente clienteValido() {
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNome("Ana Silva");
        cliente.setTelefone("11999999999");
        return cliente;
    }

    private Equipamento equipamentoValido() {
        Equipamento equipamento = new Equipamento();

        equipamento.setTipo(TipoEquipamento.COMPUTADOR);
        equipamento.setMarca("Dell");
        equipamento.setModelo("OptiPlex");
        equipamento.setNumeroSerie("ABC123");

        return equipamento;
    }

    // LISTAGEM

    @Test
    void deveListarTodosEquipamentos() {
        when(repo.buscar(null, null))
                .thenReturn(List.of(equipamentoValido()));

        List<Equipamento> resultado =
                service.listar("   ", null);

        assertEquals(1, resultado.size());
        verify(repo).buscar(null, null);
    }

    @Test
    void devePesquisarComFiltro() {
        when(repo.buscar("Dell", TipoEquipamento.COMPUTADOR))
                .thenReturn(List.of(equipamentoValido()));

        List<Equipamento> resultado = service.listar(
                " Dell ",
                TipoEquipamento.COMPUTADOR
        );

        assertEquals(1, resultado.size());

        verify(repo).buscar(
                "Dell",
                TipoEquipamento.COMPUTADOR
        );
    }

    @Test
    void deveListarClientesOrdenados() {
        when(clienteRepo.findAll(Sort.by("nome")))
                .thenReturn(List.of(clienteValido()));

        assertEquals(1, service.listarClientes().size());

        verify(clienteRepo).findAll(Sort.by("nome"));
    }

    // BUSCA POR ID

    @Test
    void deveBuscarEquipamentoExistente() {
        Equipamento equipamento = equipamentoValido();
        equipamento.setId(7L);

        when(repo.findById(7L))
                .thenReturn(Optional.of(equipamento));

        assertSame(equipamento, service.buscarPorId(7L));
    }

    @Test
    void naoDeveBuscarEquipamentoInexistente() {
        when(repo.findById(7L))
                .thenReturn(Optional.empty());

        assertThrows(
                EquipamentoService.EquipamentoNaoEncontradoException.class,
                () -> service.buscarPorId(7L)
        );
    }

    // CADASTRO

    @Test
    void deveCadastrarEquipamentoValido() {
        Equipamento equipamento = equipamentoValido();

        equipamento.setMarca("  Dell  ");
        equipamento.setModelo("  Latitude  ");
        equipamento.setNumeroSerie("  ABC123  ");

        when(clienteRepo.findById(1L))
                .thenReturn(Optional.of(clienteValido()));

        when(repo.save(any(Equipamento.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        Equipamento salvo = service.cadastrar(equipamento, 1L);

        assertEquals("Dell", salvo.getMarca());
        assertEquals("Latitude", salvo.getModelo());
        assertEquals("ABC123", salvo.getNumeroSerie());
        assertEquals(1L, salvo.getCliente().getId());

        verify(repo).save(same(equipamento));
    }

    @Test
    void naoDeveCadastrarEquipamentoNulo() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.cadastrar(null, 1L)
        );

        verifyNoInteractions(repo);
    }

    @Test
    void naoDeveCadastrarEquipamentoComId() {
        Equipamento equipamento = equipamentoValido();
        equipamento.setId(5L);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.cadastrar(equipamento, 1L)
        );

        verify(repo, never()).save(any());
    }

    @Test
    void naoDeveCadastrarSemCliente() {
        Equipamento equipamento = equipamentoValido();

        assertThrows(
                IllegalArgumentException.class,
                () -> service.cadastrar(equipamento, null)
        );

        verify(repo, never()).save(any());
    }

    @Test
    void naoDeveCadastrarComClienteInexistente() {
        when(clienteRepo.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                EquipamentoService.ClienteNaoEncontradoException.class,
                () -> service.cadastrar(equipamentoValido(), 99L)
        );

        verify(repo, never()).save(any());
    }

    @Test
    void naoDeveCadastrarMarcaInvalida() {
        Equipamento equipamento = equipamentoValido();
        equipamento.setMarca("   ");

        when(clienteRepo.findById(1L))
                .thenReturn(Optional.of(clienteValido()));

        assertThrows(
                ConstraintViolationException.class,
                () -> service.cadastrar(equipamento, 1L)
        );

        verify(repo, never()).save(any());
    }

    @Test
    void naoDeveCadastrarNumeroSerieDuplicado() {
        when(clienteRepo.findById(1L))
                .thenReturn(Optional.of(clienteValido()));

        when(repo.existsByNumeroSerie("ABC123"))
                .thenReturn(true);

        assertThrows(
                EquipamentoService.NumeroSerieDuplicadoException.class,
                () -> service.cadastrar(equipamentoValido(), 1L)
        );

        verify(repo, never()).save(any());
    }

    // ATUALIZAÇÃO

    @Test
    void deveAtualizarEquipamentoSemTrocarId() {
        Equipamento existente = equipamentoValido();
        existente.setId(7L);

        Equipamento dados = equipamentoValido();
        dados.setId(7L);
        dados.setMarca("Lenovo");
        dados.setModelo("ThinkCentre");

        Cliente cliente = clienteValido();

        when(repo.findById(7L))
                .thenReturn(Optional.of(existente));

        when(clienteRepo.findById(1L))
                .thenReturn(Optional.of(cliente));

        when(repo.save(existente))
                .thenReturn(existente);

        Equipamento atualizado =
                service.atualizar(7L, dados, 1L);

        assertEquals(7L, atualizado.getId());
        assertEquals("Lenovo", atualizado.getMarca());
        assertEquals("ThinkCentre", atualizado.getModelo());
        assertSame(cliente, atualizado.getCliente());

        verify(repo).save(same(existente));
    }

    @Test
    void naoDeveAtualizarComIdDivergente() {
        Equipamento dados = equipamentoValido();
        dados.setId(99L);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.atualizar(7L, dados, 1L)
        );

        verify(repo, never()).save(any());
    }

    @Test
    void naoDeveAtualizarNumeroSerieDuplicado() {
        Equipamento existente = equipamentoValido();
        existente.setId(7L);

        Equipamento dados = equipamentoValido();
        dados.setId(7L);
        dados.setNumeroSerie("XYZ999");

        when(repo.findById(7L))
                .thenReturn(Optional.of(existente));

        when(clienteRepo.findById(1L))
                .thenReturn(Optional.of(clienteValido()));

        when(repo.existsByNumeroSerieAndIdNot("XYZ999", 7L))
                .thenReturn(true);

        assertThrows(
                EquipamentoService.NumeroSerieDuplicadoException.class,
                () -> service.atualizar(7L, dados, 1L)
        );

        verify(repo, never()).save(any());
    }

    @Test
    void naoDeveAtualizarEquipamentoInexistente() {
        Equipamento dados = equipamentoValido();
        dados.setId(7L);

        when(repo.findById(7L))
                .thenReturn(Optional.empty());

        assertThrows(
                EquipamentoService.EquipamentoNaoEncontradoException.class,
                () -> service.atualizar(7L, dados, 1L)
        );

        verify(repo, never()).save(any());
    }

    // EXCLUSÃO

    @Test
    void deveExcluirEquipamentoExistente() {
        Equipamento equipamento = equipamentoValido();
        equipamento.setId(7L);

        when(repo.findById(7L))
                .thenReturn(Optional.of(equipamento));

        service.excluir(7L);

        verify(repo).delete(equipamento);
        verify(repo).flush();
    }

    @Test
    void naoDeveExcluirEquipamentoInexistente() {
        when(repo.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                EquipamentoService.EquipamentoNaoEncontradoException.class,
                () -> service.excluir(99L)
        );

        verify(repo, never()).delete(any());
        verify(repo, never()).flush();
    }
}
