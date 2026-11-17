
package br.com.bugnacio.techos.controller;

import br.com.bugnacio.techos.model.Cliente;
import br.com.bugnacio.techos.service.ClienteService;

import jakarta.validation.Valid;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteService service;

    // O Controller utiliza a camada Service.
    public ClienteController(ClienteService service) {
        this.service = service;
    }

    // Aceita somente os campos permitidos no formulário.
    @InitBinder("cliente")
    public void configurarCampos(WebDataBinder binder) {

        binder.setAllowedFields(
                "id",
                "nome",
                "telefone",
                "email",
                "cpf",
                "cep",
                "logradouro",
                "numero",
                "complemento",
                "bairro",
                "cidade",
                "estado"
        );

        binder.registerCustomEditor(
                String.class,
                new StringTrimmerEditor(true)
        );
    }

    // LISTAR E PESQUISAR

    @GetMapping
    public String listar(
            @RequestParam(value = "busca", required = false)
            String busca,
            Model model) {

        String termo = busca == null ? "" : busca.trim();

        model.addAttribute(
                "clientes",
                service.listar(termo)
        );

        model.addAttribute("busca", termo);

        return "clientes/lista";
    }

    // FORMULÁRIO DE CADASTRO

    @GetMapping("/novo")
    public String novo(Model model) {

        model.addAttribute("cliente", new Cliente());

        return "clientes/form";
    }

    // SALVAR NOVO CLIENTE

    @PostMapping("/salvar")
    public String salvar(
            @Valid @ModelAttribute("cliente") Cliente cliente,
            BindingResult result,
            RedirectAttributes redirect) {

        if (result.hasErrors()) {
            return "clientes/form";
        }

        if (cliente.getId() != null) {
            redirect.addFlashAttribute(
                    "erro",
                    "Cadastro inválido. Um novo cliente não pode possuir ID."
            );

            return "redirect:/clientes";
        }

        try {

            Cliente salvo = service.cadastrar(cliente);

            redirect.addFlashAttribute(
                    "mensagem",
                    "Cliente " + salvo.getNome()
                            + " cadastrado com sucesso!"
            );

            return "redirect:/clientes";

        } catch (ClienteService.TelefoneDuplicadoException ex) {

            result.rejectValue(
                    "telefone",
                    "duplicado",
                    ex.getMessage()
            );

            return "clientes/form";
        }
    }

    // FORMULÁRIO DE EDIÇÃO

    @GetMapping("/{id}/editar")
    public String editar(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirect) {

        try {

            Cliente cliente = service.buscarPorId(id);

            model.addAttribute("cliente", cliente);

            return "clientes/form";

        } catch (ClienteService.ClienteNaoEncontradoException ex) {

            redirect.addFlashAttribute(
                    "erro",
                    "Cliente não encontrado."
            );

            return "redirect:/clientes";
        }
    }

    // ATUALIZAR CLIENTE

    @PostMapping("/atualizar")
    public String atualizar(
            @Valid @ModelAttribute("cliente") Cliente cliente,
            BindingResult result,
            RedirectAttributes redirect) {

        if (cliente.getId() == null) {
            redirect.addFlashAttribute(
                    "erro",
                    "Cliente não identificado para atualização."
            );

            return "redirect:/clientes";
        }

        if (result.hasErrors()) {
            return "clientes/form";
        }

        try {

            Cliente atualizado = service.atualizar(
                    cliente.getId(),
                    cliente
            );

            redirect.addFlashAttribute(
                    "mensagem",
                    "Cliente " + atualizado.getNome()
                            + " alterado com sucesso!"
            );

            return "redirect:/clientes";

        } catch (ClienteService.TelefoneDuplicadoException ex) {

            result.rejectValue(
                    "telefone",
                    "duplicado",
                    ex.getMessage()
            );

            return "clientes/form";

        } catch (ClienteService.ClienteNaoEncontradoException ex) {

            redirect.addFlashAttribute(
                    "erro",
                    "Cliente não encontrado."
            );

            return "redirect:/clientes";
        }
    }

    // EXCLUIR CLIENTE

    @PostMapping("/{id}/excluir")
    public String excluir(
            @PathVariable Long id,
            RedirectAttributes redirect) {

        try {

            Cliente cliente = service.buscarPorId(id);

            String nome = cliente.getNome();

            service.excluir(id);

            redirect.addFlashAttribute(
                    "mensagem",
                    "Cliente " + nome
                            + " excluído com sucesso!"
            );

        } catch (ClienteService.ClienteNaoEncontradoException ex) {

            redirect.addFlashAttribute(
                    "erro",
                    "Cliente não encontrado."
            );

        } catch (DataIntegrityViolationException ex) {

            redirect.addFlashAttribute(
                    "erro",
                    "Não foi possível excluir o cliente. "
                            + "Verifique se existem equipamentos "
                            + "ou outros registros vinculados."
            );
        }

        return "redirect:/clientes";
    }
}
