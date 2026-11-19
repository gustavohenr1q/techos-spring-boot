
package br.com.bugnacio.techos.repository;

import br.com.bugnacio.techos.model.HistoricoStatusOrdemServico;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoricoStatusOrdemServicoRepository
        extends JpaRepository<HistoricoStatusOrdemServico, Long> {

    List<HistoricoStatusOrdemServico>
        findByOrdemServico_IdOrderByIdAsc(Long ordemServicoId);

}
