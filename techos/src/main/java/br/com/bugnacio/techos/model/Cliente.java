
package br.com.bugnacio.techos.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Dados pessoais

    @NotBlank(message = "Nome do cliente é obrigatório")
    @Size(max = 100, message = "O nome não pode ultrapassar 100 caracteres")
    @Column(length = 100, nullable = false)
    private String nome;

    @NotBlank(message = "Informe o telefone do cliente")
    @Size(max = 25, message = "O telefone não pode ultrapassar 25 caracteres")
    private String telefone;

    @Email(message = "Insira um e-mail válido")
    @Size(max = 150, message = "O e-mail não pode ultrapassar 150 caracteres")
    private String email;

    // CPF opcional por enquanto.
    // A validação completa será incluída após verificarmos
    // as máscaras e os dados já cadastrados.

    @Size(max = 14, message = "O CPF não pode ultrapassar 14 caracteres")
    private String cpf;

    // Endereço

    @Size(max = 9, message = "O CEP não pode ultrapassar 9 caracteres")
    private String cep;

    @Size(max = 150, message = "O logradouro não pode ultrapassar 150 caracteres")
    private String logradouro;

    @Size(max = 20, message = "O número não pode ultrapassar 20 caracteres")
    private String numero;

    @Size(max = 150, message = "O complemento não pode ultrapassar 150 caracteres")
    private String complemento;

    @Size(max = 100, message = "O bairro não pode ultrapassar 100 caracteres")
    private String bairro;

    @Size(max = 100, message = "A cidade não pode ultrapassar 100 caracteres")
    private String cidade;

    @Size(max = 50, message = "O estado não pode ultrapassar 50 caracteres")
    private String estado;

    // Data original de cadastro

    @Column(updatable = false)
    private LocalDateTime dataCadastro;

    @PrePersist
    public void prePersist() {
        if (dataCadastro == null) {
            dataCadastro = LocalDateTime.now();
        }
    }

    // Construtor padrão para o JPA

    public Cliente() {
    }

    // Getters e Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getCep() {
        return cep;
    }

    public void setCep(String cep) {
        this.cep = cep;
    }

    public String getLogradouro() {
        return logradouro;
    }

    public void setLogradouro(String logradouro) {
        this.logradouro = logradouro;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getComplemento() {
        return complemento;
    }

    public void setComplemento(String complemento) {
        this.complemento = complemento;
    }

    public String getBairro() {
        return bairro;
    }

    public void setBairro(String bairro) {
        this.bairro = bairro;
    }

    public String getCidade() {
        return cidade;
    }

    public void setCidade(String cidade) {
        this.cidade = cidade;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDateTime getDataCadastro() {
        return dataCadastro;
    }

    public void setDataCadastro(LocalDateTime dataCadastro) {
        this.dataCadastro = dataCadastro;
    }
}
