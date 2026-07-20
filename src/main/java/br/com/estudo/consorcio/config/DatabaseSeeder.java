package br.com.estudo.consorcio.config;

import br.com.estudo.consorcio.domain.model.Usuario;
import br.com.estudo.consorcio.domain.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import br.com.estudo.consorcio.domain.model.Perfil;
import br.com.estudo.consorcio.domain.model.Permissao;
import br.com.estudo.consorcio.domain.repository.PerfilRepository;
import java.util.Set;
import java.util.EnumSet;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private final UsuarioRepository repository;
    private final PerfilRepository perfilRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseSeeder(UsuarioRepository repository, PerfilRepository perfilRepository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.perfilRepository = perfilRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        Perfil adminPerfil = perfilRepository.findByNome("ADMIN").orElseGet(() -> new Perfil("ADMIN"));
        adminPerfil.setPermissoes(EnumSet.allOf(Permissao.class));
        adminPerfil = perfilRepository.save(adminPerfil);

        Perfil gestorPerfil = perfilRepository.findByNome("GESTOR").orElseGet(() -> {
            Perfil p = new Perfil("GESTOR");
            p.setPermissoes(Set.of(Permissao.VIEW_DASHBOARD, Permissao.VIEW_COTAS, Permissao.VIEW_GRUPOS));
            return perfilRepository.save(p);
        });

        Perfil compliancePerfil = perfilRepository.findByNome("COMPLIANCE").orElseGet(() -> {
            Perfil p = new Perfil("COMPLIANCE");
            p.setPermissoes(Set.of(Permissao.VIEW_DASHBOARD, Permissao.VIEW_COTAS, Permissao.VIEW_GRUPOS, Permissao.VIEW_FINANCEIRO));
            return perfilRepository.save(p);
        });

        Perfil consorciadoPerfil = perfilRepository.findByNome("CONSORCIADO").orElseGet(() -> {
            Perfil p = new Perfil("CONSORCIADO");
            p.setPermissoes(Set.of(Permissao.VIEW_MEUS_DADOS));
            return perfilRepository.save(p);
        });

        if (repository.count() == 0) {
            Usuario admin = new Usuario("admin", passwordEncoder.encode("admin"), adminPerfil, "Administrador", "ronaldoguitarrista@gmail.com");
            repository.save(admin);

            System.out.println("🌱 Banco de dados semeado com usuário admin padrão!");
        } else {
            // Garante que se o usuário 'admin' existir, ele tenha a senha 'admin' para testes
            var adminOpt = repository.findAll().stream().filter(u -> "admin".equals(u.getUsername())).findFirst();
            if (adminOpt.isPresent()) {
                var admin = adminOpt.get();
                admin.setPerfil(adminPerfil);
                admin.setSenha(passwordEncoder.encode("admin"));
                admin.setNome("Administrador");
                admin.setEmail("ronaldoguitarrista@gmail.com");
                repository.save(admin);
                System.out.println("🌱 Senha do usuário 'admin' restaurada para 'admin'!");
            }
        }
    }
}
