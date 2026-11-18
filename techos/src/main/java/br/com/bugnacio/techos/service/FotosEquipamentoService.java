
package br.com.bugnacio.techos.service;

import br.com.bugnacio.techos.model.*;
import br.com.bugnacio.techos.repository.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FotosEquipamentoService {

    private final EquipamentoRepository equipamentoRepo;
    private final RegistroFotograficoRepository registroRepo;
    private final FotoEquipamentoRepository fotoRepo;
    private final FotoStorageService storage;

    public record RegistroComFotos(
            RegistroFotografico registro,
            List<FotoEquipamento> fotos
    ) {}

    public record Imagem(
            byte[] conteudo,
            String contentType
    ) {}

    public FotosEquipamentoService(
            EquipamentoRepository equipamentoRepo,
            RegistroFotograficoRepository registroRepo,
            FotoEquipamentoRepository fotoRepo,
            FotoStorageService storage) {

        this.equipamentoRepo = equipamentoRepo;
        this.registroRepo = registroRepo;
        this.fotoRepo = fotoRepo;
        this.storage = storage;
    }

    @Transactional(readOnly = true)
    public Equipamento buscarEquipamento(Long equipamentoId) {
        return equipamentoRepo.findById(equipamentoId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Equipamento não encontrado."
                        ));
    }

    @Transactional
    public void abrirRegistro(Long equipamentoId) {

        Equipamento equipamento =
                buscarEquipamento(equipamentoId);

        RegistroFotografico registro =
                new RegistroFotografico();

        registro.setEquipamento(equipamento);

        registroRepo.save(registro);
    }

    @Transactional(readOnly = true)
    public List<RegistroComFotos> listar(Long equipamentoId) {

        buscarEquipamento(equipamentoId);

        List<RegistroFotografico> registros =
                registroRepo
                        .findByEquipamento_IdOrderByCriadoEmDesc(
                                equipamentoId
                        );

        List<FotoEquipamento> fotos =
                fotoRepo
                        .findByRegistro_Equipamento_IdOrderByRegistradaEmAsc(
                                equipamentoId
                        );

        Map<Long, List<FotoEquipamento>> agrupadas =
                fotos.stream().collect(
                        Collectors.groupingBy(
                                foto -> foto.getRegistro().getId()
                        )
                );

        return registros.stream()
                .map(registro -> new RegistroComFotos(
                        registro,
                        agrupadas.getOrDefault(
                                registro.getId(),
                                List.of()
                        )
                ))
                .toList();
    }

    @Transactional
    public void enviar(
            Long equipamentoId,
            Long registroId,
            TipoFoto tipo,
            MultipartFile[] arquivos) {

        RegistroFotografico registro = registroRepo
                .findByIdAndEquipamento_Id(
                        registroId,
                        equipamentoId
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Registro não encontrado para este equipamento."
                        ));

        if (tipo == null) {
            throw new IllegalArgumentException(
                    "Selecione entrada ou saída."
            );
        }

        if (arquivos == null
                || arquivos.length == 0
                || arquivos.length > 5) {

            throw new IllegalArgumentException(
                    "Selecione de 1 a 5 fotos por envio."
            );
        }

        List<String> arquivosCriados = new ArrayList<>();

        try {

            for (MultipartFile arquivo : arquivos) {

                FotoStorageService.FotoSalva salva =
                        storage.salvar(arquivo);

                arquivosCriados.add(salva.nomeArquivo());

                FotoEquipamento foto = new FotoEquipamento();

                foto.setRegistro(registro);
                foto.setTipo(tipo);
                foto.setNomeArquivo(salva.nomeArquivo());
                foto.setContentType(salva.contentType());

                fotoRepo.save(foto);
            }

            fotoRepo.flush();

        } catch (RuntimeException ex) {

            // Remove arquivos criados quando o envio falha.
            for (String nome : arquivosCriados) {
                try {
                    storage.remover(nome);
                } catch (RuntimeException erroLimpeza) {
                    ex.addSuppressed(erroLimpeza);
                }
            }

            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public Imagem buscarImagem(
            Long equipamentoId,
            Long fotoId) {

        FotoEquipamento foto = fotoRepo
                .findByIdAndRegistro_Equipamento_Id(
                        fotoId,
                        equipamentoId
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Foto não encontrada."
                        ));

        return new Imagem(
                storage.ler(foto.getNomeArquivo()),
                foto.getContentType()
        );
    }
}
