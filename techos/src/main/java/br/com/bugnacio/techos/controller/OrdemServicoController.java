
package br.com.bugnacio.techos.controller;

import br.com.bugnacio.techos.dto.AbrirOrdemServicoRequest;

import br.com.bugnacio.techos.model.OrdemServico;
import br.com.bugnacio.techos.model.StatusOrdemServico;

import br.com.bugnacio.techos.service.EquipamentoService;
import br.com.bugnacio.techos.service.FluxoOrdemServicoService;
import br.com.bugnacio.techos.service.OrdemServicoService;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Set;

@Controller
@RequestMapping("/ordens-servico")
public class OrdemServicoController {

    private static final String PAGINA_LISTA =
            "ordens-servico/lista";

    private static final String PAGINA_FORM =
            "ordens-servico/form";

    private static final String PAGINA_DETALHES =
            "ordens-servico/detalhes";

    private static final int TAMANHO_PAGINA = 15;

    // Temporário, apenas para desenvolvimento.
    // Será substituído pelo usuário autenticado.
    private static final String OPERADOR_TESTE =
            "Operador de teste";

    private static final Set<String> CAMPOS_FORM =
            Set.of(
                    "equipamentoId",
                    "defeitoRelatado",
                    "acessoriosRecebidos",
                    "estadoConservacao",
                    "dataPrevistaEntrega"
            );

    private final OrdemServicoService ordemServicoService;

    private final FluxoOrdemServicoService fluxoService;

    private final EquipamentoService equipamentoService;

    public OrdemServicoController(
            OrdemServicoService ordemServicoService,
            FluxoOrdemServicoService fluxoService,
            EquipamentoService equipamentoService) {

        this.ordemServicoService = ordemServicoService;
        this.fluxoService = fluxoService;
        this.equipamentoService = equipamentoService;
    }

    // CAMPOS PERMITIDOS NO FORMULÁRIO

    @InitBinder("form")
    public void configurarFormulario(WebDataBinder binder) {

        binder.setAllowedFields(
                "equipamentoId",
                "defeitoRelatado",
                "acessoriosRecebidos",
                "estadoConservacao",
                "dataPrevistaEntrega"
        );

        binder.registerCustomEditor(
                String.class,
                new StringTrimmerEditor(true)
        );
    }

    // LISTAGEM COM PESQUISA E PAGINAÇÃO

    @GetMapping
    public String listar(
            @RequestParam(
                    value = "busca",
                    required = false
            ) String busca,

            @RequestParam(
                    value = "status",
                    required = false
            ) StatusOrdemServico status,

            @RequestParam(
                    value = "pagina",
                    defaultValue = "0"
            ) int pagina,

            Model model) {

        int paginaSegura = Math.max(0, pagina);

        Page<OrdemServico> resultado =
                ordemServicoService.listar(
                        busca,
                        status,
                        paginaSegura,
                        TAMANHO_PAGINA
                );

        model.addAttribute(
                "ordens",
                resultado.getContent()
        );

        model.addAttribute(
                "paginaOrdens",
                resultado
        );

        model.addAttribute("busca", busca);

        model.addAttribute(
                "statusSelecionado",
                status
        );

        model.addAttribute(
                "statusDisponiveis",
                StatusOrdemServico.values()
        );

        return PAGINA_LISTA;
    }

    // FORMULÁRIO DE ABERTURA

    @GetMapping("/novo")
    public String novo(Model model) {

        AbrirOrdemServicoRequest form =
                new AbrirOrdemServicoRequest(
                        null,
                        null,
                        null,
                        null,
                        null
                );

        model.addAttribute("form", form);

        carregarEquipamentos(model);

        return PAGINA_FORM;
    }

    // ABRIR ORDEM DE SERVIÇO

    @PostMapping("/salvar")
    public String salvar(
            @Valid
            @ModelAttribute("form")
            AbrirOrdemServicoRequest form,

            BindingResult erros,

            Model model,
            RedirectAttributes redirect) {

        if (erros.hasErrors()) {
            return retornarFormulario(model);
        }

        try {

            // Usa o fluxo para registrar também
            // o evento de abertura no histórico.

            OrdemServico ordem = fluxoService.abrir(
                    form,
                    OPERADOR_TESTE
            );

            redirect.addFlashAttribute(
                    "mensagem",
                    "Ordem de serviço "
                            + ordem.getProtocolo()
                            + " aberta com sucesso!"
            );

            return "redirect:/ordens-servico/"
                    + ordem.getId();

        } catch (
                EquipamentoService
                        .EquipamentoNaoEncontradoException ex
        ) {

            erros.rejectValue(
                    "equipamentoId",
                    "equipamento.inexistente",
                    "O equipamento selecionado não existe."
            );

        } catch (ConstraintViolationException ex) {

            // Converte violações do Service em
            // mensagens para o formulário.

            ex.getConstraintViolations().forEach(
                    violacao -> {

                        String campo = violacao
                                .getPropertyPath()
                                .toString();

                        if (CAMPOS_FORM.contains(campo)) {

                            if (!erros.hasFieldErrors(campo)) {

                                erros.rejectValue(
                                        campo,
                                        "invalido",
                                        violacao.getMessage()
                                );
                            }

                        } else {

                            erros.reject(
                                    "dados.invalidos",
                                    "Existem informações inválidas "
                                            + "na ordem de serviço."
                            );
                        }
                    }
            );

        } catch (DataIntegrityViolationException ex) {

            erros.reject(
                    "integridade",
                    "Não foi possível abrir a ordem. "
                            + "Confira o equipamento selecionado."
            );

        } catch (IllegalStateException ex) {

            erros.reject(
                    "estado.invalido",
                    "O equipamento não está em condições "
                            + "de receber uma ordem de serviço."
            );
        }

        return retornarFormulario(model);
    }

    // DETALHES E HISTÓRICO DA OS

    @GetMapping("/{id}")
    public String detalhes(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirect) {

        try {

            OrdemServico ordem =
                    ordemServicoService.buscarPorId(id);

            model.addAttribute(
                    "ordem",
                    ordem
            );

            model.addAttribute(
                    "historico",
                    fluxoService.listarHistorico(id)
            );

            return PAGINA_DETALHES;

        } catch (
                OrdemServicoService
                        .OrdemServicoNaoEncontradaException ex
        ) {

            redirect.addFlashAttribute(
                    "erro",
                    "Ordem de serviço não encontrada."
            );

            return "redirect:/ordens-servico";

        } catch (IllegalArgumentException ex) {

            redirect.addFlashAttribute(
                    "erro",
                    "Identificador da ordem de serviço inválido."
            );

            return "redirect:/ordens-servico";
        }
    }

    // RECARREGAR EQUIPAMENTOS DO FORMULÁRIO

    private void carregarEquipamentos(Model model) {

        model.addAttribute(
                "equipamentos",
                equipamentoService.listar(null, null)
        );
    }

    // RETORNAR AO FORMULÁRIO COM ERROS

    private String retornarFormulario(Model model) {

        carregarEquipamentos(model);

        return PAGINA_FORM;
    }
}
