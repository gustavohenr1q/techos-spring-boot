
package br.com.bugnacio.techos.controller;

import br.com.bugnacio.techos.model.TipoFoto;
import br.com.bugnacio.techos.service.FotosEquipamentoService;

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

    public FotosEquipamentoController(
            FotosEquipamentoService service) {

        this.service = service;
    }

    @GetMapping
    public String pagina(
            @PathVariable Long equipamentoId,
            Model model) {

        try {
            model.addAttribute(
                    "equipamento",
                    service.buscarEquipamento(equipamentoId)
            );

            model.addAttribute(
                    "registros",
                    service.listar(equipamentoId)
            );

            return "equipamentos/fotos";

        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Equipamento não encontrado."
            );
        }
    }

    @PostMapping("/novo")
    public String novoRegistro(
            @PathVariable Long equipamentoId,
            RedirectAttributes redirect) {

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

        return redirecionar(equipamentoId);
    }

    @PostMapping("/{registroId}/enviar")
    public String enviar(
            @PathVariable Long equipamentoId,
            @PathVariable Long registroId,
            @RequestParam("tipo") TipoFoto tipo,
            @RequestParam("arquivos") MultipartFile[] arquivos,
            RedirectAttributes redirect) {

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

        return redirecionar(equipamentoId);
    }

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
                    .contentType(MediaType.parseMediaType(
                            imagem.contentType()
                    ))
                    .cacheControl(CacheControl.noStore())
                    .header("X-Content-Type-Options", "nosniff")
                    .body(imagem.conteudo());

        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Foto não encontrada."
            );
        }
    }

    private String redirecionar(Long equipamentoId) {
        return "redirect:/equipamentos/"
                + equipamentoId
                + "/fotos";
    }
}
