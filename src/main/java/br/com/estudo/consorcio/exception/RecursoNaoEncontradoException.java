package br.com.estudo.consorcio.exception;

public class RecursoNaoEncontradoException extends RuntimeException {
  public RecursoNaoEncontradoException(String mensagem) {
    super(mensagem);
  }
}