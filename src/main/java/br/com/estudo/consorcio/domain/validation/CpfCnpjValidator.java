package br.com.estudo.consorcio.domain.validation;

public class CpfCnpjValidator {

    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        // Remove non-digits
        String clean = value.replaceAll("\\D", "");

        if (clean.length() == 11) {
            return isValidCpf(clean);
        } else if (clean.length() == 14) {
            return isValidCnpj(clean);
        }
        return false;
    }

    private static boolean isValidCpf(String cpf) {
        if (cpf.matches("(\\d)\\1{10}")) {
            return false;
        }

        int sum = 0;
        for (int i = 0; i < 9; i++) {
            sum += Character.getNumericValue(cpf.charAt(i)) * (10 - i);
        }
        int r = sum % 11;
        int d1 = (r < 2) ? 0 : 11 - r;
        if (Character.getNumericValue(cpf.charAt(9)) != d1) {
            return false;
        }

        sum = 0;
        for (int i = 0; i < 10; i++) {
            sum += Character.getNumericValue(cpf.charAt(i)) * (11 - i);
        }
        r = sum % 11;
        int d2 = (r < 2) ? 0 : 11 - r;
        return Character.getNumericValue(cpf.charAt(10)) == d2;
    }

    private static boolean isValidCnpj(String cnpj) {
        if (cnpj.matches("(\\d)\\1{13}")) {
            return false;
        }

        int[] weights1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            sum += Character.getNumericValue(cnpj.charAt(i)) * weights1[i];
        }
        int r = sum % 11;
        int d1 = (r < 2) ? 0 : 11 - r;
        if (Character.getNumericValue(cnpj.charAt(12)) != d1) {
            return false;
        }

        int[] weights2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        sum = 0;
        for (int i = 0; i < 13; i++) {
            sum += Character.getNumericValue(cnpj.charAt(i)) * weights2[i];
        }
        r = sum % 11;
        int d2 = (r < 2) ? 0 : 11 - r;
        return Character.getNumericValue(cnpj.charAt(13)) == d2;
    }
}
