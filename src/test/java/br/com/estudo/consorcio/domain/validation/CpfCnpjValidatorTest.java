package br.com.estudo.consorcio.domain.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CpfCnpjValidatorTest {

    @Test
    @DisplayName("Deve validar CPFs válidos com e sem formatação")
    void deveValidarCpfsValidos() {
        // CPF válido: 12345678909 (Mod 11 testado anteriormente)
        assertTrue(CpfCnpjValidator.isValid("12345678909"));
        assertTrue(CpfCnpjValidator.isValid("123.456.789-09"));
        
        // Outro CPF válido real
        assertTrue(CpfCnpjValidator.isValid("05574345056"));
        assertTrue(CpfCnpjValidator.isValid("055.743.450-56"));
    }

    @Test
    @DisplayName("Deve rejeitar CPFs inválidos ou malformados")
    void deveRejeitarCpfsInvalidos() {
        // CPFs com sequências repetidas
        assertFalse(CpfCnpjValidator.isValid("00000000000"));
        assertFalse(CpfCnpjValidator.isValid("111.111.111-11"));
        assertFalse(CpfCnpjValidator.isValid("99999999999"));

        // CPFs com dígitos verificadores errados
        assertFalse(CpfCnpjValidator.isValid("12345678900"));
        assertFalse(CpfCnpjValidator.isValid("123.456.789-10"));

        // Comprimento incorreto
        assertFalse(CpfCnpjValidator.isValid("123456789"));
        assertFalse(CpfCnpjValidator.isValid("123456789012"));
    }

    @Test
    @DisplayName("Deve validar CNPJs válidos com e sem formatação")
    void deveValidarCnpjsValidos() {
        // CNPJ válido: 11222333000181 (Mod 11 testado anteriormente)
        assertTrue(CpfCnpjValidator.isValid("11222333000181"));
        assertTrue(CpfCnpjValidator.isValid("11.222.333/0001-81"));
    }

    @Test
    @DisplayName("Deve rejeitar CNPJs inválidos ou malformados")
    void deveRejeitarCnpjsInvalidos() {
        // CNPJs com sequências repetidas
        assertFalse(CpfCnpjValidator.isValid("00000000000000"));
        assertFalse(CpfCnpjValidator.isValid("11.111.111/1111-11"));

        // CNPJs com dígitos verificadores errados
        assertFalse(CpfCnpjValidator.isValid("11222333000100"));
        assertFalse(CpfCnpjValidator.isValid("11.222.333/0001-99"));

        // Comprimento incorreto
        assertFalse(CpfCnpjValidator.isValid("1122233300018"));
        assertFalse(CpfCnpjValidator.isValid("112223330001819"));
    }

    @Test
    @DisplayName("Deve rejeitar entradas nulas ou vazias")
    void deveRejeitarNulosEVazios() {
        assertFalse(CpfCnpjValidator.isValid(null));
        assertFalse(CpfCnpjValidator.isValid(""));
        assertFalse(CpfCnpjValidator.isValid("   "));
    }
}
