
package br.com.bugnacio.techos.controller;

import br.com.bugnacio.techos.model.Cliente;
import br.com.bugnacio.techos.service.ClienteService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class ClienteControllerTest {

    @Mock
    private ClienteService service;

    @Mock
    private RedirectAttributes redirect;

    @InjectMocks
    private ClienteController controller;

    private Cliente clienteValido() {
        Cliente cliente = new Cliente();

        cliente.setNome("Ana Silva");
        cliente.setTelefone("11999999999");
        cliente.setEmail("ana@email.com");

        return cliente;
    }

    private BindingResult resultado(Cliente cliente) {
        return new BeanPropertyBindingResult(
                cliente,
                "cliente"
        );
    }

    // TESTES DE LISTAGEM

    @Test
    void deveListarClientes() {

        Cliente cliente = clienteValido();

        when(service.listar(""))
                .thenReturn(List.of(cliente));

        Model model = new ExtendedModelMap();

        String pagina = controller.listar(null, model);

        assertEquals("clientes/lista", pagina);
        assertEquals(List.of(cliente),
                model.getAttribute("clientes"));

        verify(service).listar("");
    }

    // TESTES DE FORMULÁRIO

    @Test
    void deveAbrirFormularioNovoCliente() {

        Model model = new ExtendedModelMap();

        String pagina = controller.novo(model);

        assertEquals("clientes/form", pagina);

        assertInstanceOf(
                Cliente.class,
                model.getAttribute("cliente")
        );
    }

    // TESTES DE CADASTRO

    @Test
    void deveCadastrarClienteValido() {

        Cliente cliente = clienteValido();

        when(service.cadastrar(cliente))
                .thenReturn(cliente);

        String pagina = controller.salvar(
                cliente,
                resultado(cliente),
                redirect
        );

        assertEquals("redirect:/clientes", pagina);

        verify(service).cadastrar(cliente);

        verify(redirect).addFlashAttribute(
                eq("mensagem"),
                any()
        );
    }

    @Test
    void naoDeveSalvarFormularioComErros() {

        Cliente cliente = clienteValido();

        BindingResult erros = resultado(cliente);

        erros.rejectValue(
                "nome",
                "obrigatorio",
                "Nome obrigatório"
        );

        String pagina = controller.salvar(
                cliente,
                erros,
                redirect
        );

        assertEquals("clientes/form", pagina);

        verifyNoInteractions(service);
    }

    @Test
    void deveRejeitarTelefoneDuplicado() {

        Cliente cliente = clienteValido();

        when(service.cadastrar(cliente))
                .thenThrow(
                    new ClienteService.TelefoneDuplicadoException()
                );

        BindingResult erros = resultado(cliente);

        String pagina = controller.salvar(
                cliente,
                erros,
                redirect
        );

        assertEquals("clientes/form", pagina);

        assertTrue(
                erros.hasFieldErrors("telefone")
        );
    }

    @Test
    void naoDeveCadastrarClienteComId() {

        Cliente cliente = clienteValido();
        cliente.setId(10L);

        String pagina = controller.salvar(
                cliente,
                resultado(cliente),
                redirect
        );

        assertEquals("redirect:/clientes", pagina);

        verifyNoInteractions(service);
    }

    // TESTES DE ATUALIZAÇÃO

    @Test
    void deveAtualizarClienteValido() {

        Cliente cliente = clienteValido();
        cliente.setId(1L);

        when(service.atualizar(1L, cliente))
                .thenReturn(cliente);

        String pagina = controller.atualizar(
                cliente,
                resultado(cliente),
                redirect
        );

        assertEquals("redirect:/clientes", pagina);

        verify(service).atualizar(1L, cliente);
    }

    @Test
    void naoDeveAtualizarSemId() {

        Cliente cliente = clienteValido();

        String pagina = controller.atualizar(
                cliente,
                resultado(cliente),
                redirect
        );

        assertEquals("redirect:/clientes", pagina);

        verifyNoInteractions(service);
    }

    @Test
    void deveRejeitarTelefoneDuplicadoNaEdicao() {

        Cliente cliente = clienteValido();
        cliente.setId(1L);

        when(service.atualizar(1L, cliente))
                .thenThrow(
                    new ClienteService.TelefoneDuplicadoException()
                );

        BindingResult erros = resultado(cliente);

        String pagina = controller.atualizar(
                cliente,
                erros,
                redirect
        );

        assertEquals("clientes/form", pagina);

        assertTrue(
                erros.hasFieldErrors("telefone")
        );
    }

    @Test
    void naoDeveAtualizarClienteInexistente() {

        Cliente cliente = clienteValido();
        cliente.setId(99L);

        when(service.atualizar(99L, cliente))
                .thenThrow(
                    new ClienteService.ClienteNaoEncontradoException(99L)
                );

        String pagina = controller.atualizar(
                cliente,
                resultado(cliente),
                redirect
        );

        assertEquals("redirect:/clientes", pagina);

        verify(redirect).addFlashAttribute(
                eq("erro"),
                any()
        );
    }

    // TESTES DE EDIÇÃO

    @Test
    void deveAbrirEdicaoDeClienteExistente() {

        Cliente cliente = clienteValido();
        cliente.setId(1L);

        when(service.buscarPorId(1L))
                .thenReturn(cliente);

        Model model = new ExtendedModelMap();

        String pagina = controller.editar(
                1L,
                model,
                redirect
        );

        assertEquals("clientes/form", pagina);

        assertSame(
                cliente,
                model.getAttribute("cliente")
        );
    }

    @Test
    void naoDeveEditarClienteInexistente() {

        when(service.buscarPorId(99L))
                .thenThrow(
                    new ClienteService.ClienteNaoEncontradoException(99L)
                );

        String pagina = controller.editar(
                99L,
                new ExtendedModelMap(),
                redirect
        );

        assertEquals("redirect:/clientes", pagina);
    }

    // TESTES DE EXCLUSÃO

    @Test
    void deveExcluirClienteExistente() {

        Cliente cliente = clienteValido();
        cliente.setId(1L);

        when(service.buscarPorId(1L))
                .thenReturn(cliente);

        String pagina = controller.excluir(
                1L,
                redirect
        );

        assertEquals("redirect:/clientes", pagina);

        verify(service).excluir(1L);

        verify(redirect).addFlashAttribute(
                eq("mensagem"),
                any()
        );
    }

    @Test
    void deveTratarErroDeIntegridadeNaExclusao() {

        Cliente cliente = clienteValido();
        cliente.setId(1L);

        when(service.buscarPorId(1L))
                .thenReturn(cliente);

        doThrow(
                new DataIntegrityViolationException(
                        "Cliente vinculado"
                )
        ).when(service).excluir(1L);

        String pagina = controller.excluir(
                1L,
                redirect
        );

        assertEquals("redirect:/clientes", pagina);

        verify(redirect).addFlashAttribute(
                eq("erro"),
                any()
        );
    }

    @Test
    void naoDeveExcluirClienteInexistente() {

        when(service.buscarPorId(99L))
                .thenThrow(
                    new ClienteService.ClienteNaoEncontradoException(99L)
                );

        String pagina = controller.excluir(
                99L,
                redirect
        );

        assertEquals("redirect:/clientes", pagina);

        verify(service, never()).excluir(anyLong());
    }

    // TESTE DE SEGURANÇA DO FORMULÁRIO

    @Test
    void naoDevePermitirAlterarDataCadastroPeloFormulario() {

        Cliente cliente = new Cliente();

        WebDataBinder binder = new WebDataBinder(
                cliente,
                "cliente"
        );

        controller.configurarCampos(binder);

        MutablePropertyValues valores =
                new MutablePropertyValues();

        valores.add("nome", "Ana Silva");

        valores.add(
                "dataCadastro",
                "2000-01-01T00:00:00"
        );

        binder.bind(valores);

        assertEquals("Ana Silva", cliente.getNome());

        assertNull(cliente.getDataCadastro());

        assertArrayEquals(
                new String[]{"dataCadastro"},
                binder.getBindingResult().getSuppressedFields()
        );
    }
}
