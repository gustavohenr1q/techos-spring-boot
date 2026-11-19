
package br.com.bugnacio.techos.repository;

import br.com.bugnacio.techos.model.OrdemServico;
import br.com.bugnacio.techos.model.StatusOrdemServico;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;

public interface OrdemServicoRepository
        extends JpaRepository<OrdemServico, Long> {

    @Override
    @EntityGraph(attributePaths = {
            "cliente",
            "equipamento"
    })
    Optional<OrdemServico> findById(Long id);

    @EntityGraph(attributePaths = {
            "cliente",
            "equipamento"
    })
    @Query("""
            SELECT os
            FROM OrdemServico os
            JOIN os.cliente c
            JOIN os.equipamento e
            WHERE (:status IS NULL OR os.status = :status)
              AND (
                  :busca IS NULL
                  OR LOWER(c.nome) LIKE LOWER(CONCAT('%', :busca, '%'))
                  OR LOWER(e.marca) LIKE LOWER(CONCAT('%', :busca, '%'))
                  OR LOWER(e.modelo) LIKE LOWER(CONCAT('%', :busca, '%'))
                  OR LOWER(e.numeroSerie) LIKE LOWER(CONCAT('%', :busca, '%'))
              )
            """)
    Page<OrdemServico> pesquisar(
            @Param("busca") String busca,
            @Param("status") StatusOrdemServico status,
            Pageable pageable
    );

    boolean existsByEquipamento_Id(Long equipamentoId);
}
