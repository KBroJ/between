package com.wb.between.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration      // 스프링 설정 클래스임을 나타냄
@EnableWebSecurity  // Spring Security 설정을 활성화
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())               // CSRF 보안 설정(위조 요청 방지)을 비활성화
                .authorizeHttpRequests(auth -> auth    // URL 기반 접근제어 설정
                        .requestMatchers("/", "/signup", "/checkEmail", "/send-verification", "/css/**", "/js/**").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form //
//                        .loginPage("/login")
                        .loginPage("/")
                        .permitAll()
                );

        return http.build();
    }

}
