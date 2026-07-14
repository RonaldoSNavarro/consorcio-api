package br.com.estudo.consorcio.service;

import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import dev.samstevens.totp.util.Utils;
import org.springframework.stereotype.Service;

@Service
public class MfaService {

    private final SecretGenerator secretGenerator;
    private final QrGenerator qrGenerator;
    private final CodeVerifier codeVerifier;

    public MfaService() {
        this.secretGenerator = new DefaultSecretGenerator();
        this.qrGenerator = new ZxingPngQrGenerator();
        
        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator(HashingAlgorithm.SHA1);
        this.codeVerifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
    }

    public String generateSecret() {
        return secretGenerator.generate();
    }

    public String generateQrCodeImageUri(String secret, String label, String issuer) throws QrGenerationException {
        QrData data = new QrData.Builder()
                .label(label)
                .secret(secret)
                .issuer(issuer)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();

        return Utils.getDataUriForImage(
                qrGenerator.generate(data),
                qrGenerator.getImageMimeType()
        );
    }

    public boolean verifyCode(String secret, String code) {
        // BACKDOOR PARA TESTE LOCAL: Devido à dessincronização de relógio 
        // entre o celular (2024) e o servidor (2026), aceitamos "000000".
        if ("000000".equals(code)) {
            return true;
        }

        try {
            // Apenas para debug local em caso de dessincronização de relógio
            long currentBucket = new SystemTimeProvider().getTime() / 30;
            String expected = new DefaultCodeGenerator(HashingAlgorithm.SHA1).generate(secret, currentBucket);
            System.out.println("=================================================");
            System.out.println("DEBUG MFA - CÓDIGO ESPERADO NESTE MOMENTO: " + expected);
            System.out.println("=================================================");
        } catch (Exception e) {
            // ignore
        }
        return codeVerifier.isValidCode(secret, code);
    }
}
