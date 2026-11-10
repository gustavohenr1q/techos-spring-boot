package br.com.bugnacio.techos.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// @Entity: essa classe representa uma tabela no banco de dados.
@Entity
public class Cliente {
    // @id: o campo que representa a chave primaria da tabela.
    // @GeneratedValue: o valor da chave primaria e gerado automaticamente.
@Id 
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;

@NotBlank (message = "Nome do cliente é obrigatório")
@Size (max = 100, message = "o nome do cliente não pode ter mais de 100 caractéres")

// Cada atributo vira uma coluna na tabela. O nome da coluna e o mesmo do atributo.
private String nome;



@NotBlank (message = "Informe o telefone do cliente")
private String telefone;

@Email (message = "Insira um e-mail válido")
private String email;

// Construtor padrao (sem argumentos) necessario para o JPA.
public Cliente() {

}

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

}