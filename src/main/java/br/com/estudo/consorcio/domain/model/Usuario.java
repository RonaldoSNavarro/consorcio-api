package br.com.estudo.consorcio.domain.model;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "usuarios")
public class Usuario implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String login;

    @Column(nullable = false)
    private String senha;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "perfil_id")
    private Perfil perfil;

    @Column(length = 100)
    private String nome;

    @Column(length = 100)
    private String email;

    @Column(name = "mfa_enabled", nullable = false)
    private boolean mfaEnabled = false;

    @Column(name = "mfa_secret", length = 32)
    private String mfaSecret;

    @Column(name = "mfa_code", length = 6)
    private String mfaCode;

    @Column(name = "mfa_code_expires_at")
    private java.time.LocalDateTime mfaCodeExpiresAt;

    public Usuario() {
    }

    public Usuario(String login, String senha) {
        this.login = login;
        this.senha = senha;
    }

    public Usuario(String login, String senha, Perfil perfil, String nome, String email) {
        this.login = login;
        this.senha = senha;
        this.perfil = perfil;
        this.nome = nome;
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public void setPerfil(Perfil perfil) {
        this.perfil = perfil;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isMfaEnabled() {
        return mfaEnabled;
    }

    public void setMfaEnabled(boolean mfaEnabled) {
        this.mfaEnabled = mfaEnabled;
    }

    public String getMfaSecret() {
        return mfaSecret;
    }

    public void setMfaSecret(String mfaSecret) {
        this.mfaSecret = mfaSecret;
    }

    // Métodos obrigatórios da interface UserDetails do Spring Security
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        java.util.List<GrantedAuthority> authorities = new java.util.ArrayList<>();
        // Adiciona ROLE_<perfil> para compatibilidade com hasRole()/hasAnyRole()
        if (perfil != null && perfil.getNome() != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + perfil.getNome()));
        }
        // Adiciona permissões granulares
        if (perfil != null && perfil.getPermissoes() != null) {
            perfil.getPermissoes().forEach(
                permissao -> authorities.add(new SimpleGrantedAuthority(permissao.name()))
            );
        }
        return authorities;
    }

    @Override
    public String getPassword() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    @Override
    public String getUsername() {
        return login;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // Conta não expira
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // Conta não bloqueia
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Senha não expira
    }

    @Override
    public boolean isEnabled() {
        return true; // Usuário ativo
    }

    public String getMfaCode() {
        return mfaCode;
    }

    public void setMfaCode(String mfaCode) {
        this.mfaCode = mfaCode;
    }

    public java.time.LocalDateTime getMfaCodeExpiresAt() {
        return mfaCodeExpiresAt;
    }

    public void setMfaCodeExpiresAt(java.time.LocalDateTime mfaCodeExpiresAt) {
        this.mfaCodeExpiresAt = mfaCodeExpiresAt;
    }
}