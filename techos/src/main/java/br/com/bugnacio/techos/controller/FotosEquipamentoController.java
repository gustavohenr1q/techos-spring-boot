
package br.com.bugnacio.techos.controller;

import br.com.bugnacio.techos.model.OrdemServico;
import br.com.bugnacio.techos.model.TipoFoto;

import br.com.bugnacio.techos.service.EquipamentoService;
import br.com.bugnacio.techos.service.FotosEquipamentoService;
import br.com.bugnacio.techos.service.OrdemServicoService;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/equipamentos/{equipamentoId}/fotos")
public class FotosEquipamentoController {

    private final FotosEquipamentoService service;
    private final OrdemServicoService ordemServicoService;

    public FotosEquipamentoController(
            FotosEquipamentoService service,
            OrdemServicoService ordemServicoService) {

        this.service = service;
        this.ordemServicoService = ordemServicoService;
    }

    // PÁGINA DE FOTOGRAFIAS

    @GetMapping
    public String pagina(
            @PathVariable Long equipamentoId,

            @RequestParam(
                    value = "ordemServicoId",
                    required = false
            ) Long ordemServicoId,

            Model model) {

        try {

            var equipamento =
                    service.buscarEquipamento(equipamentoId);

            // Verifica se a OS pertence ao equipamento.
            validarOrigem(equipamentoId, ordemServicoId);

            model.addAttribute(
                    "equipamento",
                    equipamento
            );

            model.addAttribute(
                    "registros",
                    service.listar(equipamentoId)
            );

            // Preserva a origem da navegação.
            model.addAttribute(
                    "ordemServicoId",
                    ordemServicoId
            );

            return "equipamentos/fotos";

        } catch (
                EquipamentoService.EquipamentoNaoEncontradoException
                | OrdemServicoService.OrdemServicoNaoEncontradaException
                | IllegalArgumentException ex
        ) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Equipamento ou ordem de serviço não encontrado."
            );
        }
    }

    // CRIAR NOVO REGISTRO FOTOGRÁFICO

    @PostMapping("/novo")
    public String novoRegistro(
            @PathVariable Long equipamentoId,

            @RequestParam(
                    value = "ordemServicoId",
                    required = false
            ) Long ordemServicoId,

            RedirectAttributes redirect) {

        // Impede utilizar uma OS de outro equipamento.
        validarOrigem(equipamentoId, ordemServicoId);

        try {

            service.abrirRegistro(equipamentoId);

            redirect.addFlashAttribute(
                    "mensagem",
                    "Novo registro fotográfico criado!"
            );

        } catch (IllegalArgumentException ex) {

            redirect.addFlashAttribute(
                    "erro",
                    ex.getMessage()
            );
        }

        return redirecionar(
                equipamentoId,
                ordemServicoId
        );
    }

    // ENVIAR FOTOGRAFIAS

    @PostMapping("/{registroId}/enviar")
    public String enviar(
            @PathVariable Long equipamentoId,

            @PathVariable Long registroId,

            @RequestParam(
                    value = "ordemServicoId",
                    required = false
            ) Long ordemServicoId,

            @RequestParam("tipo") TipoFoto tipo,

            @RequestParam("arquivos")
            MultipartFile[] arquivos,

            RedirectAttributes redirect) {

        validarOrigem(equipamentoId, ordemServicoId);

        try {

            service.enviar(
                    equipamentoId,
                    registroId,
                    tipo,
                    arquivos
            );

            redirect.addFlashAttribute(
                    "mensagem",
                    "Fotos registradas com sucesso!"
            );

        } catch (IllegalArgumentException ex) {

            redirect.addFlashAttribute(
                    "erro",
                    ex.getMessage()
            );

        } catch (RuntimeException ex) {

            redirect.addFlashAttribute(
                    "erro",
                    "Não foi possível armazenar as fotos. "
                            + "Tente novamente."
            );
        }

        return redirecionar(
                equipamentoId,
                ordemServicoId
        );
    }

    // EXIBIR IMAGEM

    @GetMapping("/{fotoId}/imagem")
    @ResponseBody
    public ResponseEntity<byte[]> imagem(
            @PathVariable Long equipamentoId,
            @PathVariable Long fotoId) {

        try {

            FotosEquipamentoService.Imagem imagem =
                    service.buscarImagem(
                            equipamentoId,
                            fotoId
                    );

            return ResponseEntity.ok()
                    .contentType(
                            MediaType.parseMediaType(
                                    imagem.contentType()
                            )
                    )
                    .cacheControl(
                            CacheControl.noStore()
                    )
                    .header(
                            "X-Content-Type-Options",
                            "nosniff"
                    )
                    .body(imagem.conteudo());

        } catch (IllegalArgumentException ex) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Foto não encontrada."
            );
        }
    }

    // VALIDAR A ORIGEM DA NAVEGAÇÃO

    private void validarOrigem(
            Long equipamentoId,
            Long ordemServicoId) {

        // Acesso normal pelo módulo de Equipamentos.
        if (ordemServicoId == null) {
            return;
        }

        if (ordemServicoId <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Ordem de serviço inválida."
            );
        }

        OrdemServico ordem;

        try {

            ordem = ordemServicoService.buscarPorId(
                    ordemServicoId
            );

        } catch (
                OrdemServicoService.OrdemServicoNaoEncontradaException
                | IllegalArgumentException ex
        ) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Ordem de serviço não encontrada."
            );
        }

        if (ordem.getEquipamento() == null
                || !equipamentoId.equals(
                        ordem.getEquipamento().getId()
                )) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Esta ordem de serviço não pertence "
                            + "ao equipamento informado."
            );
        }
    }

    // REDIRECIONAMENTO PRESERVANDO A ORIGEM

    private String redirecionar(
            Long equipamentoId,
            Long ordemServicoId) {

        String destino =
                "redirect:/equipamentos/"
                        + equipamentoId
                        + "/fotos";

        // Se veio do módulo de Equipamentos,
        // mantém o comportamento original.
        if (ordemServicoId == null) {
            return destino;
        }

        // Se veio de uma OS,
        // preserva o ID após criar ou enviar fotos.
        return destino
                + "?ordemServicoId="
                + ordemServicoId;
    }
}
