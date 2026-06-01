package com.deltatrade.platform.common.auth;

import com.deltatrade.platform.common.api.ApiResponse;
import com.deltatrade.platform.common.exception.ErrorCode;
import com.deltatrade.platform.modules.auth.service.AuthRedisStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class AuthTokenFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AuthTokenFilter.class);
    private static final String TOKEN_COOKIE_NAME = "delta_trade_token";
    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final List<String> PUBLIC_PATHS = Arrays.asList(
        "/actuator/health",
        "/actuator/info",
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/ws-im/**",
        "/api/public/**",
        "/api/auth/sms-code",
        "/api/auth/sms-login",
        "/api/auth/password-login",
        "/api/auth/register/verify-code",
        "/api/auth/register/complete",
        "/api/auth/password-reset/verify-code",
        "/api/auth/password-reset/complete",
        "/api/auth/wechat/qr-code",
        "/api/auth/wechat/qr-page",
        "/api/auth/wechat/qr-image",
        "/api/auth/wechat/poll",
        "/api/auth/wechat/bind-phone",
        "/api/auth/wechat/callback",
        "/api/payments/wechat/notify"
    );

    private final AuthRedisStore authRedisStore;
    private final ObjectMapper objectMapper;

    public AuthTokenFilter(AuthRedisStore authRedisStore, ObjectMapper objectMapper) {
        this.authRedisStore = authRedisStore;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        for (String publicPath : PUBLIC_PATHS) {
            if (PATH_MATCHER.match(publicPath, path)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        String traceId = ensureTraceId(request, response);
        boolean forwarded = false;
        try {
            String token = resolveToken(request);
            if (!StringUtils.hasText(token)) {
                log.warn("auth token missing path={} traceId={}", request.getRequestURI(), traceId);
                writeUnauthorized(response, "登录已失效，请重新登录");
                return;
            }

            long startAt = System.currentTimeMillis();
            AuthRedisStore.LoginSessionCache session = authRedisStore.getLoginSession(token);
            if (session == null) {
                log.warn("auth token not found path={} costMs={} traceId={}",
                    request.getRequestURI(), System.currentTimeMillis() - startAt, traceId);
                writeUnauthorized(response, "登录已失效，请重新登录");
                return;
            }

            if (session.getExpireAt() == null || session.getExpireAt().isBefore(LocalDateTime.now())) {
                authRedisStore.deleteLoginSession(token);
                log.warn("auth token expired path={} userId={} costMs={}",
                    request.getRequestURI(), session.getUserId(), System.currentTimeMillis() - startAt);
                writeUnauthorized(response, "登录已过期，请重新登录");
                return;
            }

            AuthPrincipal principal = new AuthPrincipal(
                token,
                session.getUserId(),
                session.getNickname(),
                session.getPhone(),
                session.isVerified(),
                session.isHasPassword()
            );
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                AuthorityUtils.NO_AUTHORITIES
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.info("auth token verified path={} userId={} costMs={}",
                request.getRequestURI(), principal.getUserId(), System.currentTimeMillis() - startAt);
            forwarded = true;
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
            if (!forwarded) {
                MDC.remove("traceId");
            }
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            return authorization.substring(7).trim();
        }
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (TOKEN_COOKIE_NAME.equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                return cookie.getValue().trim();
            }
        }
        return null;
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(
            ApiResponse.failure(ErrorCode.UNAUTHORIZED.name(), message, MDC.get("traceId"))
        ));
    }

    private String ensureTraceId(HttpServletRequest request, HttpServletResponse response) {
        String traceId = Optional.ofNullable(MDC.get("traceId"))
            .filter(StringUtils::hasText)
            .orElseGet(() -> Optional.ofNullable(request.getHeader(TRACE_ID_HEADER))
                .filter(StringUtils::hasText)
                .orElse(UUID.randomUUID().toString().replace("-", "")));
        MDC.put("traceId", traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        return traceId;
    }
}
