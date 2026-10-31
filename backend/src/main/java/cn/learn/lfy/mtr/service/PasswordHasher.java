package cn.learn.lfy.mtr.service;

import cn.learn.lfy.mtr.config.AuthSettings;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
public class PasswordHasher {
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int KEY_LENGTH_BITS = 256;

    private final int defaultIterations;

    public PasswordHasher(AuthSettings authSettings) {
        this.defaultIterations = authSettings.passwordIterations();
    }

    public boolean matches(String rawPassword, String expectedHash, String saltHex, Integer iterations) {
        if (rawPassword == null || expectedHash == null || saltHex == null) {
            return false;
        }
        int rounds = iterations == null ? defaultIterations : iterations;
        if (rounds < 100_000) {
            return false;
        }

        char[] password = rawPassword.toCharArray();
        PBEKeySpec keySpec = null;
        try {
            keySpec = new PBEKeySpec(
                    password,
                    HexFormat.of().parseHex(saltHex),
                    rounds,
                    KEY_LENGTH_BITS
            );
            byte[] calculated = SecretKeyFactory.getInstance(ALGORITHM)
                    .generateSecret(keySpec)
                    .getEncoded();
            return MessageDigest.isEqual(calculated, HexFormat.of().parseHex(expectedHash));
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            return false;
        } finally {
            if (keySpec != null) {
                keySpec.clearPassword();
            }
            java.util.Arrays.fill(password, '\0');
        }
    }
}
