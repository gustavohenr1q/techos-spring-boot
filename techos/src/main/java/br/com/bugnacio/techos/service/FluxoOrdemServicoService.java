
package br.com.bugnacio.techos.service;

import br.com.bugnacio.techos.dto.AbrirOrdemServicoRequest;

import br.com.bugnacio.techos.model.HistoricoStatusOrdemServico;
import br.com.bugnacio.techos.model.OrdemServico;
import br.com.bugnacio.techos.model.StatusOrdemServico;
import br.com.bugnacio.techos.model.TipoEventoOrdemServico;

import br.com.bugnacio.techos.repository.HistoricoStatusOrdemServicoRepository;
import br.com.bugnacio.techos.repository.OrdemServicoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static br.com.bugnacio.techos.model.StatusOrdemServico.*;

@Service
public class FluxoOrdemServicoService {

    private static final Map<
            StatusOrdemServico,
            Set<StatusOrdemServico>
    > TRANSICOES = Map.of(

            ABERTA,
            Set.of(EM_DIAGNOSTICO),

            EM_DIAGNOSTICO,
            Set.of(AGUARDANDO_APROVACAO),

            AGUARDANDO_APROVACAO,
            Set.of(EM_DIAGNOSTICO),

            EM_REPARO,
            Set.of(
                    AGUARDANDO_PECA,
                    PRONTA_PARA_ENTREGA
            ),

            AGUARDANDO_PECA,
            Set.of(EM_REPARO),

            PRONTA_PARA_ENTREGA,
            Set.of(EM_REPARO, ENTREGUE),

            ENTREGUE,
            Set.of(),

            CANCELADA,
            Set.of()
    );

    private final OrdemServicoRepository repository;
    private final HistoricoStatusOrdemServicoRepository
            historicoRepository;
    private final OrdemServicoService ordemServicoService;

    public FluxoOrdemServicoService(
            OrdemServicoRepository repository,
            HistoricoStatusOrdemServicoRepository historicoRepository,
            OrdemServicoService ordemServicoService) {

        this.repository = repository;
        this.historicoRepository = historicoRepository;
        this.ordemServicoService = ordemServicoService;
    }

    // ABERTURA COM REGISTRO NO HISTÓRICO

    @Transactional
    public OrdemServico abrir(
            AbrirOrdemServicoRequest request,
            String responsavel) {

        String operador = validarTexto(
                responsavel,
                "Responsável",
                100
        );

        OrdemServico ordem = ordemServicoService.abrir(request);

        registrar(
                ordem,
                null,
                ABERTA,
                TipoEventoOrdemServico.ABERTURA,
                "Ordem de serviço aberta.",
                operador
        );

        return ordem;
    }

    // ALTERAÇÃO NORMAL DE STATUS

    @Transactional
    public OrdemServico alterarStatus(
            Long id,
            StatusOrdemServico novoStatus,
            String observacao,
            String responsavel) {

        String motivo = validarTexto(
                observacao,
                "Observação",
                1000
        );

        String operador = validarTexto(
                responsavel,
                "Responsável",
                100
        );

        if (novoStatus == null) {
            throw new IllegalArgumentException(
                    "Informe o novo status."
            );
        }

        OrdemServico ordem = buscar(id);

        StatusOrdemServico atual = ordem.getStatus();

        Set<StatusOrdemServico> permitidos =
                TRANSICOES.getOrDefault(
                        atual,
                        Set.of()
                );

        if (!permitidos.contains(novoStatus)) {
            throw new TransicaoInvalidaException(
                    atual,
                    novoStatus
            );
        }

        return atualizarStatus(
                ordem,
                novoStatus,
                TipoEventoOrdemServico.ALTERACAO_STATUS,
                motivo,
                operador
        );
    }

    // APROVAÇÃO DO ORÇAMENTO

    @Transactional
    public OrdemServico aprovarOrcamento(
            Long id,
            String observacao,
            String responsavel) {

        String motivo = validarTexto(
                observacao,
                "Observação da aprovação",
                1000
        );

        String operador = validarTexto(
                responsavel,
                "Responsável",
                100
        );

        OrdemServico ordem = buscar(id);

        exigirStatus(
                ordem,
                AGUARDANDO_APROVACAO
        );

        return atualizarStatus(
                ordem,
                EM_REPARO,
                TipoEventoOrdemServico.ORCAMENTO_APROVADO,
                motivo,
                operador
        );
    }

    // RECUSA DO ORÇAMENTO

