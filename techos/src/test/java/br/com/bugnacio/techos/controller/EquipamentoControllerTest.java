
package br.com.bugnacio.techos.controller;

import br.com.bugnacio.techos.model.Equipamento;
import br.com.bugnacio.techos.model.TipoEquipamento;
import br.com.bugnacio.techos.service.EquipamentoService;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;

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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class EquipamentoControllerTest {

    @Mock
    private EquipamentoService service;

    @Mock
    private RedirectAttributes redirect;

    @InjectMocks
    private EquipamentoController controller;

    private Equipamento equipamentoValido() {

        Equipamento equipamento = new Equipamento();

        equipamento.setTipo(TipoEquipamento.NOTEBOOK);
        equipamento.setMarca("Dell");
        equipamento.setModelo("Inspiron");
        equipamento.setNumeroSerie("ABC123");

        return equipamento;
    }

    private BindingResult resultado(Equipamento equipamento) {

        return new BeanPropertyBindingResult(
                equipamento,
                "equipamento"
        );
    }

    private Model modelo() {
        return new ExtendedModelMap();
    }

    @Test
    void deveListarEquipamentos() {

        Equipamento equipamento = equipamentoValido();

        when(service.listar("Dell", TipoEquipamento.NOTEBOOK))
                .thenReturn(List.of(equipamento));

        Model model = modelo();

        String pagina = controller.listar(
                " Dell ",
                TipoEquipamento.NOTEBOOK,
                model
        );

        assertEquals("equipamentos/lista", pagina);

        assertEquals(
                List.of(equipamento),
                model.getAttribute("equipamentos")
        );
    }

    @Test
    void deveAbrirFormularioNovo() {

        Model model = modelo();

        String pagina = controller.novo(model);

        assertEquals("equipamentos/form", pagina);

        assertInstanceOf(
                Equipamento.class,
                model.getAttribute("equipamento")
        );
    }

    @Test
    void deveCadastrarEquipamento() {

        Equipamento equipamento = equipamentoValido();

        when(service.cadastrar(equipamento, 2L))
                .thenReturn(equipamento);

        String pagina = controller.salvar(
                equipamento,
                resultado(equipamento),
                2L,
                modelo(),
                redirect
        );

        assertEquals("redirect:/equipamentos", pagina);

        verify(service).cadastrar(equipamento, 2L);

        verify(redirect).addFlashAttribute(
                eq("mensagem"),
                any()
        );
    }

    @Test
    void deveAbrirEdicao() {

        Equipamento equipamento = equipamentoValido();
        equipamento.setId(4L);

        when(service.buscarPorId(4L))
                .thenReturn(equipamento);

        Model model = modelo();

        String pagina = controller.editar(
                4L,
                model,
                redirect
        );

        assertEquals("equipamentos/form", pagina);

        assertSame(
                equipamento,
                model.getAttribute("equipamento")
        );
    }

    @Test
    void deveAtualizarEquipamento() {

        Equipamento equipamento = equipamentoValido();
        equipamento.setId(4L);

        when(service.atualizar(4L, equipamento, 2L))
                .thenReturn(equipamento);

        String pagina = controller.salvar(
                equipamento,
                resultado(equipamento),
                2L,
                modelo(),
                redirect
        );

        assertEquals("redirect:/equipamentos", pagina);

        verify(service).atualizar(
                4L,
                equipamento,
                2L
        );
    }

    @Test
    void deveExcluirEquipamento() {

        Equipamento equipamento = equipamentoValido();
        equipamento.setId(4L);

        when(service.buscarPorId(4L))
                .thenReturn(equipamento);

        String pagina = controller.excluir(4L, redirect);

        assertEquals("redirect:/equipamentos", pagina);

        verify(service).excluir(4L);

        verify(redirect).addFlashAttribute(
                eq("mensagem"),
                any()
        );
    }

    @Test
    void naoDeveSalvarSemCliente() {

        Equipamento equipamento = equipamentoValido();
        BindingResult erros = resultado(equipamento);

        String pagina = controller.salvar(
                equipamento,
                erros,
                null,
                modelo(),
                redirect
        );

        assertEquals("equipamentos/form", pagina);

        assertTrue(erros.hasFieldErrors("cliente"));

        verify(service, never())
                .cadastrar(any(), any());
    }

    @Test
    void naoDeveSalvarFormularioComErros() {

        Equipamento equipamento = equipamentoValido();
        BindingResult erros = resultado(equipamento);

        erros.rejectValue(
                "marca",
                "invalido",
                "Marca inválida"
        );

        String pagina = controller.salvar(
                equipamento,
                erros,
                2L,
                modelo(),
                redirect
        );

        assertEquals("equipamentos/form", pagina);

        verify(service, never())
                .cadastrar(any(), any());
    }

    @Test
    void deveInformarNumeroSerieDuplicado() {

        Equipamento equipamento = equipamentoValido();

        when(service.cadastrar(equipamento, 2L))
                .thenThrow(
                    new EquipamentoService
                            .NumeroSerieDuplicadoException()
                );

        BindingResult erros = resultado(equipamento);

        String pagina = controller.salvar(
                equipamento,
                erros,
                2L,
                modelo(),
                redirect
        );

        assertEquals("equipamentos/form", pagina);

        assertTrue(
                erros.hasFieldErrors("numeroSerie")
        );
    }

    @Test
    void deveInformarClienteInexistente() {

        Equipamento equipamento = equipamentoValido();

        when(service.cadastrar(equipamento, 99L))
                .thenThrow(
                    new EquipamentoService
                            .ClienteNaoEncontradoException(99L)
                );

        BindingResult erros = resultado(equipamento);

        String pagina = controller.salvar(
                equipamento,
                erros,
                99L,
                modelo(),
                redirect
        );

        assertEquals("equipamentos/form", pagina);

        assertTrue(erros.hasFieldErrors("cliente"));
    }

    
@Test
@SuppressWarnings("unchecked")
void deveExibirErroDeValidacaoDaMarca() {

    // Prepara um equipamento válido
    Equipamento equipamento = equipamentoValido();

    // Simula uma violação de validação
    ConstraintViolation<Equipamento> violacao =
            mock(ConstraintViolation.class);

    Path caminho = mock(Path.class);

    when(violacao.getPropertyPath())
            .thenReturn(caminho);

    when(caminho.toString())
            .thenReturn("marca");

    when(violacao.getMessage())
            .thenReturn("Marca obrigatória");

    // CORREÇÃO:
    // Cria a exceção antes de configurar o Service.
    ConstraintViolationException excecao =
            new ConstraintViolationException(
                    Set.of(violacao)
            );

    // Simula a falha durante o cadastro
    when(service.cadastrar(equipamento, 2L))
            .thenThrow(excecao);

    BindingResult erros = resultado(equipamento);

    // Executa o método do Controller
    String pagina = controller.salvar(
            equipamento,
            erros,
            2L,
            modelo(),
            redirect
    );

    // Confirma que retorna ao formulário
    assertEquals("equipamentos/form", pagina);

    // Confirma que o erro aparece no campo marca
    assertTrue(erros.hasFieldErrors("marca"));

    // Confirma que o erro foi tratado corretamente
    assertEquals(
            "Marca obrigatória",
            erros.getFieldError("marca").getDefaultMessage()
    );
}


    @Test
    void naoDeveEditarEquipamentoInexistente() {

        when(service.buscarPorId(99L))
                .thenThrow(
                    new EquipamentoService
                            .EquipamentoNaoEncontradoException(99L)
                );

        String pagina = controller.editar(
                99L,
                modelo(),
                redirect
        );

        assertEquals("redirect:/equipamentos", pagina);

        verify(redirect).addFlashAttribute(
                eq("erro"),
                any()
        );
    }

    @Test
    void deveTratarExclusaoComRegistrosVinculados() {

        Equipamento equipamento = equipamentoValido();
        equipamento.setId(4L);

        when(service.buscarPorId(4L))
                .thenReturn(equipamento);

        doThrow(
                new DataIntegrityViolationException(
                        "Registro vinculado"
                )
        ).when(service).excluir(4L);

        String pagina = controller.excluir(
                4L,
                redirect
        );

        assertEquals("redirect:/equipamentos", pagina);

        verify(redirect).addFlashAttribute(
                eq("erro"),
                any()
        );
    }

    @Test
    void naoDevePermitirAlterarClientePeloObjeto() {

        Equipamento equipamento = equipamentoValido();

        WebDataBinder binder = new WebDataBinder(
                equipamento,
                "equipamento"
        );

        controller.configurarCampos(binder);

        MutablePropertyValues dados =
                new MutablePropertyValues();

        dados.add("marca", "Lenovo");
        dados.add("cliente.id", "99");

        binder.bind(dados);

        assertEquals("Lenovo", equipamento.getMarca());

        assertNull(equipamento.getCliente());

        assertArrayEquals(
                new String[]{"cliente.id"},
                binder.getBindingResult().getSuppressedFields()
        );
    }
}
