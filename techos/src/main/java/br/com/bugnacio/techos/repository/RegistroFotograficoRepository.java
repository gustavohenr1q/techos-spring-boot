
package br.com.bugnacio.techos.repository;

import br.com.bugnacio.techos.model.RegistroFotografico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RegistroFotograficoRepository
        extends JpaRepository<RegistroFotografico, Long> {

    List<RegistroFotografico>
    findByEquipamento_IdOrderByCriadoEmDesc(Long equipamentoId);

    Optional<RegistroFotografico>
    findByIdAndEquipamento_Id(Long id, Long equipamentoId);
}
