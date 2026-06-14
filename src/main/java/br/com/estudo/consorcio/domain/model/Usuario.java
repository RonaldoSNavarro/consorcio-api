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

    @Column(nullable = false, length = 20)
    private String role = "ADMIN";

    @Column(length = 100)
    private String nome;

    @Column(length = 100)
    private String email;

    public Usuario() {
    }

    public Usuario(String login, String senha) {
        this.login = login;
        this.senha = senha;
    }

    public Usuario(String login, String senha, String role, String nome, String email) {
        this.login = login;
        this.senha = senha;
        this.role = role;
        this.nome = nome;
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
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

    // Métodos obrigatórios da interface UserDetails do Spring Security
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // FC-04 FIX: Retorna a role real do banco de dados para controle RBAC granular
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return senha;
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
}