    @Transactional
    public OrdemServico recusarOrcamento(
            Long id,
            String observacao,
            String responsavel) {

        String motivo = validarTexto(
                observacao,
                "Motivo da recusa",
                1000
        );

        String operador = validarTexto(
                responsavel,
                "Responsável",
                100
        );

        OrdemServico ordem = buscar(id);

        exigirStatus(
                ordem,
                AGUARDANDO_APROVACAO
        );

        return atualizarStatus(
                ordem,
                CANCELADA,
                TipoEventoOrdemServico.ORCAMENTO_RECUSADO,
                motivo,
                operador
        );
    }

    // CANCELAMENTO CONTROLADO

    @Transactional
    public OrdemServico cancelar(
            Long id,
            String motivo,
            String responsavel) {

        String justificativa = validarTexto(
                motivo,
                "Motivo do cancelamento",
                1000
        );

        String operador = validarTexto(
                responsavel,
                "Responsável",
                100
        );

        OrdemServico ordem = buscar(id);

        if (!Set.of(
                ABERTA,
                EM_DIAGNOSTICO,
                AGUARDANDO_APROVACAO
        ).contains(ordem.getStatus())) {

            throw new IllegalStateException(
                    "Esta ordem não pode ser cancelada "
                            + "nesta etapa do atendimento."
            );
        }

        return atualizarStatus(
                ordem,
                CANCELADA,
                TipoEventoOrdemServico.CANCELAMENTO,
                justificativa,
                operador
        );
    }

    // CONSULTAR HISTÓRICO

    @Transactional(readOnly = true)
    public List<HistoricoStatusOrdemServico> listarHistorico(
            Long ordemServicoId) {

        buscar(ordemServicoId);

        return historicoRepository
                .findByOrdemServico_IdOrderByIdAsc(
                        ordemServicoId
                );
    }

    // ATUALIZAR STATUS E REGISTRAR EVENTO

    private OrdemServico atualizarStatus(
            OrdemServico ordem,
            StatusOrdemServico novoStatus,
            TipoEventoOrdemServico tipoEvento,
            String observacao,
            String responsavel) {

        StatusOrdemServico anterior = ordem.getStatus();

        if (anterior == novoStatus) {
            throw new TransicaoInvalidaException(
                    anterior,
                    novoStatus
            );
        }

        ordem.setStatus(novoStatus);

        if (novoStatus == PRONTA_PARA_ENTREGA) {
            ordem.setDataConclusao(LocalDateTime.now());
        }

        if (anterior == PRONTA_PARA_ENTREGA
                && novoStatus == EM_REPARO) {

            ordem.setDataConclusao(null);
        }

        if (novoStatus == ENTREGUE) {
            ordem.setDataEntrega(LocalDateTime.now());
        }

        // Força a atualização dentro da transação.
        repository.saveAndFlush(ordem);

        registrar(
                ordem,
                anterior,
                novoStatus,
                tipoEvento,
                observacao,
                responsavel
        );

        return ordem;
    }

    // REGISTRO DE HISTÓRICO

    private void registrar(
            OrdemServico ordem,
            StatusOrdemServico anterior,
            StatusOrdemServico novo,
            TipoEventoOrdemServico tipo,
            String observacao,
            String responsavel) {

        HistoricoStatusOrdemServico evento =
                new HistoricoStatusOrdemServico(
                        ordem,
                        anterior,
                        novo,
                        tipo,
                        observacao,
                        responsavel
                );

        historicoRepository.save(evento);
    }

    // LOCALIZAR OS

    private OrdemServico buscar(Long id) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "ID da ordem de serviço inválido."
            );
        }

        return repository.findById(id)
                .orElseThrow(
                        () -> new OrdemServicoService
                                .OrdemServicoNaoEncontradaException(id)
                );
    }

    // VERIFICAR STATUS EXIGIDO

    private void exigirStatus(
            OrdemServico ordem,
            StatusOrdemServico esperado) {

        if (ordem.getStatus() != esperado) {
            throw new IllegalStateException(
                    "A ordem precisa estar no status "
                            + esperado
                            + " para realizar esta operação."
            );
        }
    }

    // VALIDAR TEXTOS

    private String validarTexto(
            String valor,
            String campo,
            int tamanhoMaximo) {

        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(
                    campo + " é obrigatório."
            );
        }

        String normalizado = valor.trim();

        if (normalizado.length() > tamanhoMaximo) {
            throw new IllegalArgumentException(
                    campo + " ultrapassa o tamanho permitido."
            );
        }

        return normalizado;
    }

    // ERRO DE TRANSIÇÃO INVÁLIDA

    public static class TransicaoInvalidaException
            extends IllegalStateException {

        public TransicaoInvalidaException(
                StatusOrdemServico atual,
                StatusOrdemServico destino) {

            super(
                    "Não é permitido mudar o status de "
                            + atual + " para " + destino + "."
            );
        }
    }
}
