package br.com.estudo.consorcio.config.observability;

public final class SecurityLogSanitizer {

    private SecurityLogSanitizer() {}

    public static String maskCpf(String cpf) {
        if (cpf == null || cpf.length() < 11) {
            return "***";
        }
        return cpf.substring(0, 3) + ".***.***-" + cpf.substring(cpf.length() - 2);
    }

    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        String[] parts = email.split("@");
        String prefix = parts[0].length() > 2 ? parts[0].substring(0, 2) + "***" : "***";
        return prefix + "@" + parts[1];
    }
}