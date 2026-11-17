
package br.com.bugnacio.techos.service;

import br.com.bugnacio.techos.model.Cliente;
import br.com.bugnacio.techos.repository.ClienteRepository;

import jakarta.validation.Valid;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Service
@Validated
public class ClienteService {

    private final ClienteRepository repo;

    public ClienteService(ClienteRepository repo) {
        this.repo = repo;
    }

    // LISTAR E PESQUISAR CLIENTES

    @Transactional(readOnly = true)
    public List<Cliente> listar(String busca) {

        if (busca == null || busca.isBlank()) {
            return repo.findAll(Sort.by("nome"));
        }

        return repo.findByNomeContainingIgnoreCase(
                busca.trim()
        );
    }

    // BUSCAR CLIENTE POR ID

    @Transactional(readOnly = true)
    public Cliente buscarPorId(Long id) {

        if (id == null) {
            throw new IllegalArgumentException(
                    "O ID do cliente é obrigatório."
            );
        }

        return repo.findById(id)
                .orElseThrow(
                        () -> new ClienteNaoEncontradoException(id)
                );
    }

    // CADASTRAR CLIENTE

    @Transactional
    public Cliente cadastrar(@Valid Cliente cliente) {

        if (cliente == null) {
            throw new IllegalArgumentException(
                    "Informe os dados do cliente."
            );
        }

        if (cliente.getId() != null) {
            throw new IllegalArgumentException(
                    "Um novo cliente não pode possuir ID."
            );
        }

        normalizarDados(cliente);
        validarCamposBasicos(cliente);

        verificarTelefoneDuplicado(
                cliente.getTelefone(),
                null
        );

        // A data deve ser criada pelo @PrePersist.
        cliente.setDataCadastro(null);

        return repo.save(cliente);
    }

    // ATUALIZAR CLIENTE

    @Transactional
    public Cliente atualizar(
            Long id,
            @Valid Cliente dados) {

        if (id == null || dados == null) {
            throw new IllegalArgumentException(
                    "Dados de atualização inválidos."
            );
        }

        // Evita divergência entre o ID da operação
        // e um ID enviado nos dados do formulário.

        if (dados.getId() != null
                && !id.equals(dados.getId())) {

            throw new IllegalArgumentException(
                    "O ID informado não corresponde ao cliente."
            );
        }

        Cliente existente = buscarPorId(id);

        normalizarDados(dados);
        validarCamposBasicos(dados);

        verificarTelefoneDuplicado(
                dados.getTelefone(),
                id
        );

        copiarCamposEditaveis(dados, existente);

        return repo.save(existente);
    }

    // EXCLUIR CLIENTE

    @Transactional
    public void excluir(Long id) {

        Cliente cliente = buscarPorId(id);

        repo.delete(cliente);

        // Envia a exclusão ao banco ainda dentro
        // da operação, permitindo identificar
        // violações de integridade.

        repo.flush();
    }

    // VERIFICAR TELEFONE DUPLICADO

    private void verificarTelefoneDuplicado(
            String telefone,
            Long idAtual) {

        boolean duplicado;

        if (idAtual == null) {
            duplicado = repo.existsByTelefone(telefone);
        } else {
            duplicado = repo.existsByTelefoneAndIdNot(
                    telefone,
                    idAtual
            );
        }

        if (duplicado) {
            throw new TelefoneDuplicadoException();
        }
    }

    // NORMALIZAÇÃO BÁSICA

    private void normalizarDados(Cliente cliente) {

        if (cliente.getNome() != null) {
            cliente.setNome(cliente.getNome().trim());
        }

        if (cliente.getTelefone() != null) {
            cliente.setTelefone(
                    cliente.getTelefone().trim()
            );
        }

        if (cliente.getEmail() != null) {
            cliente.setEmail(cliente.getEmail().trim());
        }
    }

    // VERIFICAÇÃO DE CAMPOS ESSENCIAIS

    private void validarCamposBasicos(Cliente cliente) {

        if (cliente.getNome() == null
                || cliente.getNome().isBlank()) {

            throw new IllegalArgumentException(
                    "Nome do cliente é obrigatório."
            );
        }

        if (cliente.getTelefone() == null
                || cliente.getTelefone().isBlank()) {

            throw new IllegalArgumentException(
                    "Telefone do cliente é obrigatório."
            );
        }
    }

    // COPIAR SOMENTE CAMPOS EDITÁVEIS

    private void copiarCamposEditaveis(
            Cliente origem,
            Cliente destino) {

        destino.setNome(origem.getNome());
        destino.setTelefone(origem.getTelefone());
        destino.setEmail(origem.getEmail());
        destino.setCpf(origem.getCpf());

        destino.setCep(origem.getCep());
        destino.setLogradouro(origem.getLogradouro());
        destino.setNumero(origem.getNumero());
        destino.setComplemento(origem.getComplemento());
        destino.setBairro(origem.getBairro());
        destino.setCidade(origem.getCidade());
        destino.setEstado(origem.getEstado());
    }

    // EXCEÇÕES DE NEGÓCIO

    public static class ClienteNaoEncontradoException
            extends RuntimeException {

        public ClienteNaoEncontradoException(Long id) {
            super("Cliente não encontrado: " + id);
        }
    }

    public static class TelefoneDuplicadoException
            extends RuntimeException {

        public TelefoneDuplicadoException() {
            super(
                "Já existe um cliente com esse telefone."
            );
        }
    }
}
