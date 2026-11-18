
package br.com.bugnacio.techos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
public class Equipamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // TIPO DO EQUIPAMENTO

    @NotNull(message = "O tipo do equipamento é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private TipoEquipamento tipo;

    // MARCA

    @NotBlank(message = "A marca do equipamento é obrigatória")
    @Size(max = 255, message = "A marca não pode ultrapassar 255 caracteres")
    private String marca;

    // MODELO

    @NotBlank(message = "O modelo do equipamento é obrigatório")
    @Size(max = 255, message = "O modelo não pode ultrapassar 255 caracteres")
    private String modelo;

    // NÚMERO DE SÉRIE

    @NotBlank(message = "O número de série é obrigatório")
    @Size(max = 255, message = "O número de série não pode ultrapassar 255 caracteres")
    private String numeroSerie;

    // CLIENTE RESPONSÁVEL PELO EQUIPAMENTO

    @NotNull(message = "Selecione o cliente responsável pelo equipamento")
    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    // CONSTRUTOR PADRÃO PARA O JPA

    public Equipamento() {
    }

    // GETTERS E SETTERS

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TipoEquipamento getTipo() {
        return tipo;
    }

    public void setTipo(TipoEquipamento tipo) {
        this.tipo = tipo;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public String getNumeroSerie() {
        return numeroSerie;
    }

    public void setNumeroSerie(String numeroSerie) {
        this.numeroSerie = numeroSerie;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }
}
