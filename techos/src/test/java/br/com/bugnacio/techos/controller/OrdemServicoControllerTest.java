
package br.com.bugnacio.techos.controller;

import br.com.bugnacio.techos.dto.AbrirOrdemServicoRequest;

import br.com.bugnacio.techos.model.Equipamento;
import br.com.bugnacio.techos.model.OrdemServico;
import br.com.bugnacio.techos.model.StatusOrdemServico;

import br.com.bugnacio.techos.service.EquipamentoService;
import br.com.bugnacio.techos.service.FluxoOrdemServicoService;
import br.com.bugnacio.techos.service.OrdemServicoService;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.ui.ExtendedModelMap;
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
class OrdemServicoControllerTest {

    @Mock
    private OrdemServicoService ordemServicoService;

    @Mock
    private FluxoOrdemServicoService fluxoService;

    @Mock
    private EquipamentoService equipamentoService;

    @Mock
    private RedirectAttributes redirect;

    @InjectMocks
    private OrdemServicoController controller;

    private AbrirOrdemServicoRequest requestValido() {

        return new AbrirOrdemServicoRequest(
                2L,
                "Notebook não liga",
                "Carregador",
                "Marcas de uso",
                null
        );
    }

    private OrdemServico ordemValida() {

        OrdemServico ordem = new OrdemServico();

        ordem.setId(10L);
        ordem.setStatus(StatusOrdemServico.ABERTA);
        ordem.setDefeitoRelatado("Notebook não liga");

        return ordem;
    }

    private BindingResult resultado(
            AbrirOrdemServicoRequest request) {

        return new BeanPropertyBindingResult(
                request,
                "form"
        );
    }

    @Test
    void deveListarOrdens() {

        OrdemServico ordem = ordemValida();

        Page<OrdemServico> pagina =
                new PageImpl<>(List.of(ordem));

        when(ordemServicoService.listar(
                "Dell",
                StatusOrdemServico.ABERTA,
                0,
                15
        )).thenReturn(pagina);

        ExtendedModelMap model =
                new ExtendedModelMap();

        String resposta = controller.listar(
                "Dell",
                StatusOrdemServico.ABERTA,
                0,
                model
        );

        assertEquals(
                "ordens-servico/lista",
                resposta
        );

        assertEquals(
                List.of(ordem),
                model.get("ordens")
        );
    }

    @Test
    void deveCorrigirPaginaNegativa() {

        when(ordemServicoService.listar(
                null,
                null,
                0,
                15
        )).thenReturn(Page.empty());

        controller.listar(
                null,
                null,
                -5,
                new ExtendedModelMap()
        );

        verify(ordemServicoService).listar(
                null,
                null,
                0,
                15
        );
    }

    @Test
    void deveAbrirFormularioNovo() {

        when(equipamentoService.listar(null, null))
                .thenReturn(List.of(new Equipamento()));

        ExtendedModelMap model =
                new ExtendedModelMap();

        String resposta = controller.novo(model);

        assertEquals(
                "ordens-servico/form",
                resposta
        );

        assertInstanceOf(
                AbrirOrdemServicoRequest.class,
                model.get("form")
        );

        assertNotNull(model.get("equipamentos"));
    }

    @Test
    void deveAbrirOrdemComHistorico() {

        AbrirOrdemServicoRequest request =
                requestValido();

        OrdemServico ordem = ordemValida();

        when(fluxoService.abrir(
                eq(request),
                anyString()
        )).thenReturn(ordem);

        String resposta = controller.salvar(
                request,
                resultado(request),
                new ExtendedModelMap(),
                redirect
        );

        assertEquals(
                "redirect:/ordens-servico/10",
                resposta
        );

        verify(fluxoService).abrir(
                eq(request),
                anyString()
        );

        verify(redirect).addFlashAttribute(
                eq("mensagem"),
                any()
        );
    }

    @Test
    void naoDeveSalvarFormularioComErros() {

        AbrirOrdemServicoRequest request =
                requestValido();

        BindingResult erros = resultado(request);

        erros.rejectValue(
                "defeitoRelatado",
                "obrigatorio",
                "Informe o defeito."
        );

        String resposta = controller.salvar(
                request,
                erros,
                new ExtendedModelMap(),
                redirect
        );

        assertEquals(
                "ordens-servico/form",
                resposta
        );

        assertTrue(
                erros.hasFieldErrors("defeitoRelatado")
        );

        verifyNoInteractions(fluxoService);
    }

