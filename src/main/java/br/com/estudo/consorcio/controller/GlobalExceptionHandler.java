package br.com.estudo.consorcio.controller;

import br.com.estudo.consorcio.domain.dto.ExceptionDTO;
import br.com.estudo.consorcio.exception.ClienteInativoException;
import br.com.estudo.consorcio.exception.DocumentoJaCadastradoException;
import br.com.estudo.consorcio.exception.RecursoNaoEncontradoException;
import br.com.estudo.consorcio.exception.RegraDeNegocioException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 400 — Regras de negócio violadas (ex: saldo insuficiente, lance acima do limite)
    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<ExceptionDTO> handleRegraDeNegocio(RegraDeNegocioException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ExceptionDTO("Erro de negócio", ex.getMessage()));
    }

    // 400 — Argumentos ilegais (ex: tentativa de alterar CPF/CNPJ após cadastro)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ExceptionDTO> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ExceptionDTO("Argumento inválido", ex.getMessage()));
    }

    // 404 — Recurso não encontrado (ex: cliente, grupo ou cota inexistente)
    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ExceptionDTO> handleRecursoNaoEncontrado(RecursoNaoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ExceptionDTO("Recurso não encontrado", ex.getMessage()));
    }

    // 409 — Conflito de duplicidade (ex: CPF/CNPJ já cadastrado)
    @ExceptionHandler(DocumentoJaCadastradoException.class)
    public ResponseEntity<ExceptionDTO> handleDocumentoDuplicado(DocumentoJaCadastradoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ExceptionDTO("Conflito de dados", ex.getMessage()));
    }

    // 422 — Entidade não processável (ex: operação em cliente inativo)
    @ExceptionHandler(ClienteInativoException.class)
    public ResponseEntity<ExceptionDTO> handleClienteInativo(ClienteInativoException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ExceptionDTO("Operação não permitida", ex.getMessage()));
    }

    // 400 — Erros de validação de Bean (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ExceptionDTO> handleValidationErrors(MethodArgumentNotValidException ex) {
        var errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ExceptionDTO("Erro de validação", String.join("; ", errors)));
    }

    // 409 — Conflito de concorrência (Lock Otimista)
    @ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ExceptionDTO> handleOptimisticLock(org.springframework.orm.ObjectOptimisticLockingFailureException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ExceptionDTO("Conflito de concorrência", "O registro foi alterado por outro usuário. Por favor, recarregue os dados e tente novamente."));
    }

    // 403 — Acesso Negado
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ExceptionDTO> handleAccessDenied(org.springframework.security.access.AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ExceptionDTO("Acesso negado", "Você não tem permissão para acessar este recurso."));
    }

    // 401 — Falha de Autenticação
    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<ExceptionDTO> handleAuthentication(org.springframework.security.core.AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ExceptionDTO("Falha na autenticação", "Usuário inexistente ou senha inválida."));
    }

    // 500 — Fallback para erros inesperados (não deve ser atingido em fluxos normais)
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ExceptionDTO> handleRuntimeException(RuntimeException ex) {
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ExceptionDTO("Erro interno do servidor",
                        "Ocorreu um erro inesperado. Entre em contato com o suporte."));
    }
}