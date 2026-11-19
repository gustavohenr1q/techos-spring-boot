
package br.com.bugnacio.techos.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record AbrirOrdemServicoRequest(

        @NotNull(message = "Selecione um equipamento.")
        @Positive(message = "Equipamento inválido.")
        Long equipamentoId,

        @NotBlank(
                message = "Informe o defeito relatado."
        )
        @Size(
                max = 4000,
                message = "O defeito deve ter até 4000 caracteres."
        )
        String defeitoRelatado,

        @Size(
                max = 2000,
                message = "Os acessórios devem ter até 2000 caracteres."
        )
        String acessoriosRecebidos,

        @Size(
                max = 2000,
                message = "O estado de conservação deve ter até 2000 caracteres."
        )
        String estadoConservacao,

        @FutureOrPresent(
                message = "A previsão de entrega não pode estar no passado."
        )
        LocalDate dataPrevistaEntrega

) {
}