    @Test
    void deveTratarEquipamentoInexistente() {

        AbrirOrdemServicoRequest request =
                requestValido();

        when(fluxoService.abrir(
                eq(request),
                anyString()
        )).thenThrow(
                new EquipamentoService
                        .EquipamentoNaoEncontradoException(2L)
        );

        BindingResult erros = resultado(request);

        String resposta = controller.salvar(
                request,
                erros,
                new ExtendedModelMap(),
                redirect
        );

        assertEquals(
                "ordens-servico/form",
                resposta
        );

        assertTrue(
                erros.hasFieldErrors("equipamentoId")
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void deveTratarErroDeValidacaoDoService() {

        AbrirOrdemServicoRequest request =
                requestValido();

        ConstraintViolation<AbrirOrdemServicoRequest> violacao =
                mock(ConstraintViolation.class);

        Path caminho = mock(Path.class);

        when(violacao.getPropertyPath())
                .thenReturn(caminho);

        when(caminho.toString())
                .thenReturn("defeitoRelatado");

        when(violacao.getMessage())
                .thenReturn("Defeito obrigatório");

        // Criar a exceção ANTES de configurar o Mockito
        // evita o erro UnfinishedStubbing.

        ConstraintViolationException excecao =
                new ConstraintViolationException(
                        Set.of(violacao)
                );

        when(fluxoService.abrir(
                eq(request),
                anyString()
        )).thenThrow(excecao);

        BindingResult erros = resultado(request);

        String resposta = controller.salvar(
                request,
                erros,
                new ExtendedModelMap(),
                redirect
        );

        assertEquals(
                "ordens-servico/form",
                resposta
        );

        assertTrue(
                erros.hasFieldErrors("defeitoRelatado")
        );
    }

    @Test
    void deveTratarEquipamentoSemCliente() {

        AbrirOrdemServicoRequest request =
                requestValido();

        when(fluxoService.abrir(
                eq(request),
                anyString()
        )).thenThrow(
                new IllegalStateException(
                        "Equipamento sem cliente"
                )
        );

        BindingResult erros = resultado(request);

        String resposta = controller.salvar(
                request,
                erros,
                new ExtendedModelMap(),
                redirect
        );

        assertEquals(
                "ordens-servico/form",
                resposta
        );

        assertTrue(erros.hasGlobalErrors());
    }

    @Test
    void deveExibirDetalhesDaOrdem() {

        OrdemServico ordem = ordemValida();

        when(ordemServicoService.buscarPorId(10L))
                .thenReturn(ordem);

        when(fluxoService.listarHistorico(10L))
                .thenReturn(List.of());

        ExtendedModelMap model =
                new ExtendedModelMap();

        String resposta = controller.detalhes(
                10L,
                model,
                redirect
        );

        assertEquals(
                "ordens-servico/detalhes",
                resposta
        );

        assertSame(
                ordem,
                model.get("ordem")
        );

        assertNotNull(model.get("historico"));
    }

    @Test
    void deveRedirecionarOrdemInexistente() {

        when(ordemServicoService.buscarPorId(99L))
                .thenThrow(
                        new OrdemServicoService
                                .OrdemServicoNaoEncontradaException(99L)
                );

        String resposta = controller.detalhes(
                99L,
                new ExtendedModelMap(),
                redirect
        );

        assertEquals(
                "redirect:/ordens-servico",
                resposta
        );

        verify(redirect).addFlashAttribute(
                eq("erro"),
                any()
        );
    }

    @Test
    void deveRejeitarIdentificadorInvalido() {

        when(ordemServicoService.buscarPorId(-1L))
                .thenThrow(
                        new IllegalArgumentException(
                                "ID inválido"
                        )
                );

        String resposta = controller.detalhes(
                -1L,
                new ExtendedModelMap(),
                redirect
        );

        assertEquals(
                "redirect:/ordens-servico",
                resposta
        );
    }

    @Test
    void deveRestringirCamposDoFormulario() {

        AbrirOrdemServicoRequest request =
                requestValido();

        WebDataBinder binder = new WebDataBinder(
                request,
                "form"
        );

        controller.configurarFormulario(binder);

        assertNotNull(binder.getAllowedFields());

        assertTrue(
                List.of(binder.getAllowedFields())
                        .contains("equipamentoId")
        );

        assertFalse(
                List.of(binder.getAllowedFields())
                        .contains("status")
        );
    }
}
