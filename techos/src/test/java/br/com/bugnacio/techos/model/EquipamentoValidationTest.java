
package br.com.bugnacio.techos.model;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EquipamentoValidationTest {

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

    // CRIA EQUIPAMENTO VÁLIDO PARA OS TESTES

    private Equipamento equipamentoValido() {

        Cliente cliente = new Cliente();
        cliente.setId(1L);

        Equipamento equipamento = new Equipamento();

        equipamento.setTipo(TipoEquipamento.COMPUTADOR);
        equipamento.setMarca("Dell");
        equipamento.setModelo("Inspiron 15");
        equipamento.setNumeroSerie("ABC123456");
        equipamento.setCliente(cliente);

        return equipamento;
    }

    // VERIFICA ERRO EM UM CAMPO ESPECÍFICO

    private void verificarErro(
            Equipamento equipamento,
            String campo) {

        boolean possuiErro = validator
                .validate(equipamento)
                .stream()
                .anyMatch(violacao ->
                        violacao.getPropertyPath()
                                .toString()
                                .equals(campo)
                );

        assertTrue(
                possuiErro,
                "Era esperado um erro no campo: " + campo
        );
    }

    @Test
    void deveAceitarEquipamentoValido() {

        Equipamento equipamento = equipamentoValido();

        assertTrue(
                validator.validate(equipamento).isEmpty()
        );
    }

    @Test
    void naoDeveAceitarTipoNulo() {

        Equipamento equipamento = equipamentoValido();
        equipamento.setTipo(null);

        verificarErro(equipamento, "tipo");
    }

    @Test
    void naoDeveAceitarMarcaVazia() {

        Equipamento equipamento = equipamentoValido();
        equipamento.setMarca("   ");

        verificarErro(equipamento, "marca");
    }

    @Test
    void naoDeveAceitarModeloVazio() {

        Equipamento equipamento = equipamentoValido();
        equipamento.setModelo("");

        verificarErro(equipamento, "modelo");
    }

    @Test
    void naoDeveAceitarNumeroSerieVazio() {

        Equipamento equipamento = equipamentoValido();
        equipamento.setNumeroSerie("");

        verificarErro(equipamento, "numeroSerie");
    }

    @Test
    void naoDeveAceitarClienteNulo() {

        Equipamento equipamento = equipamentoValido();
        equipamento.setCliente(null);

        verificarErro(equipamento, "cliente");
    }

    @Test
    void naoDeveAceitarMarcaMuitoLonga() {

        Equipamento equipamento = equipamentoValido();
        equipamento.setMarca("A".repeat(256));

        verificarErro(equipamento, "marca");
    }

    @Test
    void naoDeveAceitarModeloMuitoLongo() {

        Equipamento equipamento = equipamentoValido();
        equipamento.setModelo("A".repeat(256));

        verificarErro(equipamento, "modelo");
    }

    @Test
    void naoDeveAceitarNumeroSerieMuitoLongo() {

        Equipamento equipamento = equipamentoValido();
        equipamento.setNumeroSerie("A".repeat(256));

        verificarErro(equipamento, "numeroSerie");
    }
}
