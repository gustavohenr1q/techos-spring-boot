
package br.com.bugnacio.techos.service;

import br.com.bugnacio.techos.model.Cliente;
import br.com.bugnacio.techos.model.Equipamento;
import br.com.bugnacio.techos.model.TipoEquipamento;

import br.com.bugnacio.techos.repository.ClienteRepository;
import br.com.bugnacio.techos.repository.EquipamentoRepository;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class EquipamentoService {

    private final EquipamentoRepository repo;
    private final ClienteRepository clienteRepo;
    private final Validator validator;

    // INJEÇÃO DE DEPENDÊNCIAS

    public EquipamentoService(
            EquipamentoRepository repo,
            ClienteRepository clienteRepo,
            Validator validator) {

        this.repo = repo;
        this.clienteRepo = clienteRepo;
        this.validator = validator;
    }

    // LISTAR E PESQUISAR EQUIPAMENTOS

    @Transactional(readOnly = true)
    public List<Equipamento> listar(
            String busca,
            TipoEquipamento tipo) {

        String termo = busca == null || busca.isBlank()
                ? null
                : busca.trim();

        return repo.buscar(termo, tipo);
    }

    // LISTAR CLIENTES PARA O FORMULÁRIO

    @Transactional(readOnly = true)
    public List<Cliente> listarClientes() {
        return clienteRepo.findAll(Sort.by("nome"));
    }

    // BUSCAR EQUIPAMENTO PELO ID

    @Transactional(readOnly = true)
    public Equipamento buscarPorId(Long id) {

        if (id == null) {
            throw new IllegalArgumentException(
                    "Informe o ID do equipamento."
            );
        }

        return repo.findById(id)
                .orElseThrow(
                        () -> new EquipamentoNaoEncontradoException(id)
                );
    }

    // CADASTRAR EQUIPAMENTO

    @Transactional
    public Equipamento cadastrar(
            Equipamento equipamento,
            Long clienteId) {

        if (equipamento == null) {
            throw new IllegalArgumentException(
                    "Informe os dados do equipamento."
            );
        }

        if (equipamento.getId() != null) {
            throw new IllegalArgumentException(
                    "Um novo equipamento não pode possuir ID."
            );
        }

        Cliente cliente = buscarCliente(clienteId);

        equipamento.setCliente(cliente);

        normalizarDados(equipamento);
        validarEquipamento(equipamento);

        verificarNumeroSerieDuplicado(
                equipamento.getNumeroSerie(),
                null
        );

        return repo.save(equipamento);
    }

    // ATUALIZAR EQUIPAMENTO

    @Transactional
    public Equipamento atualizar(
            Long id,
            Equipamento dados,
            Long clienteId) {

        if (id == null || dados == null) {
            throw new IllegalArgumentException(
                    "Dados de atualização inválidos."
            );
        }

        if (dados.getId() != null
                && !id.equals(dados.getId())) {

            throw new IllegalArgumentException(
                    "O ID informado não corresponde ao equipamento."
            );
        }

        Equipamento existente = buscarPorId(id);

        Cliente cliente = buscarCliente(clienteId);

        dados.setCliente(cliente);

        normalizarDados(dados);
        validarEquipamento(dados);

        verificarNumeroSerieDuplicado(
                dados.getNumeroSerie(),
                id
        );

        // Atualiza somente os campos permitidos.

        existente.setTipo(dados.getTipo());
        existente.setMarca(dados.getMarca());
        existente.setModelo(dados.getModelo());
        existente.setNumeroSerie(dados.getNumeroSerie());
        existente.setCliente(cliente);

        return repo.save(existente);
    }

    // EXCLUIR EQUIPAMENTO

    @Transactional
    public void excluir(Long id) {

        Equipamento equipamento = buscarPorId(id);

        repo.delete(equipamento);
        repo.flush();
    }

    // VERIFICAR CLIENTE

    private Cliente buscarCliente(Long clienteId) {

        if (clienteId == null) {
            throw new IllegalArgumentException(
                    "Selecione um cliente."
            );
        }

        return clienteRepo.findById(clienteId)
                .orElseThrow(
                        () -> new ClienteNaoEncontradoException(clienteId)
                );
    }

    // VERIFICAR NÚMERO DE SÉRIE DUPLICADO

    private void verificarNumeroSerieDuplicado(
            String numeroSerie,
            Long idAtual) {

        boolean duplicado;

        if (idAtual == null) {
            duplicado = repo.existsByNumeroSerie(numeroSerie);
        } else {
            duplicado = repo.existsByNumeroSerieAndIdNot(
                    numeroSerie,
                    idAtual
            );
        }

        if (duplicado) {
            throw new NumeroSerieDuplicadoException();
        }
    }

    // NORMALIZAR DADOS

    private void normalizarDados(Equipamento equipamento) {

        if (equipamento.getMarca() != null) {
            equipamento.setMarca(
                    equipamento.getMarca().trim()
            );
        }

        if (equipamento.getModelo() != null) {
            equipamento.setModelo(
                    equipamento.getModelo().trim()
            );
        }

        if (equipamento.getNumeroSerie() != null) {
            equipamento.setNumeroSerie(
                    equipamento.getNumeroSerie().trim()
            );
        }
    }

    // EXECUTAR VALIDAÇÕES DA ENTIDADE

    private void validarEquipamento(
            Equipamento equipamento) {

        Set<ConstraintViolation<Equipamento>> erros =
                validator.validate(equipamento);

        if (!erros.isEmpty()) {
            throw new ConstraintViolationException(erros);
        }
    }

    // EXCEÇÕES DE NEGÓCIO

    public static class EquipamentoNaoEncontradoException
            extends RuntimeException {

        public EquipamentoNaoEncontradoException(Long id) {
            super("Equipamento não encontrado: " + id);
        }
    }

    public static class ClienteNaoEncontradoException
            extends RuntimeException {

        public ClienteNaoEncontradoException(Long id) {
            super("Cliente não encontrado: " + id);
        }
    }

    public static class NumeroSerieDuplicadoException
            extends RuntimeException {

        public NumeroSerieDuplicadoException() {
            super(
                    "Já existe um equipamento com esse número de série."
            );
        }
    }
}
