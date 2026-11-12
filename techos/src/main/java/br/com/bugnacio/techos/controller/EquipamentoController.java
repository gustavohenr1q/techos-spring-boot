
package br.com.bugnacio.techos.controller;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.com.bugnacio.techos.model.Cliente;
import br.com.bugnacio.techos.model.TipoEquipamento;
import br.com.bugnacio.techos.model.Equipamento;
import br.com.bugnacio.techos.model.TipoEquipamento;
import br.com.bugnacio.techos.repository.ClienteRepository;
import br.com.bugnacio.techos.repository.EquipamentoRepository;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/equipamentos")
public class EquipamentoController {

    private final EquipamentoRepository repo;
    private final ClienteRepository clienteRepo;

    // Injeção de dependência.
    public EquipamentoController(
            EquipamentoRepository repo,
            ClienteRepository clienteRepo) {

        this.repo = repo;
        this.clienteRepo = clienteRepo;
    }

    // Prepara os dados necessários para os formulários.
    private void prepararFormulario(Model model) {

        List<Cliente> clientes = clienteRepo.findAll(Sort.by("nome"));

        model.addAttribute("clientes", clientes);
        model.addAttribute("tipos", TipoEquipamento.values());
    }

    // Lista todos os equipamentos.
    @GetMapping
    public String listar(

            @RequestParam(value = "busca", required = false) String busca,
            @RequestParam(value = "tipo", required = false) TipoEquipamento tipo,
            Model model) {

        // Remove espaços desnecessários da pesquisa.
        if (busca != null) {
            busca = busca.trim();

            if (busca.isBlank()) {
                busca = null;
            }
        }

        // Busca os equipamentos conforme os filtros informados.
        List<Equipamento> equipamentos = repo.buscar(busca, tipo);

        model.addAttribute("equipamentos", equipamentos);
        model.addAttribute("busca", busca);
        model.addAttribute("tipoSelecionado", tipo);
        model.addAttribute("tipos", TipoEquipamento.values());

        return "equipamentos/lista";
    }

    // Abre o formulário de cadastro.
    @GetMapping("/novo")
    public String novo(Model model) {

        model.addAttribute("equipamento", new Equipamento());

        prepararFormulario(model);

        return "equipamentos/form";
    }

    @PostMapping("/salvar")
    public String salvar(
            @Valid @ModelAttribute("equipamento") Equipamento equipamento,
            BindingResult result,
            @RequestParam(value = "clienteId", required = false) Long clienteId,
            Model model,
            RedirectAttributes redirect) {

        // 1. Verifica se um cliente foi selecionado.
        if (clienteId == null) {

            result.rejectValue(
                    "cliente",
                    "obrigatorio",
                    "É obrigatório selecionar um cliente.");

        } else {

            // 2. Verifica se o cliente selecionado realmente existe.
            Cliente cliente = clienteRepo.findById(clienteId).orElse(null);

            if (cliente == null) {

                result.rejectValue(
                        "cliente",
                        "invalido",
                        "O cliente selecionado não existe.");

            } else {

                equipamento.setCliente(cliente);
            }
        }

        // 3. Verifica se o número de série já está cadastrado.
        String numeroSerie = equipamento.getNumeroSerie();

        if (numeroSerie != null && !numeroSerie.isBlank()) {

            boolean jaExiste;

            if (equipamento.getId() == null) {

                jaExiste = repo.existsByNumeroSerie(numeroSerie);

            } else {

                jaExiste = repo.existsByNumeroSerieAndIdNot(
                        numeroSerie,
                        equipamento.getId());
            }

            if (jaExiste) {

                result.rejectValue(
                        "numeroSerie",
                        "duplicado",
                        "Já existe um equipamento com esse número de série.");
            }
        }

        // 4. Se houver erros, retorna ao formulário.
        if (result.hasErrors()) {

            prepararFormulario(model);

            // Mantém a seleção do cliente no formulário.
            model.addAttribute("clienteIdSelecionado", clienteId);

            return "equipamentos/form";
        }

        // 5. Salva o equipamento.
        boolean editando = equipamento.getId() != null;

        repo.save(equipamento);

        redirect.addFlashAttribute(
                "mensagem",
                editando
                        ? "Equipamento " + equipamento.getModelo() + " alterado com sucesso!"
                        : "Equipamento " + equipamento.getModelo() + " cadastrado com sucesso!");

        return "redirect:/equipamentos";
    }

    // Exclui um equipamento.
    @PostMapping("/{id}/excluir")
    public String excluir(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        Equipamento equipamento = repo.findById(id).orElse(null);

        if (equipamento == null) {

            redirectAttributes.addFlashAttribute(
                    "erro",
                    "Equipamento não encontrado!");

            return "redirect:/equipamentos";
        }

        String modelo = equipamento.getModelo();

        repo.delete(equipamento);

        redirectAttributes.addFlashAttribute(
                "erro",
                "Equipamento " + modelo + " excluído!");

        return "redirect:/equipamentos";
    }

    // Busca um equipamento para edição.
    @GetMapping("/{id}/editar")
    public String editar(
            @PathVariable Long id,
            Model model) {

        Equipamento equipamento = repo.findById(id).orElse(null);

        if (equipamento == null) {

            return "redirect:/equipamentos";
        }

        model.addAttribute("equipamento", equipamento);

        prepararFormulario(model);

        return "equipamentos/form";
    }

}
