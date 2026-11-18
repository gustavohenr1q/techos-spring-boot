
package br.com.bugnacio.techos.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "foto_equipamento")
public class FotoEquipamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registro_id", nullable = false)
    private RegistroFotografico registro;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoFoto tipo;

    @Column(nullable = false, unique = true, length = 70)
    private String nomeArquivo;

    @Column(nullable = false, length = 20)
    private String contentType;

    @Column(nullable = false, updatable = false)
    private LocalDateTime registradaEm;

    @PrePersist
    void registrarData() {
        if (registradaEm == null) {
            registradaEm = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public RegistroFotografico getRegistro() {
        return registro;
    }

    public void setRegistro(RegistroFotografico registro) {
        this.registro = registro;
    }

    public TipoFoto getTipo() {
        return tipo;
    }

    public void setTipo(TipoFoto tipo) {
        this.tipo = tipo;
    }

    public String getNomeArquivo() {
        return nomeArquivo;
    }

    public void setNomeArquivo(String nomeArquivo) {
        this.nomeArquivo = nomeArquivo;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public LocalDateTime getRegistradaEm() {
        return registradaEm;
    }
}
