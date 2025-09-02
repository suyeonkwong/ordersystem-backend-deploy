package com.example.ordersystem.common.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@RequiredArgsConstructor
/// PreAuthorize 어노테이션 사용하기 위한 설정
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtTokenFilter jwtTokenFilter;
    private final JwtAuthenticationHandler jwtAuthenticationHandler;
    private final JwtAuthorizationHandler jwtAuthorizationHandler;

    /// 내가 만든 객체는 Component, 외부 라이브러리를 활용한 객체는 Bean + Configuration
    /// Bean은 메서드 위에 붙여 Return 되는 객체를 싱글톤객체로 생성. Component는 클래스 위에 붙여 클래스 자체를 싱글톤 객체로 셍성.
    /// filter 계층에서 filter 로직을 커스텀.
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                /// cors : 특정 도메인에 대한 허용정책, postman은 cors 정책에 적용 X
                .cors(c -> c.configurationSource(corsConfiguration()))
                /// csrf : 보안 공격중 하나로서 타 사이트의 쿠키값을 꺼내서 탈취하는는 공격
                /// 세션기반 로그인(mvc 패턴, ssr)에서는 csrf 별도 설정하는 것이 일반적
                /// 토큰기반 로그인(rest api 서버, csr)에서는 csrf 설정하지 않는것이 일반적
                .csrf(AbstractHttpConfigurer::disable)
                /// http basic은 email/pw를 인코딩하여 인증하는 방식. 간단한 인증의 경우에만 사용.
                .httpBasic(AbstractHttpConfigurer::disable)
                /// 세션 로그인방식 비활성화
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                /// token을 검증하고, token을 통해 Authentication 객체 생성
                .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(e ->
                        e.authenticationEntryPoint(jwtAuthenticationHandler)    /// 401의 경우
                        .accessDeniedHandler(jwtAuthorizationHandler)  /// 403의 경우
                )
                /// 예외 api 정책 설정
                /// authenticated() : 예외를 제외한 모든 요청에 대해서 Authentication 객체가 생성되기를 요구
                .authorizeHttpRequests(a -> a.requestMatchers(
                        "/member/create",
                                "/member/doLogin",
                                "/health",
                                "/member/refresh-at",
                                "/product/list",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html").permitAll().anyRequest().authenticated())
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    private CorsConfigurationSource corsConfiguration() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000", "https://www.suyeon26.store"));
        configuration.setAllowedMethods(Arrays.asList("*")); // 모든 HTTP(get, post 등) 메서드 허용
        configuration.setAllowedHeaders(Arrays.asList("*")); // 모든 헤더요소(Authorization 등) 허용
        configuration.setAllowCredentials(true); // 자격 증명 허용
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); //모든 url패턴에 대해 cors설정 적용
        return source;
    }
}
