package com.wb.between.config;

import com.wb.between.common.util.OAuth.CustomOAuth2UserService;
import com.wb.between.user.service.UserDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@RequiredArgsConstructor
@Configuration
public class WebSecurityConfig {

    private final UserDetailService userDetailService;
    private final CustomOAuth2UserService customOAuth2UserService;

    // 스프링 시큐리티 기능 비활성화
    @Bean
    public WebSecurityCustomizer configure() {
        return (web) -> web.ignoring()
                .requestMatchers("/static/**", "/templates/**");
    }

    // 특정 HTTP 요청에 대한 웹 기반 보안 구성
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/css/**", "/js/**", "/img/**",
                                "/", "/main",
                                "/signup", "/findUserInfo","/checkEmail", "/send-verification",
                                "/signup/verify-code", "/findUserInfo/verify-code",
                                "/findUserInfo/reqSendEmail", "/findUserInfo/verifyPwdCode", "/api/resetPwd",
                                "/login", "/faqList", "/error", "/favicon.ico",  "/api/**",
                                "/oauth2/**"
                        ).permitAll() // "/login" 누구나 접근 가능하게
                        .anyRequest().authenticated()             // 나머지 요청은 인증 필요
                )
                // 4. 폼 기반 로그인 설정
                .formLogin(form -> form
                        .loginPage("/login")                // 커스텀 로그인 페이지 지정
                        .defaultSuccessUrl("/")        // 로그인 성공 시 이동할 URL (기본값은 '/')
                        .permitAll() // 로그인 페이지 자체는 모든 사용자가 접근 가능해야 함 (authorizeHttpRequests에서 이미 /login을 permitAll 했으므로 중복될 수 있으나 명시적으로 추가 가능)
                )
                // 5. 로그아웃 설정
                .logout(logout -> logout
                        .logoutSuccessUrl("/")         // 로그아웃 성공 시 이동할 URL
                        .invalidateHttpSession(true)        // 로그아웃 시 세션 무효화
                )
                // 6. CSRF 비활성화
                .csrf(AbstractHttpConfigurer::disable) // .csrf(csrf -> csrf.disable()) 와 동일, 메서드 레퍼런스 사용

                // 7. OAuth2 소셜 로그인 설정 추가
                .oauth2Login(oauth2 -> oauth2
                                .loginPage("/login") // 로그인 페이지 지정 (인증이 필요할 때 이동)
                                // .defaultSuccessUrl("/") // 로그인 성공 시 이동할 URL (SuccessHandler 사용 시 주석 처리 가능)
                                .userInfoEndpoint(userInfo -> userInfo
                                        .userService(customOAuth2UserService) // 소셜 로그인 성공 후 사용자 정보 처리 서비스 지정
                                )
                        // 로그인 성공 핸들러 (선택적): 로그인 성공 후 특정 로직 수행 필요 시
                         .successHandler(oAuth2LoginSuccessHandler())
                        // 로그인 실패 핸들러 (선택적)
                         .failureHandler(oAuth2LoginFailureHandler())
                );

        return http.build();
    }

    // 인증 관리자 관련 설정
    /*
     *
     * AuthenticationManager Bean 정의 (Spring Security 6+ 스타일)
     * 이전 방식(http.getSharedObject(AuthenticationManagerBuilder.class))은 deprecated 되었으므로,
     * DaoAuthenticationProvider를 직접 생성하고 설정하여 ProviderManager를 반환하는 방식을 사용합니다.


     */
    @Bean
    public AuthenticationManager authenticationManager(
            UserDetailService userDetailService, // 사용자 정의 UserDetailsService 주입
            BCryptPasswordEncoder bCryptPasswordEncoder) { // PasswordEncoder 주입

        // DaoAuthenticationProvider: UserDetailsService와 PasswordEncoder를 사용하여
        // 사용자 인증을 처리하는 AuthenticationProvider 구현체
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
        daoAuthenticationProvider.setUserDetailsService(userDetailService); // 사용자 정보 로드 서비스 설정
        daoAuthenticationProvider.setPasswordEncoder(bCryptPasswordEncoder); // 비밀번호 인코더 설정

        // ProviderManager: 여러 AuthenticationProvider를 관리하고 인증 요청을 위임하는
        // AuthenticationManager의 표준 구현체
        // 여기서는 DaoAuthenticationProvider 하나만 사용합니다.
        return new ProviderManager(daoAuthenticationProvider);
    }

    // 패스워드 인코더로 사용할 빈 등록
    @Bean
    public  BCryptPasswordEncoder bCryptPasswordEncoder(){
        return new BCryptPasswordEncoder();
    }


    // --- 선택적: 로그인 성공/실패 핸들러 빈 등록 ---
    /*
    */
    @Bean
    public AuthenticationSuccessHandler oAuth2LoginSuccessHandler() {
        // 성공 시 로직 구현 (예: 첫 로그인 시 추가 정보 입력 페이지 이동 등)
        return (request, response, authentication) -> {
            // Custom 로직 수행
            response.sendRedirect("/"); // 예시: 성공 후 메인 페이지로 리다이렉트
        };
    }

    @Bean
    public AuthenticationFailureHandler oAuth2LoginFailureHandler() {
        // 실패 시 로직 구현 (예: 에러 메시지와 함께 로그인 페이지로 리다이렉트)
        return (request, response, exception) -> {
            // Custom 로직 수행 (로깅 등)
            response.sendRedirect("/login?error=oauth_fail"); // 예시: 실패 시 쿼리 파라미터와 함께 리다이렉트
        };
    }
}

