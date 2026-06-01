package com.deltatrade.platform.common.config;

import com.deltatrade.platform.common.auth.AuthTokenFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final AuthTokenFilter authTokenFilter;

    public SecurityConfig(AuthTokenFilter authTokenFilter) {
        this.authTokenFilter = authTokenFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf().disable()
            .cors(Customizer.withDefaults())
            .httpBasic().disable()
            .formLogin().disable()
            .headers().frameOptions().disable()
            .and()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeRequests()
            .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            .antMatchers(
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
                "/api/auth/real-name/face/notify",
                "/api/payments/wechat/notify")
            .permitAll()
            .anyRequest()
            .authenticated();
        return http.build();
    }
}
