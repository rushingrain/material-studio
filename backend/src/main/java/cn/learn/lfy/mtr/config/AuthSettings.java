package cn.learn.lfy.mtr.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AuthSettings {
    private final String cookieName;
    private final boolean cookieSecure;
    private final long sessionTtlSeconds;
    private final int passwordIterations;

    public AuthSettings(
            @Value("${app.auth.cookie-name}") String cookieName,
            @Value("${app.auth.cookie-secure}") boolean cookieSecure,
            @Value("${app.auth.session-ttl-seconds}") long sessionTtlSeconds,
            @Value("${app.auth.password-iterations}") int passwordIterations
    ) {
        if (cookieName == null || cookieName.isBlank()) {
            throw new IllegalArgumentException("app.auth.cookie-name 不能为空");
        }
        if (sessionTtlSeconds <= 0) {
            throw new IllegalArgumentException("app.auth.session-ttl-seconds 必须大于 0");
        }
        if (passwordIterations < 100_000) {
            throw new IllegalArgumentException("app.auth.password-iterations 不能小于 100000");
        }
        this.cookieName = cookieName.trim();
        this.cookieSecure = cookieSecure;
        this.sessionTtlSeconds = sessionTtlSeconds;
        this.passwordIterations = passwordIterations;
    }

    public String cookieName() {
        return cookieName;
    }

    public boolean cookieSecure() {
        return cookieSecure;
    }

    public long sessionTtlSeconds() {
        return sessionTtlSeconds;
    }

    public int passwordIterations() {
        return passwordIterations;
    }
}
