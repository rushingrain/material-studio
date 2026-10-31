package cn.learn.lfy.mtr.service;

import cn.learn.lfy.mtr.config.AuthSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {
    private static final String HASH = "370d8fe7acecba4f4bd47cac0862194364d2cf8470f9f2b74ca2a0de91975901";
    private static final String SALT = "2c5c8b95d775e5732593b1af8ec9dd93";

    private final PasswordHasher hasher = new PasswordHasher(
            new AuthSettings("test-session", false, 3600, 210000)
    );

    @Test
    void matchesSeedPassword() {
        assertTrue(hasher.matches("demo1234", HASH, SALT, 210000));
    }

    @Test
    void rejectsWrongOrMalformedCredentials() {
        assertFalse(hasher.matches("wrong-password", HASH, SALT, 210000));
        assertFalse(hasher.matches("demo1234", "not-hex", SALT, 210000));
        assertFalse(hasher.matches(null, HASH, SALT, 210000));
    }
}
