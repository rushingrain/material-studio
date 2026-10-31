package cn.learn.lfy.mtr.service;

import cn.learn.lfy.mtr.api.ApiErrorCode;
import cn.learn.lfy.mtr.api.BusinessException;
import cn.learn.lfy.mtr.api.LoginRequest;
import cn.learn.lfy.mtr.config.AuthSettings;
import cn.learn.lfy.mtr.domain.model.AppUser;
import cn.learn.lfy.mtr.domain.model.UserCredential;
import cn.learn.lfy.mtr.mapper.AuthMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private AuthMapper authMapper;
    @Mock
    private PasswordHasher passwordHasher;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                authMapper,
                passwordHasher,
                new AuthSettings("test-session", false, 3600, 210000)
        );
    }

    @Test
    void loginCreatesHashedSessionAndReturnsUser() {
        UserCredential credential = credential();
        AppUser user = new AppUser(1L, "演示用户", LocalDateTime.now());
        when(authMapper.findCredentialByUsername("demo")).thenReturn(Optional.of(credential));
        when(passwordHasher.matches("demo1234", credential.passwordHash(), credential.passwordSalt(), 210000))
                .thenReturn(true);
        when(authMapper.findUserById(1L)).thenReturn(Optional.of(user));

        AuthService.LoginResult result = authService.login(new LoginRequest(" Demo ", "demo1234"));

        assertEquals(user, result.user());
        assertNotEquals("", result.token());
        ArgumentCaptor<String> tokenHash = ArgumentCaptor.forClass(String.class);
        verify(authMapper).insertSession(tokenHash.capture(), eq(1L), any(LocalDateTime.class));
        assertEquals(64, tokenHash.getValue().length());
        assertEquals(AuthService.tokenHash(result.token()), tokenHash.getValue());
        assertNotEquals(result.token(), tokenHash.getValue());
        verify(authMapper).deleteExpiredSessions(eq(1L), any(LocalDateTime.class));
    }

    @Test
    void invalidPasswordDoesNotCreateSession() {
        UserCredential credential = credential();
        when(authMapper.findCredentialByUsername("demo")).thenReturn(Optional.of(credential));
        when(passwordHasher.matches("wrong", credential.passwordHash(), credential.passwordSalt(), 210000))
                .thenReturn(false);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.login(new LoginRequest("demo", "wrong"))
        );

        assertEquals(ApiErrorCode.INVALID_CREDENTIALS, exception.getErrorCode());
        verify(authMapper, never()).insertSession(any(), anyLong(), any());
    }

    @Test
    void authenticateAndLogoutUseTokenHash() {
        AppUser user = new AppUser(2L, "光影访客", LocalDateTime.now());
        String hash = AuthService.tokenHash("session-token");
        when(authMapper.findUserBySessionTokenHash(eq(hash), any(LocalDateTime.class)))
                .thenReturn(Optional.of(user));

        assertEquals(Optional.of(user), authService.authenticate("session-token"));

        authService.logout("session-token");
        verify(authMapper).deleteSession(hash);
    }

    private static UserCredential credential() {
        return new UserCredential(
                1L,
                "demo",
                "370d8fe7acecba4f4bd47cac0862194364d2cf8470f9f2b74ca2a0de91975901",
                "2c5c8b95d775e5732593b1af8ec9dd93",
                210000,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
