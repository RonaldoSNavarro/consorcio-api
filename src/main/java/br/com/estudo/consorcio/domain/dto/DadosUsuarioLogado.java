package br.com.estudo.consorcio.domain.dto;

public record DadosUsuarioLogado(String login, String role, String nome, String email, boolean mfaEnabled, java.util.List<String> authorities) {}
