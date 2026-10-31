package cn.learn.lfy.mtr.auth;

import cn.learn.lfy.mtr.api.ApiErrorCode;
import cn.learn.lfy.mtr.api.BusinessException;
import cn.learn.lfy.mtr.domain.model.AppUser;
import cn.learn.lfy.mtr.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    private final AuthService authService;
    private final AuthCookieService authCookieService;

    public AuthInterceptor(AuthService authService, AuthCookieService authCookieService) {
        this.authService = authService;
        this.authCookieService = authCookieService;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String token = authCookieService.readToken(request);
        AppUser user = authService.authenticate(token)
                .orElseThrow(() -> new BusinessException(
                        ApiErrorCode.AUTH_REQUIRED,
                        "请先登录后再继续"
                ));
        request.setAttribute(AuthConstants.REQUEST_USER, user);
        return true;
    }
}
