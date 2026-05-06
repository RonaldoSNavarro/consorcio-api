package br.com.estudo.consorcio.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Toda vez que o sistema lançar uma RuntimeException, este método assume o controle
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> tratarRegrasDeNegocio(RuntimeException ex) {
        Map<String, String> resposta = new HashMap<>();
        // Pega a mensagem personalizada e coloca no JSON
        resposta.put("erro", ex.getMessage());

        // Retorna o status 400 Bad Request ao invés de 500
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resposta);
    }
}