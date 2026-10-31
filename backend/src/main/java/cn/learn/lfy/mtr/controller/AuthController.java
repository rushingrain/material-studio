package cn.learn.lfy.mtr.controller;

import cn.learn.lfy.mtr.api.ApiResponse;
import cn.learn.lfy.mtr.api.LoginRequest;
import cn.learn.lfy.mtr.auth.AuthConstants;
import cn.learn.lfy.mtr.auth.AuthCookieService;
import cn.learn.lfy.mtr.domain.model.AppUser;
import cn.learn.lfy.mtr.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    private final AuthCookieService authCookieService;

    public AuthController(AuthService authService, AuthCookieService authCookieService) {
        this.authService = authService;
        this.authCookieService = authCookieService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AppUser>> login(@Valid @RequestBody LoginRequest request) {
        AuthService.LoginResult result = authService.login(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authCookieService.create(result.token()).toString())
                .body(ApiResponse.success(result.user()));
    }

    @GetMapping("/me")
    public ApiResponse<AppUser> me(
            @RequestAttribute(AuthConstants.REQUEST_USER) AppUser user
    ) {
        return ApiResponse.success(user);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        authService.logout(authCookieService.readToken(request));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authCookieService.clear().toString())
                .body(ApiResponse.success());
    }
}
