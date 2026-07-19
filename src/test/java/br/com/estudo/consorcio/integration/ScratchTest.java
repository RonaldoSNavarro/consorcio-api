package br.com.estudo.consorcio.integration;

import br.com.estudo.consorcio.domain.model.Usuario;
import br.com.estudo.consorcio.domain.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
public class ScratchTest {

    @Autowired
    private UsuarioRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    public void testPassword() {
        System.out.println("========== SCRATCH TEST ==========");
        var users = repository.findAll();
        for (Usuario user : users) {
            boolean matchesAdmin = passwordEncoder.matches("admin", user.getPassword());
            boolean matchesAdmin123 = passwordEncoder.matches("admin123", user.getPassword());
            System.out.println("User: " + user.getUsername());
            System.out.println("  Role: " + (user.getPerfil() != null ? user.getPerfil().getNome() : "null"));
            System.out.println("  Hash: " + user.getPassword());
            System.out.println("  Matches 'admin': " + matchesAdmin);
            System.out.println("  Matches 'admin123': " + matchesAdmin123);
        }
        System.out.println("==================================");
    }
}
