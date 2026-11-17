
package br.com.bugnacio.techos.service;

import br.com.bugnacio.techos.model.Cliente;
import br.com.bugnacio.techos.repository.ClienteRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository repo;

    @InjectMocks
    private ClienteService service;

    private Cliente criarClienteValido() {
        Cliente cliente = new Cliente();
        cliente.setNome("Ana Silva");
        cliente.setTelefone("11999999999");
        cliente.setEmail("ana@email.com");
        return cliente;
    }

    @Test
    void deveListarClientesOrdenados() {

        when(repo.findAll(Sort.by("nome")))
                .thenReturn(List.of(criarClienteValido()));

        List<Cliente> clientes = service.listar(null);

        assertEquals(1, clientes.size());
        verify(repo).findAll(Sort.by("nome"));
    }

    @Test
    void devePesquisarClientePorNome() {

        when(repo.findByNomeContainingIgnoreCase("Ana"))
                .thenReturn(List.of(criarClienteValido()));

        List<Cliente> resultado =
                service.listar(" Ana ");

        assertEquals(1, resultado.size());

        verify(repo).findByNomeContainingIgnoreCase(
                "Ana"
        );
    }

    @Test
    void deveCadastrarClienteValido() {

        Cliente cliente = criarClienteValido();

        when(repo.save(cliente)).thenReturn(cliente);

        Cliente salvo = service.cadastrar(cliente);

        assertSame(cliente, salvo);
        verify(repo).save(cliente);
    }

    @Test
    void naoDeveCadastrarClienteComId() {

        Cliente cliente = criarClienteValido();
        cliente.setId(5L);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.cadastrar(cliente)
        );

        verify(repo, never()).save(any(Cliente.class));
    }

    @Test
    void naoDeveCadastrarTelefoneDuplicado() {

        Cliente cliente = criarClienteValido();

        when(repo.existsByTelefone("11999999999"))
                .thenReturn(true);

        assertThrows(
                ClienteService.TelefoneDuplicadoException.class,
                () -> service.cadastrar(cliente)
        );

        verify(repo, never()).save(any(Cliente.class));
    }

    @Test
    void devePreservarDataOriginalNaEdicao() {

        Cliente existente = criarClienteValido();
        existente.setId(1L);

        LocalDateTime dataOriginal =
                LocalDateTime.of(2026, 1, 10, 10, 0);

        existente.setDataCadastro(dataOriginal);

        Cliente alterado = criarClienteValido();
        alterado.setId(1L);
        alterado.setNome("Ana Atualizada");

        alterado.setDataCadastro(
                LocalDateTime.of(2000, 1, 1, 0, 0)
        );

        when(repo.findById(1L))
                .thenReturn(Optional.of(existente));

        when(repo.save(existente))
                .thenReturn(existente);

        Cliente resultado =
                service.atualizar(1L, alterado);

        assertEquals(
                "Ana Atualizada",
                resultado.getNome()
        );

        assertEquals(
                dataOriginal,
                resultado.getDataCadastro()
        );

        verify(repo).save(same(existente));
    }

    @Test
    void naoDeveAtualizarClienteInexistente() {

        Cliente cliente = criarClienteValido();

        when(repo.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ClienteService.ClienteNaoEncontradoException.class,
                () -> service.atualizar(99L, cliente)
        );

        verify(repo, never()).save(any(Cliente.class));
    }

    @Test
    void naoDeveAtualizarTelefoneDuplicado() {

        Cliente existente = criarClienteValido();
        existente.setId(1L);

        Cliente alterado = criarClienteValido();
        alterado.setId(1L);
        alterado.setTelefone("11888888888");

        when(repo.findById(1L))
                .thenReturn(Optional.of(existente));

        when(repo.existsByTelefoneAndIdNot(
                "11888888888", 1L
        )).thenReturn(true);

        assertThrows(
                ClienteService.TelefoneDuplicadoException.class,
                () -> service.atualizar(1L, alterado)
        );

        verify(repo, never()).save(any(Cliente.class));
    }

    @Test
    void deveExcluirClienteExistente() {

        Cliente cliente = criarClienteValido();
        cliente.setId(1L);

        when(repo.findById(1L))
                .thenReturn(Optional.of(cliente));

        service.excluir(1L);

        verify(repo).delete(cliente);
        verify(repo).flush();
    }

    @Test
    void naoDeveExcluirClienteInexistente() {

        when(repo.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ClienteService.ClienteNaoEncontradoException.class,
                () -> service.excluir(99L)
        );

        verify(repo, never()).delete(any(Cliente.class));
    }
}
