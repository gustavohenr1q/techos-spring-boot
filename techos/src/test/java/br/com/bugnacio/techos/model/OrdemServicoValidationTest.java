
package br.com.bugnacio.techos.model;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class OrdemServicoValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void iniciarValidator() {

        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void encerrarValidator() {

        factory.close();
    }

    private OrdemServico criarOrdemValida() {

        Cliente cliente = new Cliente();

        cliente.setNome("Cliente Teste");
        cliente.setTelefone("11999999999");

        Equipamento equipamento = new Equipamento();

        equipamento.setTipo(TipoEquipamento.NOTEBOOK);
        equipamento.setMarca("Dell");
        equipamento.setModelo("Inspiron");
        equipamento.setNumeroSerie("TESTE123");
        equipamento.setCliente(cliente);

        OrdemServico ordem = new OrdemServico();

        ordem.setCliente(cliente);
        ordem.setEquipamento(equipamento);
        ordem.setDefeitoRelatado(
                "Equipamento não liga."
        );

        return ordem;
    }

    @Test
    void deveAceitarOrdemValida() {

        OrdemServico ordem = criarOrdemValida();

        assertTrue(
                validator.validate(ordem).isEmpty()
        );
    }

    @Test
    void deveExigirCliente() {

        OrdemServico ordem = criarOrdemValida();

        ordem.setCliente(null);

        assertTrue(
                validator.validate(ordem)
                        .stream()
                        .anyMatch(v ->
                                v.getPropertyPath()
                                 .toString()
                                 .equals("cliente")
                        )
        );
    }

    @Test
    void deveExigirEquipamento() {

        OrdemServico ordem = criarOrdemValida();

        ordem.setEquipamento(null);

        assertTrue(
                validator.validate(ordem)
                        .stream()
                        .anyMatch(v ->
                                v.getPropertyPath()
                                 .toString()
                                 .equals("equipamento")
                        )
        );
    }

    @Test
    void deveExigirDefeitoRelatado() {

        OrdemServico ordem = criarOrdemValida();

        ordem.setDefeitoRelatado("   ");

        assertTrue(
                validator.validate(ordem)
                        .stream()
                        .anyMatch(v ->
                                v.getPropertyPath()
                                 .toString()
                                 .equals("defeitoRelatado")
                        )
        );
    }

    @Test
    void deveExigirStatus() {

        OrdemServico ordem = criarOrdemValida();

        ordem.setStatus(null);

        assertTrue(
                validator.validate(ordem)
                        .stream()
                        .anyMatch(v ->
                                v.getPropertyPath()
                                 .toString()
                                 .equals("status")
                        )
        );
    }

    @Test
    void deveRejeitarDefeitoMuitoLongo() {

        OrdemServico ordem = criarOrdemValida();

        ordem.setDefeitoRelatado(
                "A".repeat(4001)
        );

        assertFalse(
                validator.validate(ordem).isEmpty()
        );
    }

    @Test
    void deveRejeitarValorNegativo() {

        OrdemServico ordem = criarOrdemValida();

        ordem.setValorPecas(
                new BigDecimal("-10.00")
        );

        assertTrue(
                validator.validate(ordem)
                        .stream()
                        .anyMatch(v ->
                                v.getPropertyPath()
                                 .toString()
                                 .equals("valorPecas")
                        )
        );
    }

    @Test
    void deveRejeitarMaisDeDuasCasasDecimais() {

        OrdemServico ordem = criarOrdemValida();

        ordem.setValorMaoDeObra(
                new BigDecimal("10.123")
        );

        assertTrue(
                validator.validate(ordem)
                        .stream()
                        .anyMatch(v ->
                                v.getPropertyPath()
                                 .toString()
                                 .equals("valorMaoDeObra")
                        )
        );
    }

    @Test
    void deveCalcularValorTotal() {

        OrdemServico ordem = criarOrdemValida();

        ordem.setValorMaoDeObra(
                new BigDecimal("100.50")
        );

        ordem.setValorPecas(
                new BigDecimal("20.00")
        );

        assertEquals(
                new BigDecimal("120.50"),
                ordem.getValorTotal()
        );
    }

    @Test
    void deveGerarProtocolo() {

        OrdemServico ordem = criarOrdemValida();

        assertNull(ordem.getProtocolo());

        ordem.setId(42L);

        assertEquals(
                "OS-000042",
                ordem.getProtocolo()
        );
    }

    @Test
    void deveIniciarComStatusAberta() {

        OrdemServico ordem = criarOrdemValida();

        assertEquals(
                StatusOrdemServico.ABERTA,
                ordem.getStatus()
        );
    }
}
