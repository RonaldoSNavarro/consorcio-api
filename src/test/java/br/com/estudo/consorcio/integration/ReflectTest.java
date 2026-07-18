package br.com.estudo.consorcio.integration;

import dev.samstevens.totp.code.DefaultCodeVerifier;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;

public class ReflectTest {
    @Test
    public void testMethods() {
        System.out.println("========== DEFAULT CODE VERIFIER METHODS ==========");
        for (Method method : DefaultCodeVerifier.class.getDeclaredMethods()) {
            System.out.println(method.toString());
        }
        System.out.println("==================================================");
    }
}
