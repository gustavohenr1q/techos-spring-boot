
package br.com.bugnacio.techos.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

@Entity
@Table(
    name = "ordem_servico",
    indexes = {
        @Index(
            name = "idx_os_cliente",
            columnList = "cliente_id"
        ),
        @Index(
            name = "idx_os_equipamento",
            columnList = "equipamento_id"
        ),
        @Index(
            name = "idx_os_status",
            columnList = "status"
        ),
        @Index(
            name = "idx_os_criada_em",
            columnList = "criada_em"
        )
    }
)
public class OrdemServico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "versao", nullable = false)
    private Long versao;

    // CLIENTE E EQUIPAMENTO

    @NotNull(message = "O cliente é obrigatório.")
    @ManyToOne(
        fetch = FetchType.LAZY,
        optional = false
    )
    @JoinColumn(
        name = "cliente_id",
        nullable = false
    )
    private Cliente cliente;

    @NotNull(message = "O equipamento é obrigatório.")
    @ManyToOne(
        fetch = FetchType.LAZY,
        optional = false
    )
    @JoinColumn(
        name = "equipamento_id",
        nullable = false
    )
    private Equipamento equipamento;

    // STATUS

    @NotNull(message = "O status é obrigatório.")
    @Enumerated(EnumType.STRING)
    @Column(
        name = "status",
        nullable = false,
        length = 35
    )
    private StatusOrdemServico status =
            StatusOrdemServico.ABERTA;

    // RECEBIMENTO

    @NotBlank(
        message = "Informe o defeito relatado pelo cliente."
    )
    @Size(
        max = 4000,
        message = "O defeito relatado deve ter até 4000 caracteres."
    )
    @Column(
        name = "defeito_relatado",
        nullable = false,
        columnDefinition = "TEXT"
    )
    private String defeitoRelatado;

    @Size(max = 2000)
    @Column(
        name = "acessorios_recebidos",
        columnDefinition = "TEXT"
    )
    private String acessoriosRecebidos;

    @Size(max = 2000)
    @Column(
        name = "estado_conservacao",
        columnDefinition = "TEXT"
    )
    private String estadoConservacao;

    // ANÁLISE TÉCNICA

    @Size(max = 4000)
    @Column(
        name = "diagnostico",
        columnDefinition = "TEXT"
    )
    private String diagnostico;

    @Size(max = 4000)
    @Column(
        name = "servico_executado",
        columnDefinition = "TEXT"
    )
    private String servicoExecutado;

    @Size(max = 4000)
    @Column(
        name = "observacoes_internas",
        columnDefinition = "TEXT"
    )
    private String observacoesInternas;

    // VALORES

    @NotNull
    @DecimalMin(
        value = "0.00",
        message = "O valor não pode ser negativo."
    )
    @Digits(integer = 10, fraction = 2)
    @Column(
        name = "valor_mao_de_obra",
        nullable = false,
        precision = 12,
        scale = 2
    )
    private BigDecimal valorMaoDeObra =
            BigDecimal.ZERO;

    @NotNull
    @DecimalMin(
        value = "0.00",
        message = "O valor não pode ser negativo."
    )
    @Digits(integer = 10, fraction = 2)
    @Column(
        name = "valor_pecas",
        nullable = false,
        precision = 12,
        scale = 2
    )
    private BigDecimal valorPecas =
            BigDecimal.ZERO;

    // DATAS

    @Column(
        name = "criada_em",
        nullable = false,
        updatable = false
    )
    private LocalDateTime criadaEm;

    @Column(
        name = "atualizada_em",
        nullable = false
    )
    private LocalDateTime atualizadaEm;

    @Column(name = "data_prevista_entrega")
    private LocalDate dataPrevistaEntrega;

    @Column(name = "data_conclusao")
    private LocalDateTime dataConclusao;

    @Column(name = "data_entrega")
    private LocalDateTime dataEntrega;

    // EVENTOS JPA

    @PrePersist
    private void antesDeCriar() {

        LocalDateTime agora = LocalDateTime.now();

        if (criadaEm == null) {
            criadaEm = agora;
        }

        atualizadaEm = agora;
    }

    @PreUpdate
    private void antesDeAtualizar() {

        atualizadaEm = LocalDateTime.now();
    }

    // CAMPOS CALCULADOS

    @Transient
    public String getProtocolo() {

        if (id == null) {
            return null;
        }

        return String.format(
            Locale.ROOT,
            "OS-%06d",
            id
        );
    }

    @Transient
    public BigDecimal getValorTotal() {

        BigDecimal maoDeObra =
                valorMaoDeObra == null
                    ? BigDecimal.ZERO
                    : valorMaoDeObra;

        BigDecimal pecas =
                valorPecas == null
                    ? BigDecimal.ZERO
                    : valorPecas;

        return maoDeObra.add(pecas);
    }

    // GETTERS E SETTERS

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVersao() {
        return versao;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public Equipamento getEquipamento() {
        return equipamento;
    }

    public void setEquipamento(Equipamento equipamento) {
        this.equipamento = equipamento;
    }

    public StatusOrdemServico getStatus() {
        return status;
    }

    public void setStatus(StatusOrdemServico status) {
        this.status = status;
    }

    public String getDefeitoRelatado() {
        return defeitoRelatado;
    }

    public void setDefeitoRelatado(String defeitoRelatado) {
        this.defeitoRelatado = defeitoRelatado;
    }

    public String getAcessoriosRecebidos() {
        return acessoriosRecebidos;
    }

    public void setAcessoriosRecebidos(String acessoriosRecebidos) {
        this.acessoriosRecebidos = acessoriosRecebidos;
    }

    public String getEstadoConservacao() {
        return estadoConservacao;
    }

    public void setEstadoConservacao(String estadoConservacao) {
        this.estadoConservacao = estadoConservacao;
    }

    public String getDiagnostico() {
        return diagnostico;
    }

    public void setDiagnostico(String diagnostico) {
        this.diagnostico = diagnostico;
    }

    public String getServicoExecutado() {
        return servicoExecutado;
    }

    public void setServicoExecutado(String servicoExecutado) {
        this.servicoExecutado = servicoExecutado;
    }

    public String getObservacoesInternas() {
        return observacoesInternas;
    }

    public void setObservacoesInternas(String observacoesInternas) {
        this.observacoesInternas = observacoesInternas;
    }

    public BigDecimal getValorMaoDeObra() {
        return valorMaoDeObra;
    }

    public void setValorMaoDeObra(BigDecimal valorMaoDeObra) {
        this.valorMaoDeObra = valorMaoDeObra;
    }

    public BigDecimal getValorPecas() {
        return valorPecas;
    }

    public void setValorPecas(BigDecimal valorPecas) {
        this.valorPecas = valorPecas;
    }

    public LocalDateTime getCriadaEm() {
        return criadaEm;
    }

    public LocalDateTime getAtualizadaEm() {
        return atualizadaEm;
    }

    public LocalDate getDataPrevistaEntrega() {
        return dataPrevistaEntrega;
    }

    public void setDataPrevistaEntrega(
            LocalDate dataPrevistaEntrega) {

        this.dataPrevistaEntrega =
                dataPrevistaEntrega;
    }

    public LocalDateTime getDataConclusao() {
        return dataConclusao;
    }

    public void setDataConclusao(
            LocalDateTime dataConclusao) {

        this.dataConclusao = dataConclusao;
    }

    public LocalDateTime getDataEntrega() {
        return dataEntrega;
    }

    public void setDataEntrega(
            LocalDateTime dataEntrega) {

        this.dataEntrega = dataEntrega;
    }
}
