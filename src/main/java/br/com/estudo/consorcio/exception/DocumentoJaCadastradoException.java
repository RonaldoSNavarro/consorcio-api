package br.com.estudo.consorcio.exception;

public class DocumentoJaCadastradoException extends RuntimeException {
    public DocumentoJaCadastradoException(String mensagem) {
        super(mensagem);
    }
}