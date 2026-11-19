
package br.com.bugnacio.techos.service;

import br.com.bugnacio.techos.dto.AbrirOrdemServicoRequest;

import br.com.bugnacio.techos.model.Cliente;
import br.com.bugnacio.techos.model.Equipamento;
import br.com.bugnacio.techos.model.OrdemServico;
import br.com.bugnacio.techos.model.StatusOrdemServico;

import br.com.bugnacio.techos.repository.OrdemServicoRepository;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrdemServicoService {

    private static final int TAMANHO_MAXIMO_PAGINA = 50;

    private final OrdemServicoRepository repository;
    private final EquipamentoService equipamentoService;
    private final Validator validator;

    public OrdemServicoService(
            OrdemServicoRepository repository,
            EquipamentoService equipamentoService,
            Validator validator) {

        this.repository = repository;
        this.equipamentoService = equipamentoService;
        this.validator = validator;
    }

    // ABERTURA DA ORDEM DE SERVIÇO

    @Transactional
    public OrdemServico abrir(
            AbrirOrdemServicoRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Os dados da ordem de serviço são obrigatórios."
            );
        }

        validar(request);

        // Busca o equipamento pelo Service existente.
        Equipamento equipamento =
                equipamentoService.buscarPorId(
                        request.equipamentoId()
                );

        // O cliente deve vir do próprio equipamento.
        Cliente cliente = equipamento.getCliente();

        if (cliente == null || cliente.getId() == null) {
            throw new IllegalStateException(
                    "O equipamento não possui um cliente válido."
            );
        }

        OrdemServico ordem = new OrdemServico();

        ordem.setCliente(cliente);
        ordem.setEquipamento(equipamento);

        ordem.setStatus(StatusOrdemServico.ABERTA);

        ordem.setDefeitoRelatado(
                request.defeitoRelatado().trim()
        );

        ordem.setAcessoriosRecebidos(
                normalizarOpcional(
                        request.acessoriosRecebidos()
                )
        );

        ordem.setEstadoConservacao(
                normalizarOpcional(
                        request.estadoConservacao()
                )
        );

        ordem.setDataPrevistaEntrega(
                request.dataPrevistaEntrega()
        );

        // Validação final antes da persistência.
        validar(ordem);

        return repository.save(ordem);
    }

    // BUSCAR ORDEM DE SERVIÇO POR ID

    @Transactional(readOnly = true)
    public OrdemServico buscarPorId(Long id) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "Informe um ID de ordem de serviço válido."
            );
        }

        return repository.findById(id)
                .orElseThrow(
                        () -> new OrdemServicoNaoEncontradaException(id)
                );
    }

    // LISTAR E PESQUISAR COM PAGINAÇÃO

    @Transactional(readOnly = true)
    public Page<OrdemServico> listar(
            String busca,
            StatusOrdemServico status,
            int pagina,
            int tamanho) {

        if (pagina < 0) {
            throw new IllegalArgumentException(
                    "O número da página não pode ser negativo."
            );
        }

        if (tamanho < 1 || tamanho > TAMANHO_MAXIMO_PAGINA) {
            throw new IllegalArgumentException(
                    "O tamanho da página deve estar entre 1 e 50."
            );
        }

        String termo = normalizarOpcional(busca);

        PageRequest paginacao = PageRequest.of(
                pagina,
                tamanho,
                Sort.by(
                        Sort.Order.desc("criadaEm"),
                        Sort.Order.desc("id")
                )
        );

        return repository.pesquisar(
                termo,
                status,
                paginacao
        );
    }

    // VALIDAÇÃO DOS OBJETOS

    private <T> void validar(T objeto) {

        Set<ConstraintViolation<T>> erros =
                validator.validate(objeto);

        if (!erros.isEmpty()) {
            throw new ConstraintViolationException(erros);
        }
    }

    // NORMALIZAÇÃO DOS CAMPOS OPCIONAIS

    private String normalizarOpcional(String valor) {

        if (valor == null || valor.isBlank()) {
            return null;
        }

        return valor.trim();
    }

    // EXCEÇÃO DE ORDEM NÃO ENCONTRADA

    public static class OrdemServicoNaoEncontradaException
            extends RuntimeException {

        public OrdemServicoNaoEncontradaException(Long id) {

            super(
                    "Ordem de serviço não encontrada: " + id
            );
        }
    }
}
