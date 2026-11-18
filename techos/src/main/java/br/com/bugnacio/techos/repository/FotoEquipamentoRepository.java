
package br.com.bugnacio.techos.repository;

import br.com.bugnacio.techos.model.FotoEquipamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FotoEquipamentoRepository
        extends JpaRepository<FotoEquipamento, Long> {

    List<FotoEquipamento>
    findByRegistro_Equipamento_IdOrderByRegistradaEmAsc(
            Long equipamentoId
    );

    Optional<FotoEquipamento>
    findByIdAndRegistro_Equipamento_Id(
            Long id,
            Long equipamentoId
    );
}
