package br.com.estudo.consorcio.domain.util;

import br.com.estudo.consorcio.domain.model.AlgoritmoPedraChave;

/**
 * Calculador de Pedra-Chave baseado na extração da Loteria Federal.
 */
public class PedraChaveCalculator {

    /**
     * Calcula a pedra-chave com base no algoritmo do grupo.
     *
     * @param algoritmo    O algoritmo definido para o grupo.
     * @param numeroPremio O número do prêmio da Loteria Federal (geralmente o 1º prêmio).
     * @param totalCotas   O número total de cotas do grupo (ativas ou máxima, dependendo do algoritmo).
     * @return O número da cota que representa a pedra-chave.
     */
    public static int calcular(AlgoritmoPedraChave algoritmo, int numeroPremio, int totalCotas) {
        if (totalCotas <= 0) {
            return 1;
        }

        switch (algoritmo) {
            case CENTENA:
                return calcularCentena(numeroPremio, totalCotas);
            case DIVISAO_TOTAL:
                return calcularDivisaoTotal(numeroPremio, totalCotas);
            case DIVISAO_1000:
                return calcularDivisao1000(numeroPremio, totalCotas);
            default:
                throw new IllegalArgumentException("Algoritmo de pedra-chave desconhecido: " + algoritmo);
        }
    }

    private static int calcularCentena(int numeroPremio, int totalCotas) {
        int pedra;
        if (totalCotas <= 99) {
            pedra = numeroPremio % 100;
        } else if (totalCotas <= 999) {
            pedra = numeroPremio % 1000;
        } else {
            pedra = numeroPremio % 10000;
        }

        // Se a pedra for 0, geralmente aponta para a última cota
        if (pedra == 0) {
            pedra = totalCotas;
        }
        
        // Garante que não retorne maior que o total de cotas se o grupo não for "cheio" (ex: 600 cotas, prêmio 800)
        // Nesses casos, o fallback vai buscar a próxima disponível, mas podemos limitar.
        // O padrão da skill apenas modula pelo dígito, deixando o fallback resolver.
        return pedra;
    }

    private static int calcularDivisaoTotal(int numeroPremio, int totalCotas) {
        double resultado = (double) numeroPremio / totalCotas;
        double parteDecimal = resultado - Math.floor(resultado);
        double pedraRaw = parteDecimal * totalCotas;
        
        double fracao = pedraRaw - Math.floor(pedraRaw);
        int pedra = fracao >= 0.5 ? (int) Math.ceil(pedraRaw) : (int) Math.floor(pedraRaw);
        
        if (pedra == 0) {
            pedra = 1;
        }
        return pedra;
    }

    private static int calcularDivisao1000(int numeroPremio, int totalCotas) {
        int centena = numeroPremio % 1000;
        double fator = centena / 1000.0;
        double pedraRaw = fator * totalCotas;
        
        double fracao = pedraRaw - Math.floor(pedraRaw);
        int pedra = fracao >= 0.5 ? (int) Math.ceil(pedraRaw) : (int) Math.floor(pedraRaw);
        
        if (pedra == 0) {
            pedra = 1;
        }
        return pedra;
    }
}
