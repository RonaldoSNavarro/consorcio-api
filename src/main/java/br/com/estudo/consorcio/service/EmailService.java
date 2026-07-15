package br.com.estudo.consorcio.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.port:587}")
    private String mailPort;

    @Value("${spring.mail.username:}")
    private String username;

    @Value("${spring.mail.password:}")
    private String password;

    public void enviarCodigoMfa(String destinatario, String codigo) {
        String assunto = "Seu Código de Segurança MFA - Consórcio Admin";
        String mensagem = "Olá,\n\nSeu código de verificação para acesso ao Consórcio Admin é:\n\n" 
                + codigo + "\n\nEste código expira em 5 minutos.\nSe você não solicitou este código, por favor ignore este e-mail.";

        // Sempre imprime no console para debug local facilitado
        imprimirBannerNoConsole(destinatario, codigo);

        // Se o SMTP estiver configurado no application.properties, envia o e-mail real
        if (mailHost != null && !mailHost.isBlank() && username != null && !username.isBlank()) {
            try {
                Properties prop = new Properties();
                prop.put("mail.smtp.auth", "true");
                prop.put("mail.smtp.starttls.enable", "true");
                prop.put("mail.smtp.host", mailHost);
                prop.put("mail.smtp.port", mailPort);
                prop.put("mail.smtp.ssl.protocols", "TLSv1.2");
                prop.put("mail.smtp.connectiontimeout", "5000");
                prop.put("mail.smtp.timeout", "5000");

                Session session = Session.getInstance(prop, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });

                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(username));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
                message.setSubject(assunto);
                message.setText(mensagem);

                Transport.send(message);
                logger.info("📧 E-mail real com código MFA enviado com sucesso para: {}", destinatario);
            } catch (Exception e) {
                logger.error("❌ Falha ao enviar e-mail real de MFA (serviço offline ou credenciais incorretas). Detalhes: {}", e.getMessage());
                logger.info("👉 Use o código exibido no console acima para prosseguir.");
            }
        } else {
            logger.info("ℹ️ SMTP não configurado completamente (spring.mail.host ou username vazio). O código MFA foi enviado apenas para o console da aplicação.");
        }
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
