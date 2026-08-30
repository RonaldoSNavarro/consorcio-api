package br.com.estudo.consorcio.security;

import br.com.estudo.consorcio.domain.dto.ClienteResponseDTO;
import br.com.estudo.consorcio.domain.model.NivelRisco;
import br.com.estudo.consorcio.domain.model.StatusCliente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MaskingResponseBodyAdviceTest {

    private MaskingResponseBodyAdvice advice;
    private ClienteResponseDTO dto;

    @BeforeEach
    void setUp() {
        advice = new MaskingResponseBodyAdvice();
        dto = new ClienteResponseDTO(
                1L, "Rodrigo Silva", "12345678901", "rodrigo.silva@exemplo.com", "11988887777",
                "01001000", "Praça da Sé", "100", "Apto 1", "Sé", "São Paulo", "SP",
                new BigDecimal("500000.00"), new BigDecimal("15000.00"), NivelRisco.BAIXO, false,
                LocalDate.now(), StatusCliente.ATIVO
        );
    }

    @Test
    @DisplayName("Não deve mascarar dados quando sessão não for suspeita")
    void naoDeveMascararSessaoNormal() {
        MockHttpServletRequest mockReq = new MockHttpServletRequest();
        ServletServerHttpRequest request = new ServletServerHttpRequest(mockReq);
        ServletServerHttpResponse response = new ServletServerHttpResponse(new MockHttpServletResponse());

        Object result = advice.beforeBodyWrite(dto, null, null, null, request, response);

        assertSame(dto, result);
    }

    @Test
    @DisplayName("Deve mascarar CPF, Nome, Email e Telefone quando sessão for marcada como suspeita")
    void deveMascararDadosEmSessaoSuspeita() {
        MockHttpServletRequest mockReq = new MockHttpServletRequest();
        mockReq.setAttribute("suspicious_session", true);
        ServletServerHttpRequest request = new ServletServerHttpRequest(mockReq);
        ServletServerHttpResponse response = new ServletServerHttpResponse(new MockHttpServletResponse());

        Object result = advice.beforeBodyWrite(dto, null, null, null, request, response);

        assertTrue(result instanceof ClienteResponseDTO);
        ClienteResponseDTO masked = (ClienteResponseDTO) result;

        assertEquals("Rod***", masked.nome());
        assertEquals("***.456.789-**", masked.cpfCnpj());
        assertTrue(masked.email().startsWith("rod***@"));
        assertTrue(masked.telefone().endsWith("****"));
    }

    @Test
    @DisplayName("Deve mascarar itens dentro de uma Page paginada")
    void deveMascararItensEmPagePaginada() {
        MockHttpServletRequest mockReq = new MockHttpServletRequest();
        mockReq.setAttribute("suspicious_session", true);
        ServletServerHttpRequest request = new ServletServerHttpRequest(mockReq);
        ServletServerHttpResponse response = new ServletServerHttpResponse(new MockHttpServletResponse());

        Page<ClienteResponseDTO> page = new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1);

        Object result = advice.beforeBodyWrite(page, null, null, null, request, response);

        assertTrue(result instanceof Page<?>);
        Page<?> maskedPage = (Page<?>) result;
        assertEquals(1, maskedPage.getContent().size());
        ClienteResponseDTO maskedDto = (ClienteResponseDTO) maskedPage.getContent().get(0);

        assertEquals("Rod***", maskedDto.nome());
        assertEquals("***.456.789-**", maskedDto.cpfCnpj());
    }
}