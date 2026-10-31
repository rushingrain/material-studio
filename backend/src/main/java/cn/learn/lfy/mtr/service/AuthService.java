package cn.learn.lfy.mtr.service;

import cn.learn.lfy.mtr.api.ApiErrorCode;
import cn.learn.lfy.mtr.api.BusinessException;
import cn.learn.lfy.mtr.api.LoginRequest;
import cn.learn.lfy.mtr.config.AuthSettings;
import cn.learn.lfy.mtr.domain.model.AppUser;
import cn.learn.lfy.mtr.domain.model.UserCredential;
import cn.learn.lfy.mtr.mapper.AuthMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

@Service
public class AuthService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AuthMapper authMapper;
    private final PasswordHasher passwordHasher;
    private final long sessionTtlSeconds;

    public AuthService(
            AuthMapper authMapper,
            PasswordHasher passwordHasher,
            AuthSettings authSettings
    ) {
        this.authMapper = authMapper;
        this.passwordHasher = passwordHasher;
        this.sessionTtlSeconds = authSettings.sessionTtlSeconds();
    }

    @Transactional
    public LoginResult login(LoginRequest request) {
        String username = request.username().trim().toLowerCase(Locale.ROOT);
        UserCredential credential = authMapper.findCredentialByUsername(username)
                .orElseThrow(() -> invalidCredentials());
        if (!passwordHasher.matches(
                request.password(),
                credential.passwordHash(),
                credential.passwordSalt(),
                credential.passwordIterations()
        )) {
            throw invalidCredentials();
        }

        AppUser user = authMapper.findUserById(credential.userId())
                .orElseThrow(AuthService::invalidCredentials);

        LocalDateTime now = LocalDateTime.now();
        authMapper.deleteExpiredSessions(user.id(), now);
        String token = newToken();
        authMapper.insertSession(tokenHash(token), user.id(), now.plusSeconds(sessionTtlSeconds));
        return new LoginResult(user, token);
    }

    @Transactional(readOnly = true)
    public Optional<AppUser> authenticate(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return authMapper.findUserBySessionTokenHash(tokenHash(token), LocalDateTime.now());
    }

    @Transactional
    public void logout(String token) {
        if (token != null && !token.isBlank()) {
            authMapper.deleteSession(tokenHash(token));
        }
    }

    private static BusinessException invalidCredentials() {
        return new BusinessException(ApiErrorCode.INVALID_CREDENTIALS, "用户名或密码不正确");
    }

    private static String newToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String tokenHash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }

    public record LoginResult(AppUser user, String token) {
    }
}
