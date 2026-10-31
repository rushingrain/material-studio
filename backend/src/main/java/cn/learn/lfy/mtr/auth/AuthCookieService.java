package cn.learn.lfy.mtr.auth;

import cn.learn.lfy.mtr.config.AuthSettings;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class AuthCookieService {
    private final AuthSettings authSettings;

    public AuthCookieService(AuthSettings authSettings) {
        this.authSettings = authSettings;
    }

    public String readToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (authSettings.cookieName().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public ResponseCookie create(String token) {
        return baseCookie(token)
                .maxAge(Duration.ofSeconds(authSettings.sessionTtlSeconds()))
                .build();
    }

    public ResponseCookie clear() {
        return baseCookie("")
                .maxAge(Duration.ZERO)
                .build();
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie.from(authSettings.cookieName(), value)
                .httpOnly(true)
                .secure(authSettings.cookieSecure())
                .sameSite("Lax")
                .path("/");
    }
}
