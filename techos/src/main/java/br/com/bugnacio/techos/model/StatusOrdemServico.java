
package br.com.bugnacio.techos.model;

public enum StatusOrdemServico {

    ABERTA("Aberta"),

    EM_DIAGNOSTICO("Em diagnóstico"),

    AGUARDANDO_APROVACAO("Aguardando aprovação"),

    AGUARDANDO_PECA("Aguardando peça"),

    EM_REPARO("Em reparo"),

    PRONTA_PARA_ENTREGA("Pronta para entrega"),

    ENTREGUE("Entregue"),

    CANCELADA("Cancelada");

    private final String descricao;

    StatusOrdemServico(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
