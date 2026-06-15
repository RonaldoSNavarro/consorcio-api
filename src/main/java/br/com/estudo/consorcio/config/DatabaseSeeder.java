package br.com.estudo.consorcio.config;

import br.com.estudo.consorcio.domain.model.Usuario;
import br.com.estudo.consorcio.domain.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseSeeder(UsuarioRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (repository.count() == 0) {
            Usuario admin = new Usuario("admin", passwordEncoder.encode("admin"), "ADMIN", "Administrador", "admin@consorcio.com.br");
            repository.save(admin);

            Usuario gestor = new Usuario("gestor", passwordEncoder.encode("admin"), "ADMIN", "Gestor", "gestor@consorcio.com.br"); // Gestor com senha admin
            repository.save(gestor);

            Usuario consorciado = new Usuario("consorciado", passwordEncoder.encode("admin"), "ADMIN", "Consorciado", "consorciado@consorcio.com.br"); // Consorciado com senha admin
            repository.save(consorciado);

            System.out.println("🌱 Banco de dados semeado com usuários padrão!");
        } else {
            // Garante que se o usuário 'admin' existir, ele tenha a senha 'admin' para testes
            var adminOpt = repository.findAll().stream().filter(u -> "admin".equals(u.getUsername())).findFirst();
            if (adminOpt.isPresent()) {
                var admin = adminOpt.get();
                admin.setRole("ADMIN");
                admin.setSenha(passwordEncoder.encode("admin"));
                admin.setNome("Administrador");
                admin.setEmail("admin@consorcio.com.br");
                repository.save(admin);
                System.out.println("🌱 Senha do usuário 'admin' restaurada para 'admin'!");
            }
        }
    }
}
