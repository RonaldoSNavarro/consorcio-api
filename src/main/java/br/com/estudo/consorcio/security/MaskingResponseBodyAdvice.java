package br.com.estudo.consorcio.security;

import br.com.estudo.consorcio.domain.dto.ClienteResponseDTO;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.stream.Collectors;

@ControllerAdvice
public class MaskingResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        
        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpServletRequest = servletRequest.getServletRequest();
            Object flag = httpServletRequest.getAttribute("suspicious_session");
            if (flag != null && (Boolean) flag) {
                return applyMask(body);
            }
        }
        return body;
    }

    private Object applyMask(Object body) {
        if (body instanceof Page<?> page) {
            List<Object> maskedContent = page.getContent().stream()
                    .map(this::maskObject)
                    .collect(Collectors.toList());
            return new PageImpl<>(maskedContent, page.getPageable(), page.getTotalElements());
        } else if (body instanceof List<?> list) {
            return list.stream().map(this::maskObject).collect(Collectors.toList());
        }
        return maskObject(body);
    }

    private Object maskObject(Object obj) {
        if (obj instanceof ClienteResponseDTO dto) {
            return new ClienteResponseDTO(
                    dto.id(),
                    mask(dto.nome(), MaskType.NOME),
                    mask(dto.cpfCnpj(), MaskType.CPF),
                    mask(dto.email(), MaskType.EMAIL),
                    mask(dto.telefone(), MaskType.TELEFONE),
                    dto.cep(), dto.logradouro(), dto.numero(), dto.complemento(),
                    dto.bairro(), dto.localidade(), dto.uf(), dto.patrimonio(),
                    dto.rendaMensal(), dto.nivelRisco(), dto.dataCadastro(), dto.statusCliente()
            );
        }
        return obj;
    }

    private String mask(String value, MaskType type) {
        if (value == null || value.isBlank()) return value;
        switch (type) {
            case NOME:
                if (value.length() <= 3) return value + "***";
                return value.substring(0, 3) + "***";
            case CPF:
                String digits = value.replaceAll("\\D", "");
                if (digits.length() == 11) {
                    return "***." + digits.substring(3, 6) + "." + digits.substring(6, 9) + "-**";
                } else if (digits.length() == 14) {
                    return "**." + digits.substring(2, 5) + "." + digits.substring(5, 8) + "/****-**";
                }
                return "***";
            case EMAIL:
                int atIndex = value.indexOf("@");
                if (atIndex > 3) {
                    return value.substring(0, 3) + "***" + value.substring(atIndex);
                }
                return "***" + (atIndex > 0 ? value.substring(atIndex) : "");
            case TELEFONE:
                if (value.length() >= 10) {
                    return value.substring(0, value.length() - 4) + "****";
                }
                return value.substring(0, value.length() / 2) + "****";
            default:
                return "***";
        }
    }
}
