package br.com.bugnacio.techos.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.bugnacio.techos.model.Cliente;

// Interface (Não é classe): Não escrevemos o corpo da classe, o Spring Data JPA faz isso para nós.
// A interface JpaRepository já possui métodos prontos para salvar, buscar, atualizar e deletar registros
// no banco de dados


public interface ClienteRepository extends JpaRepository <Cliente, Long> {

    boolean existsByTelefone(String telefone);

    boolean existsByTelefoneAndIdNot(String telefone,Long id);

    List<Cliente> findByNomeContainingIgnoreCase(String nome);


}
