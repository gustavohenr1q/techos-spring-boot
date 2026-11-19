package br.com.bugnacio.techos.excpetion;

public class RegraDeNegocioException extends RuntimeException {

    public RegraDeNegocioException (String mensagem){
        super(mensagem);
    }

}
