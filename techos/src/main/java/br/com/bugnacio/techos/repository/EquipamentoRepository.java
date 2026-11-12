
package br.com.bugnacio.techos.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.bugnacio.techos.model.Equipamento;
import br.com.bugnacio.techos.model.TipoEquipamento;

public interface EquipamentoRepository extends JpaRepository<Equipamento, Long> {

    boolean existsByNumeroSerie(String numeroSerie);

    boolean existsByNumeroSerieAndIdNot(String numeroSerie, Long id);

    @Query("""
                SELECT e FROM Equipamento e
                JOIN e.cliente c
                WHERE
                    (:busca IS NULL OR
                        LOWER(e.marca) LIKE LOWER(CONCAT('%', :busca, '%')) OR
                        LOWER(e.modelo) LIKE LOWER(CONCAT('%', :busca, '%')) OR
                        LOWER(e.numeroSerie) LIKE LOWER(CONCAT('%', :busca, '%')) OR
                        LOWER(c.nome) LIKE LOWER(CONCAT('%', :busca, '%'))
                    )
                    AND (:tipo IS NULL OR e.tipo = :tipo)
                ORDER BY e.marca
            """)
    List<Equipamento> buscar(
            @Param("busca") String busca,
            @Param("tipo") TipoEquipamento tipo);
}
