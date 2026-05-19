package br.com.estudo.consorcio.exception;

public class ClienteInativoException extends RuntimeException {
  public ClienteInativoException(String mensagem) {
    super(mensagem);
  }
}