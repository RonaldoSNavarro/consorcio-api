package br.com.estudo.consorcio.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.username:}")
    private String username;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void enviarCodigoMfa(String destinatario, String codigo) {
        String assunto = "Seu Código de Segurança MFA - Consórcio Admin";
        String mensagem = "Olá,\n\nSeu código de verificação para acesso ao Consórcio Admin é:\n\n" 
                + codigo + "\n\nEste código expira em 5 minutos.\nSe você não solicitou este código, por favor ignore este e-mail.";

        imprimirBannerNoConsole(destinatario, codigo);

        if (mailHost != null && !mailHost.isBlank()) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                if (username != null && !username.isBlank()) {
                    message.setFrom(username);
                }
                message.setTo(destinatario);
                message.setSubject(assunto);
                message.setText(mensagem);

                mailSender.send(message);
                logger.info("📧 E-mail real com código MFA enviado com sucesso para: {}", destinatario);
            } catch (Exception e) {
                logger.error("❌ Falha ao enviar e-mail real de MFA: {}", e.getMessage());
            }
        }
    }

    public void enviarEmail(String destinatario, String assunto, String mensagem) {
        imprimirBannerGenericoNoConsole(destinatario, assunto, mensagem);

        if (mailHost != null && !mailHost.isBlank()) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                if (username != null && !username.isBlank()) {
                    message.setFrom(username);
                }
                message.setTo(destinatario);
                message.setSubject(assunto);
                message.setText(mensagem);

                mailSender.send(message);
                logger.info("📧 E-mail enviado com sucesso para: {}", destinatario);
            } catch (Exception e) {
                logger.error("❌ Falha ao enviar e-mail: {}", e.getMessage());
            }
        }
    }

    private void imprimirBannerGenericoNoConsole(String destinatario, String assunto, String mensagem) {
        System.out.println("\n");
        System.out.println("========================================================================");
        System.out.println("📧 [SIMULAÇÃO DE E-MAIL] NOTIFICAÇÃO ENVIADA");
        System.out.println("------------------------------------------------------------------------");
        System.out.println("Para:    " + destinatario);
        System.out.println("Assunto: " + assunto);
        System.out.println("------------------------------------------------------------------------");
        System.out.println("Mensagem: " + mensagem);
        System.out.println("========================================================================");
        System.out.println("\n");
    }

    private void imprimirBannerNoConsole(String destinatario, String codigo) {
        System.out.println("\n");
        System.out.println("========================================================================");
        System.out.println("📧 [SIMULAÇÃO DE E-MAIL] CÓDIGO MFA ENVIADO");
        System.out.println("------------------------------------------------------------------------");
        System.out.println("Para:    " + destinatario);
        System.out.println("Assunto: Seu Código de Segurança MFA - Consórcio Admin");
        System.out.println("------------------------------------------------------------------------");
        System.out.println("Código:  [ " + codigo + " ]");
        System.out.println("------------------------------------------------------------------------");
        System.out.println("Expira em: 5 minutos");
        System.out.println("========================================================================");
        System.out.println("\n");
    }
}