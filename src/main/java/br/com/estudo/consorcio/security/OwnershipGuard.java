package br.com.estudo.consorcio.security;

import br.com.estudo.consorcio.domain.model.Cliente;
import br.com.estudo.consorcio.domain.model.Cota;
import br.com.estudo.consorcio.domain.repository.ClienteRepository;
import br.com.estudo.consorcio.domain.repository.CotaRepository;
import br.com.estudo.consorcio.domain.repository.ParcelaRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Componente responsável por garantir as regras de propriedade de recursos (IDOR).
 * O consorciado só pode acessar dados (clientes, cotas, parcelas) vinculados à sua própria conta.
 */
@Component("ownershipGuard")
public class OwnershipGuard {

    private final ClienteRepository clienteRepository;
    private final CotaRepository cotaRepository;
    private final ParcelaRepository parcelaRepository;

    public OwnershipGuard(ClienteRepository clienteRepository, CotaRepository cotaRepository, ParcelaRepository parcelaRepository) {
        this.clienteRepository = clienteRepository;
        this.cotaRepository = cotaRepository;
        this.parcelaRepository = parcelaRepository;
    }

    /**
     * Extrai o e-mail do usuário autenticado no JWT local a partir do SecurityContext.
     */
    private String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        
        if (auth.getPrincipal() instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        
        return auth.getName();
    }

    /**
     * Verifica se o cliente solicitado pertence ao usuário logado.
     */
    public boolean canAccessCliente(Long clienteId) {
        String email = getCurrentUserEmail();
        if (email == null) return false;
        
        return clienteRepository.findById(clienteId)
                .map(Cliente::getEmail)
                .filter(cEmail -> cEmail.equalsIgnoreCase(email))
                .isPresent();
    }

    /**
     * Verifica se a cota solicitada pertence ao usuário logado (através do cliente dono da cota).
     */
    public boolean canAccessCota(Long cotaId) {
        String email = getCurrentUserEmail();
        if (email == null) return false;

        return cotaRepository.findById(cotaId)
                .map(Cota::getCliente)
                .map(Cliente::getEmail)
                .filter(cEmail -> cEmail.equalsIgnoreCase(email))
                .isPresent();
    }

    /**
     * Verifica se a parcela solicitada pertence a uma cota do usuário logado.
     */
    public boolean canAccessParcela(Long parcelaId) {
        String email = getCurrentUserEmail();
        if (email == null) return false;

        return parcelaRepository.findById(parcelaId)
                .map(p -> p.getCota().getCliente().getEmail())
                .filter(cEmail -> cEmail.equalsIgnoreCase(email))
                .isPresent();
    }
}
