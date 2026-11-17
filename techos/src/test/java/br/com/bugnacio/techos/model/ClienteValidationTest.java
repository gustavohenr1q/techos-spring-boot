
package br.com.bugnacio.techos.model;

import jakarta.persistence.Column;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ClienteValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void iniciar() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void finalizar() {
        factory.close();
    }

    private Cliente clienteValido() {
        Cliente cliente = new Cliente();

        cliente.setNome("Ana Silva");
        cliente.setTelefone("(11) 99999-9999");
        cliente.setEmail("ana@email.com");

        return cliente;
    }

    @Test
    void deveAceitarClienteValido() {
        assertTrue(
                validator.validate(clienteValido()).isEmpty()
        );
    }

    @Test
    void deveRejeitarNomeEmBranco() {
        Cliente cliente = clienteValido();
        cliente.setNome("   ");

        assertFalse(validator.validate(cliente).isEmpty());
    }

    @Test
    void deveRejeitarTelefoneVazio() {
        Cliente cliente = clienteValido();
        cliente.setTelefone("");

        assertFalse(validator.validate(cliente).isEmpty());
    }

    @Test
    void deveRejeitarTelefoneMuitoLongo() {
        Cliente cliente = clienteValido();
        cliente.setTelefone("1".repeat(26));

        assertFalse(validator.validate(cliente).isEmpty());
    }

    @Test
    void deveRejeitarEmailInvalido() {
        Cliente cliente = clienteValido();
        cliente.setEmail("email-invalido");

        assertFalse(validator.validate(cliente).isEmpty());
    }

    @Test
    void deveAceitarEmailOpcional() {
        Cliente cliente = clienteValido();
        cliente.setEmail(null);

        assertTrue(validator.validate(cliente).isEmpty());
    }

    @Test
    void deveRejeitarCpfMuitoLongo() {
        Cliente cliente = clienteValido();
        cliente.setCpf("123456789012345");

        assertFalse(validator.validate(cliente).isEmpty());
    }

    @Test
    void deveRejeitarCepMuitoLongo() {
        Cliente cliente = clienteValido();
        cliente.setCep("1234567890");

        assertFalse(validator.validate(cliente).isEmpty());
    }

    @Test
    void deveCriarDataQuandoNaoInformada() {
        Cliente cliente = clienteValido();

        assertNull(cliente.getDataCadastro());

        cliente.prePersist();

        assertNotNull(cliente.getDataCadastro());
    }

    @Test
    void naoDeveSobrescreverDataPreExistente() {
        Cliente cliente = clienteValido();

        LocalDateTime original =
                LocalDateTime.of(2026, 1, 5, 12, 0);

        cliente.setDataCadastro(original);
        cliente.prePersist();

        assertEquals(original, cliente.getDataCadastro());
    }

    @Test
    void dataCadastroNaoDeveSerAtualizadaPeloJpa()
            throws NoSuchFieldException {

        Field campo = Cliente.class.getDeclaredField(
                "dataCadastro"
        );

        Column coluna = campo.getAnnotation(Column.class);

        assertNotNull(coluna);
        assertFalse(coluna.updatable());
    }
}
