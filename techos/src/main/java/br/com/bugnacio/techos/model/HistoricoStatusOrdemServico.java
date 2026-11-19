
package br.com.bugnacio.techos.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "historico_status_os",
    indexes = {
        @Index(
            name = "idx_historico_os_data",
            columnList = "ordem_servico_id, registrado_em"
        )
    }
)
public class HistoricoStatusOrdemServico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "ordem_servico_id",
        nullable = false
    )
    private OrdemServico ordemServico;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior", length = 35)
    private StatusOrdemServico statusAnterior;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(
        name = "status_novo",
        nullable = false,
        length = 35
    )
    private StatusOrdemServico statusNovo;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(
        name = "tipo_evento",
        nullable = false,
        length = 40
    )
    private TipoEventoOrdemServico tipoEvento;

    @NotBlank
    @Size(max = 1000)
    @Column(
        name = "observacao",
        nullable = false,
        length = 1000
    )
    private String observacao;

    @NotBlank
    @Size(max = 100)
    @Column(
        name = "responsavel",
        nullable = false,
        length = 100
    )
    private String responsavel;

    @Column(
        name = "registrado_em",
        nullable = false,
        updatable = false
    )
    private LocalDateTime registradoEm;

    protected HistoricoStatusOrdemServico() {
    }

    public HistoricoStatusOrdemServico(
            OrdemServico ordemServico,
            StatusOrdemServico statusAnterior,
            StatusOrdemServico statusNovo,
            TipoEventoOrdemServico tipoEvento,
            String observacao,
            String responsavel) {

        this.ordemServico = ordemServico;
        this.statusAnterior = statusAnterior;
        this.statusNovo = statusNovo;
        this.tipoEvento = tipoEvento;
        this.observacao = observacao;
        this.responsavel = responsavel;
    }

    @PrePersist
    private void antesDeSalvar() {

        if (registradoEm == null) {
            registradoEm = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public OrdemServico getOrdemServico() {
        return ordemServico;
    }

    public StatusOrdemServico getStatusAnterior() {
        return statusAnterior;
    }

    public StatusOrdemServico getStatusNovo() {
        return statusNovo;
    }

    public TipoEventoOrdemServico getTipoEvento() {
        return tipoEvento;
    }

    public String getObservacao() {
        return observacao;
    }

    public String getResponsavel() {
        return responsavel;
    }

    public LocalDateTime getRegistradoEm() {
        return registradoEm;
    }
}
