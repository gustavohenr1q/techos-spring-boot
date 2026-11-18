
package br.com.bugnacio.techos.controller;

import java.util.List;

import br.com.bugnacio.techos.model.Cliente;
import br.com.bugnacio.techos.model.Equipamento;
import br.com.bugnacio.techos.model.TipoEquipamento;

import br.com.bugnacio.techos.service.EquipamentoService;

import jakarta.validation.ConstraintViolationException;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/equipamentos")
public class EquipamentoController {

    private final EquipamentoService service;

    // INJEÇÃO DE DEPENDÊNCIA

    public EquipamentoController(EquipamentoService service) {
        this.service = service;
    }

    // CAMPOS PERMITIDOS NO FORMULÁRIO

    @InitBinder("equipamento")
    public void configurarCampos(WebDataBinder binder) {

        binder.setAllowedFields(
                "id",
                "tipo",
                "marca",
                "modelo",
                "numeroSerie"
        );

        binder.registerCustomEditor(
                String.class,
                new StringTrimmerEditor(true)
        );
    }

    // PREPARAR FORMULÁRIO

    private void prepararFormulario(Model model) {

        List<Cliente> clientes = service.listarClientes();

        model.addAttribute("clientes", clientes);

        model.addAttribute(
                "tipos",
                TipoEquipamento.values()
        );
    }

    // RETORNAR AO FORMULÁRIO COM ERROS

    private String formularioComErros(
            Model model,
            Long clienteId) {

        prepararFormulario(model);

        model.addAttribute(
                "clienteIdSelecionado",
                clienteId
        );

        return "equipamentos/form";
    }

    // LISTAR E PESQUISAR EQUIPAMENTOS

    @GetMapping
    public String listar(
            @RequestParam(
                    value = "busca",
                    required = false
            ) String busca,

            @RequestParam(
                    value = "tipo",
                    required = false
            ) TipoEquipamento tipo,

            Model model) {

        String termo = busca == null || busca.isBlank()
                ? null
                : busca.trim();

        model.addAttribute(
                "equipamentos",
                service.listar(termo, tipo)
        );

        model.addAttribute("busca", termo);

        model.addAttribute(
                "tipoSelecionado",
                tipo
        );

        model.addAttribute(
                "tipos",
                TipoEquipamento.values()
        );

        return "equipamentos/lista";
    }

    // FORMULÁRIO DE NOVO EQUIPAMENTO

    @GetMapping("/novo")
    public String novo(Model model) {

        model.addAttribute(
                "equipamento",
                new Equipamento()
        );

        prepararFormulario(model);

        return "equipamentos/form";
    }

    // CADASTRAR OU ATUALIZAR EQUIPAMENTO

    @PostMapping("/salvar")
    public String salvar(
            @ModelAttribute("equipamento")
            Equipamento equipamento,

            BindingResult result,

            @RequestParam(
                    value = "clienteId",
                    required = false
            ) Long clienteId,

            Model model,
            RedirectAttributes redirect) {

        // Verifica a seleção do cliente.

        if (clienteId == null) {

            result.rejectValue(
                    "cliente",
                    "obrigatorio",
                    "Selecione um cliente."
            );
        }

        // Erros de conversão ou campos inválidos
        // identificados durante o recebimento.

        if (result.hasErrors()) {
            return formularioComErros(model, clienteId);
        }

        // Identifica se é cadastro ou edição.

        boolean editando = equipamento.getId() != null;

        try {

            Equipamento salvo;

            if (editando) {

                salvo = service.atualizar(
                        equipamento.getId(),
                        equipamento,
                        clienteId
                );

            } else {

                salvo = service.cadastrar(
                        equipamento,
                        clienteId
                );
            }

            redirect.addFlashAttribute(
                    "mensagem",
                    "Equipamento "
                            + salvo.getModelo()
                            + (editando
                                ? " alterado com sucesso!"
                                : " cadastrado com sucesso!")
            );

            return "redirect:/equipamentos";

        } catch (
                EquipamentoService.NumeroSerieDuplicadoException ex
        ) {

            result.rejectValue(
                    "numeroSerie",
                    "duplicado",
                    ex.getMessage()
            );

            return formularioComErros(model, clienteId);

        } catch (
                EquipamentoService.ClienteNaoEncontradoException ex
        ) {

            result.rejectValue(
                    "cliente",
                    "invalido",
                    "O cliente selecionado não existe."
            );

            return formularioComErros(model, clienteId);

        } catch (ConstraintViolationException ex) {

            // Transforma erros do Service em mensagens
            // para os campos do formulário.

            ex.getConstraintViolations().forEach(violacao -> {

                String campo = violacao
                        .getPropertyPath()
                        .toString();

                switch (campo) {

                    case "tipo", "marca", "modelo",
                         "numeroSerie", "cliente" -> {

                        if (!result.hasFieldErrors(campo)) {
                            result.rejectValue(
                                    campo,
                                    "invalido",
                                    violacao.getMessage()
                            );
                        }
                    }

                    default -> result.reject(
                            "dados.invalidos",
                            "Existem dados inválidos no equipamento."
                    );
                }
            });

            return formularioComErros(model, clienteId);

        } catch (
                EquipamentoService.EquipamentoNaoEncontradoException ex
        ) {

            redirect.addFlashAttribute(
                    "erro",
                    "Equipamento não encontrado para atualização."
            );

            return "redirect:/equipamentos";

        } catch (DataIntegrityViolationException ex) {

            model.addAttribute(
                    "erro",
                    "Não foi possível salvar o equipamento. "
                            + "Verifique os dados e possíveis registros duplicados."
            );

            return formularioComErros(model, clienteId);

        } catch (IllegalArgumentException ex) {

            model.addAttribute(
                    "erro",
                    "Não foi possível salvar o equipamento. "
                            + "Verifique os dados informados."
            );

            return formularioComErros(model, clienteId);
        }
    }

    // FORMULÁRIO DE EDIÇÃO

    @GetMapping("/{id}/editar")
    public String editar(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirect) {

        try {

            Equipamento equipamento =
                    service.buscarPorId(id);

            model.addAttribute(
                    "equipamento",
                    equipamento
            );

            prepararFormulario(model);

            return "equipamentos/form";

        } catch (
                EquipamentoService.EquipamentoNaoEncontradoException ex
        ) {

            redirect.addFlashAttribute(
                    "erro",
                    "Equipamento não encontrado."
            );

            return "redirect:/equipamentos";
        }
    }

    // EXCLUIR EQUIPAMENTO

    @PostMapping("/{id}/excluir")
    public String excluir(
            @PathVariable Long id,
            RedirectAttributes redirect) {

        try {

            Equipamento equipamento =
                    service.buscarPorId(id);

            String modelo = equipamento.getModelo();

            service.excluir(id);

            redirect.addFlashAttribute(
                    "mensagem",
                    "Equipamento " + modelo
                            + " excluído com sucesso!"
            );

        } catch (
                EquipamentoService.EquipamentoNaoEncontradoException ex
        ) {

            redirect.addFlashAttribute(
                    "erro",
                    "Equipamento não encontrado."
            );

        } catch (DataIntegrityViolationException ex) {

            redirect.addFlashAttribute(
                    "erro",
                    "Não foi possível excluir o equipamento. "
                            + "Existem registros ou fotografias vinculados."
            );
        }

        return "redirect:/equipamentos";
    }
}
