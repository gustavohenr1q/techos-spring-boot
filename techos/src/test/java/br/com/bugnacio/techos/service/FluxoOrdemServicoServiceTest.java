
package br.com.bugnacio.techos.service;

import br.com.bugnacio.techos.dto.AbrirOrdemServicoRequest;

import br.com.bugnacio.techos.model.HistoricoStatusOrdemServico;
import br.com.bugnacio.techos.model.OrdemServico;
import br.com.bugnacio.techos.model.StatusOrdemServico;
import br.com.bugnacio.techos.model.TipoEventoOrdemServico;

import br.com.bugnacio.techos.repository.HistoricoStatusOrdemServicoRepository;
import br.com.bugnacio.techos.repository.OrdemServicoRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static br.com.bugnacio.techos.model.StatusOrdemServico.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class FluxoOrdemServicoServiceTest {

    @Mock
    private OrdemServicoRepository repository;

    @Mock
    private HistoricoStatusOrdemServicoRepository historicoRepository;

    @Mock
    private OrdemServicoService ordemServicoService;

    private FluxoOrdemServicoService service;

    @BeforeEach
    void preparar() {

        service = new FluxoOrdemServicoService(
                repository,
                historicoRepository,
                ordemServicoService
        );
    }

    private OrdemServico carregar(
            StatusOrdemServico status) {

        OrdemServico ordem = new OrdemServico();

        ordem.setId(10L);
        ordem.setStatus(status);

        when(repository.findById(10L))
                .thenReturn(Optional.of(ordem));

        return ordem;
    }

    @Test
    void deveRegistrarAbertura() {

        AbrirOrdemServicoRequest request =
                new AbrirOrdemServicoRequest(
                        2L, "Não liga", null, null, null
                );

        OrdemServico ordem = new OrdemServico();
        ordem.setId(10L);

        when(ordemServicoService.abrir(request))
                .thenReturn(ordem);

        service.abrir(request, " Técnico ");

        ArgumentCaptor<HistoricoStatusOrdemServico> captor =
                ArgumentCaptor.forClass(
                        HistoricoStatusOrdemServico.class
                );

        verify(historicoRepository).save(captor.capture());

        HistoricoStatusOrdemServico evento =
                captor.getValue();

        assertNull(evento.getStatusAnterior());
        assertEquals(ABERTA, evento.getStatusNovo());

        assertEquals(
                TipoEventoOrdemServico.ABERTURA,
                evento.getTipoEvento()
        );

        assertEquals("Técnico", evento.getResponsavel());
    }

    @Test
    void naoDeveAbrirSemResponsavel() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.abrir(null, " ")
        );

        verifyNoInteractions(
                repository,
                historicoRepository,
                ordemServicoService
        );
    }

    @Test
    void deveIniciarDiagnostico() {

        OrdemServico ordem = carregar(ABERTA);

        service.alterarStatus(
                10L,
                EM_DIAGNOSTICO,
                "Iniciando análise",
                "Técnico"
        );

        assertEquals(EM_DIAGNOSTICO, ordem.getStatus());

        verify(repository).saveAndFlush(ordem);

        verify(historicoRepository).save(
                any(HistoricoStatusOrdemServico.class)
        );
    }

    @Test
    void deveEncaminharParaAprovacao() {

        OrdemServico ordem = carregar(EM_DIAGNOSTICO);

        service.alterarStatus(
                10L,
                AGUARDANDO_APROVACAO,
                "Orçamento enviado",
                "Técnico"
        );

        assertEquals(
                AGUARDANDO_APROVACAO,
                ordem.getStatus()
        );
    }

    @Test
    void naoDeveIniciarReparoSemAprovacao() {

        carregar(AGUARDANDO_APROVACAO);

        assertThrows(
                FluxoOrdemServicoService
                        .TransicaoInvalidaException.class,
                () -> service.alterarStatus(
                        10L,
                        EM_REPARO,
                        "Iniciar reparo",
                        "Técnico"
                )
        );

        verify(repository, never()).saveAndFlush(any());
        verifyNoInteractions(historicoRepository);
    }

    @Test
    void deveAprovarOrcamento() {

        OrdemServico ordem = carregar(AGUARDANDO_APROVACAO);

        service.aprovarOrcamento(
                10L,
                "Cliente autorizou o orçamento",
                "Atendente"
        );

        assertEquals(EM_REPARO, ordem.getStatus());

        ArgumentCaptor<HistoricoStatusOrdemServico> captor =
                ArgumentCaptor.forClass(
                        HistoricoStatusOrdemServico.class
                );

        verify(historicoRepository).save(captor.capture());

        assertEquals(
                TipoEventoOrdemServico.ORCAMENTO_APROVADO,
                captor.getValue().getTipoEvento()
        );
    }

    @Test
    void deveRecusarOrcamento() {

        OrdemServico ordem = carregar(AGUARDANDO_APROVACAO);

        service.recusarOrcamento(
                10L,
                "Cliente não autorizou",
                "Atendente"
        );

        assertEquals(CANCELADA, ordem.getStatus());

        ArgumentCaptor<HistoricoStatusOrdemServico> captor =
                ArgumentCaptor.forClass(
                        HistoricoStatusOrdemServico.class
                );

        verify(historicoRepository).save(captor.capture());

        assertEquals(
                TipoEventoOrdemServico.ORCAMENTO_RECUSADO,
                captor.getValue().getTipoEvento()
        );
    }

    @Test
    void naoDeveAprovarOrcamentoEmStatusIncorreto() {

        carregar(ABERTA);

        assertThrows(
                IllegalStateException.class,
                () -> service.aprovarOrcamento(
                        10L,
                        "Aprovado",
                        "Atendente"
                )
        );

        verifyNoInteractions(historicoRepository);
    }

    @Test
    void deveCancelarOrdemAberta() {

        OrdemServico ordem = carregar(ABERTA);

        service.cancelar(
                10L,
                "Cliente desistiu do atendimento",
                "Atendente"
        );

        assertEquals(CANCELADA, ordem.getStatus());

        verify(historicoRepository).save(
                any(HistoricoStatusOrdemServico.class)
        );
    }

    @Test
    void naoDeveCancelarOrdemEntregue() {

        carregar(ENTREGUE);

        assertThrows(
                IllegalStateException.class,
                () -> service.cancelar(
                        10L,
                        "Cancelar",
                        "Atendente"
                )
        );
    }

    @Test
    void naoDevePermitirMesmoStatus() {

        carregar(ABERTA);

        assertThrows(
                FluxoOrdemServicoService
                        .TransicaoInvalidaException.class,
                () -> service.alterarStatus(
                        10L,
                        ABERTA,
                        "Sem alteração",
                        "Técnico"
                )
        );
    }

    @Test
    void deveMarcarDataDeConclusao() {

        OrdemServico ordem = carregar(EM_REPARO);

        service.alterarStatus(
                10L,
                PRONTA_PARA_ENTREGA,
                "Testes finalizados",
                "Técnico"
        );

        assertNotNull(ordem.getDataConclusao());
    }

    @Test
    void deveRegistrarDataDaEntrega() {

        OrdemServico ordem = carregar(PRONTA_PARA_ENTREGA);

        service.alterarStatus(
                10L,
                ENTREGUE,
                "Entregue ao cliente",
                "Atendente"
        );

        assertNotNull(ordem.getDataEntrega());
    }

    @Test
    void deveLimparConclusaoAoVoltarParaReparo() {

        OrdemServico ordem = carregar(PRONTA_PARA_ENTREGA);

        ordem.setDataConclusao(
                LocalDateTime.now().minusDays(1)
        );

        service.alterarStatus(
                10L,
                EM_REPARO,
                "Falha encontrada nos testes",
                "Técnico"
        );

        assertNull(ordem.getDataConclusao());
    }

    @Test
    void naoDeveAceitarObservacaoVazia() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.alterarStatus(
                        10L,
                        EM_DIAGNOSTICO,
                        " ",
                        "Técnico"
                )
        );

        verifyNoInteractions(repository);
    }

    @Test
    void deveListarHistorico() {

        OrdemServico ordem = carregar(ABERTA);

        HistoricoStatusOrdemServico evento =
                new HistoricoStatusOrdemServico(
                        ordem,
                        null,
                        ABERTA,
                        TipoEventoOrdemServico.ABERTURA,
                        "Ordem aberta",
                        "Atendente"
                );

        when(historicoRepository
                .findByOrdemServico_IdOrderByIdAsc(10L))
                .thenReturn(List.of(evento));

        List<HistoricoStatusOrdemServico> resultado =
                service.listarHistorico(10L);

        assertEquals(1, resultado.size());
        assertSame(evento, resultado.get(0));
    }

    @Test
    void naoDeveAlterarOrdemInexistente() {

        when(repository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                OrdemServicoService
                        .OrdemServicoNaoEncontradaException.class,
                () -> service.alterarStatus(
                        99L,
                        EM_DIAGNOSTICO,
                        "Iniciar diagnóstico",
                        "Técnico"
                )
        );

        verifyNoInteractions(historicoRepository);
    }

    @Test
    void naoDeveAlterarOrdemCancelada() {

        carregar(CANCELADA);

        assertThrows(
                FluxoOrdemServicoService
                        .TransicaoInvalidaException.class,
                () -> service.alterarStatus(
                        10L,
                        EM_DIAGNOSTICO,
                        "Tentar reabrir",
                        "Técnico"
                )
        );

        verifyNoInteractions(historicoRepository);
    }
}
