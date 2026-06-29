package br.com.estudo.consorcio.security;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class IntrusionDetectionService {

    private final Map<String, RequestData> requestCounts = new ConcurrentHashMap<>();
    private final Map<String, Long> suspiciousIps = new ConcurrentHashMap<>();

    private static final int MAX_REQUESTS_PER_MINUTE = 30;
    private static final long SUSPICIOUS_BLOCK_TIME_MILLIS = 15 * 60 * 1000; // 15 minutos em modo defensivo

    public boolean isSuspicious(String ipOrToken) {
        if (ipOrToken == null || ipOrToken.isBlank()) {
            return false;
        }

        long currentTime = System.currentTimeMillis();

        // Limpa IPs/Tokens que já saíram do tempo de suspeita
        suspiciousIps.entrySet().removeIf(entry -> currentTime > entry.getValue());

        if (suspiciousIps.containsKey(ipOrToken)) {
            return true;
        }

        RequestData data = requestCounts.computeIfAbsent(ipOrToken, k -> new RequestData(currentTime));

        if (currentTime - data.timestamp > 60000) {
            // Passou 1 minuto, reseta contador
            data.timestamp = currentTime;
            data.count.set(1);
            return false;
        }

        int count = data.count.incrementAndGet();
        if (count > MAX_REQUESTS_PER_MINUTE) {
            suspiciousIps.put(ipOrToken, currentTime + SUSPICIOUS_BLOCK_TIME_MILLIS);
            return true;
        }

        return false;
    }

    private static class RequestData {
        long timestamp;
        AtomicInteger count;

        RequestData(long timestamp) {
            this.timestamp = timestamp;
            this.count = new AtomicInteger(0);
        }
    }
}
