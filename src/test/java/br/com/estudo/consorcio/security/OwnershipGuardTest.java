package br.com.estudo.consorcio.security;

import br.com.estudo.consorcio.domain.model.Cliente;
import br.com.estudo.consorcio.domain.model.Cota;
import br.com.estudo.consorcio.domain.model.Parcela;
import br.com.estudo.consorcio.domain.repository.ClienteRepository;
import br.com.estudo.consorcio.domain.repository.CotaRepository;
import br.com.estudo.consorcio.domain.repository.ParcelaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OwnershipGuardTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private CotaRepository cotaRepository;

    @Mock
    private ParcelaRepository parcelaRepository;

    @InjectMocks
    private OwnershipGuard ownershipGuard;

    private Cliente cliente;
    private Cota cota;
    private Parcela parcela;

    @BeforeEach
    void setUp() {
        cliente = new Cliente();
        cliente.setId(10L);
        cliente.setEmail("consorciado@banco.com");

        cota = new Cota();
        cota.setId(20L);
        cota.setCliente(cliente);

        parcela = new Parcela();
        parcela.setId(30L);
        parcela.setCota(cota);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateUser(String email) {
        UserDetails userDetails = new User(email, "password", Collections.emptyList());
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("Deve permitir acesso ao cliente se o e-mail autenticado for o dono do registro")
    void devePermitirAcessoAoClienteProprio() {
        authenticateUser("consorciado@banco.com");
        when(clienteRepository.findById(10L)).thenReturn(Optional.of(cliente));

        assertTrue(ownershipGuard.canAccessCliente(10L));
    }

    @Test
    @DisplayName("Deve negar acesso ao cliente se o e-mail autenticado for diferente do dono")
    void deveNegarAcessoAoClienteDeTerceiro() {
        authenticateUser("invasor@hacker.com");
        when(clienteRepository.findById(10L)).thenReturn(Optional.of(cliente));

        assertFalse(ownershipGuard.canAccessCliente(10L));
    }

    @Test
    @DisplayName("Deve permitir acesso à cota se o e-mail autenticado for o dono da cota")
    void devePermitirAcessoACotaPropria() {
        authenticateUser("consorciado@banco.com");
        when(cotaRepository.findById(20L)).thenReturn(Optional.of(cota));

        assertTrue(ownershipGuard.canAccessCota(20L));
    }

    @Test
    @DisplayName("Deve negar acesso à cota de terceiro")
    void deveNegarAcessoACotaDeTerceiro() {
        authenticateUser("invasor@hacker.com");
        when(cotaRepository.findById(20L)).thenReturn(Optional.of(cota));

        assertFalse(ownershipGuard.canAccessCota(20L));
    }

    @Test
    @DisplayName("Deve permitir acesso à parcela vinculada à cota do próprio usuário")
    void devePermitirAcessoAParcelaPropria() {
        authenticateUser("consorciado@banco.com");
        when(parcelaRepository.findById(30L)).thenReturn(Optional.of(parcela));

        assertTrue(ownershipGuard.canAccessParcela(30L));
    }

    @Test
    @DisplayName("Deve negar acesso quando não houver usuário autenticado")
    void deveNegarAcessoSemAutenticacao() {
        SecurityContextHolder.clearContext();
        assertFalse(ownershipGuard.canAccessCliente(10L));
        assertFalse(ownershipGuard.canAccessCota(20L));
        assertFalse(ownershipGuard.canAccessParcela(30L));
    }
}