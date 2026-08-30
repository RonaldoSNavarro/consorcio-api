package br.com.estudo.consorcio.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @Test
    @DisplayName("Deve enviar e-mail com sucesso quando host SMTP estiver configurado")
    void deveEnviarEmailComSucesso() {
        ReflectionTestUtils.setField(emailService, "mailHost", "smtp.servidor.com");
        ReflectionTestUtils.setField(emailService, "username", "notificacao@consorcio.com");

        emailService.enviarCodigoMfa("cliente@teste.com", "123456");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Não deve chamar mailSender quando host SMTP estiver vazio")
    void naoDeveChamarMailSenderSemHost() {
        ReflectionTestUtils.setField(emailService, "mailHost", "");

        emailService.enviarCodigoMfa("cliente@teste.com", "123456");

        verifyNoInteractions(mailSender);
    }

    @Test
    @DisplayName("Deve capturar exceção de SMTP sem propagar erro fatal")
    void deveCapturarExcecaoSmtp() {
        ReflectionTestUtils.setField(emailService, "mailHost", "smtp.servidor.com");
        doThrow(new RuntimeException("SMTP Connection Refused")).when(mailSender).send(any(SimpleMailMessage.class));

        emailService.enviarCodigoMfa("cliente@teste.com", "123456");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }
}