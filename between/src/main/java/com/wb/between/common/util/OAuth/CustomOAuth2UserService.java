package com.wb.between.common.util.OAuth;

import com.wb.between.user.domain.User;
import com.wb.between.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // 추가

import java.util.Collections;
import java.util.Optional;

/*
    네이버로부터 사용자 정보를 받아온 후, 우리 서비스의 DB에 사용자를 저장하거나 업데이트하는 로직을 담당
    호출 흐름
        1. 사용자: 소셜 로그인 버튼 클릭
        2. 브라우저: 요청 전송
            브라우저는 GET /oauth2/authorization/naver 요청을 백엔드 스프링 부트 애플리케이션으로 보냅니다.

        3.Spring Security: OAuth2AuthorizationRequestRedirectFilter 동작
            스프링 시큐리티 설정 (WebSecurityConfig)에 의해 활성화된 필터 중 OAuth2AuthorizationRequestRedirectFilter가 /oauth2/authorization/{registrationId} 패턴의 요청을 가로챕니다.
            {registrationId}가 naver인 것을 인지합니다.
            application.yml에 정의된 spring.security.oauth2.client.registration.naver 설정을 읽어옵니다 (client-id, scope, 네이버 인증 URL 등).
            네이버 인증 페이지로 리디렉션하기 위한 URL을 생성합니다. 이 URL에는 client_id, redirect_uri, response_type=code, 요청된 scope, 그리고 CSRF 방지를 위한 state 파라미터 등이 포함됩니다.

        4. 브라우저: 네이버 인증 페이지로 이동
        5. 사용자: 네이버 로그인 및 권한 동의
            네이버 로그인 페이지가 표시됩니다. 사용자는 네이버 아이디와 비밀번호를 입력하여 로그인합니다.
            (최초 시도 시 또는 동의 철회 후) 애플리케이션이 요청한 정보(이름, 이메일, 휴대폰 번호) 제공에 대한 동의 화면이 나타나면, 사용자가 "동의하기"를 클릭합니다.
        6.네이버 서버: 권한 부여 및 콜백 리디렉션
            네이버 인증 서버는 사용자의 인증 및 권한 부여가 성공하면, application.yml에 설정된 redirect-uri (http://localhost:8080/login/oauth2/code/naver)로 브라우저를 다시 리디렉션 시킵니다.
            중요: 이때 리디렉션 URL 뒤에 **code (Authorization Code)**와 state 파라미터를 쿼리 스트링으로 추가하여 보냅니다.
                 (예: http://localhost:8080/login/oauth2/code/naver?code=ABCDEFG&state=XYZ123)

        7. 브라우저: 콜백 URL 요청
            브라우저는 네이버가 보내준 리디렉션 URL (/login/oauth2/code/naver?code=...&state=...)로 다시 스프링 부트 애플리케이션에 요청을 보냅니다.

=============================================== Spring Security 내부 동작 ===========================================
        8. Spring Security: OAuth2LoginAuthenticationFilter 동작
            이번에는 OAuth2LoginAuthenticationFilter가 /login/oauth2/code/{registrationId} 패턴의 요청을 가로챕니다.
            URL에서 code와 state 파라미터 값을 추출합니다.
            state 값의 유효성을 검증하여 CSRF 공격을 방어합니다.
            추출한 code 값, application.yml의 client-id, client-secret, 네이버 token-uri를 사용하여
            백그라운드에서 네이버 토큰 발급 서버(https://nid.naver.com/oauth2.0/token)에 Access Token 요청을 보냅니다.

        9. 네이버 서버: Access Token 발급
            네이버 토큰 서버는 code와 클라이언트 정보를 검증하고, 유효하면 Access Token과 기타 정보(Refresh Token 등)를 JSON 형태로 응답합니다.

        10. Spring Security: 사용자 정보 요청
            OAuth2LoginAuthenticationFilter는 9단계에서 받은 Access Token을 사용하여,
            application.yml에 정의된 네이버 user-info-uri (https://openapi.naver.com/v1/nid/me)로 사용자 정보(Profile) 요청을 보냅니다.
            (HTTP 요청 헤더에 Authorization: Bearer <Access Token> 포함)


        11. 네이버 서버: 사용자 정보 응답
                네이버 API 서버는 Access Token을 검증하고, 유효하면 요청된 scope에 해당하는 사용자 정보를 JSON 형태로 응답합니다.
                (이름, 이메일, 휴대폰 번호 등이 response 키 아래에 중첩된 형태로 옴)
=============================================== Spring Security 내부 동작 ===========================================

        12. Spring Security & CustomOAuth2UserService: loadUser 메소드 호출 ★★★★★★★
                OAuth2LoginAuthenticationFilter는 11단계에서 받은 사용자 정보와 Access Token 등의 정보를 OAuth2UserRequest 객체에 담아,
                WebSecurityConfig의 .userInfoEndpoint().userService()에 등록된 CustomOAuth2UserService의 loadUser 메소드를 호출합니다.

        13. Spring Security: 인증 완료 처리
            OAuth2LoginAuthenticationFilter는 loadUser가 반환한 DefaultOAuth2User 객체를 받습니다.
            이 정보를 바탕으로 OAuth2AuthenticationToken (Spring Security의 Authentication 구현체)을 생성합니다.
            SecurityContextHolder에 이 Authentication 객체를 저장하여, 현재 세션에서 사용자가 인증되었음을 기록합니다.

        14. Spring Security: 최종 리디렉션
            인증이 성공적으로 완료되었으므로, 사용자를 WebSecurityConfig에 설정된 성공 URL (defaultSuccessUrl("/")) 또는 로그인 전 접근하려 했던 페이지로 리디렉션 시킵니다.

*/
@RequiredArgsConstructor
@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;
    // HttpSession 주입 (선택적: 세션에 사용자 정보 저장 시 필요)
    // private final HttpSession httpSession;

    @Override
    @Transactional // 추가: DB 작업을 포함하므로 트랜잭션 처리
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        System.out.println("CustomOAuth2UserService|loadUser|userRequest = " + userRequest);

        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        // 1. 소셜 로그인 서비스 구분 (naver, google, ...)
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        // 2. OAuth2 로그인 시 키가 되는 필드값 (Primary Key와 같은 역할)
        // application.yml의 provider 설정에서 user-name-attribute 값 : response
        String userNameAttributeName = userRequest.getClientRegistration()
                                                    .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        // 3. OAuth2UserService를 통해 가져온 OAuth2User의 attribute를 담을 클래스
        OAuthAttributes attributes = OAuthAttributes.of(registrationId, userNameAttributeName, oAuth2User.getAttributes());
        System.out.println("CustomOAuth2UserService|loadUser|attributes = " + attributes);


        // 4. 소셜로그인 시 사용자 존재 유무 파악 후 없으면 신규 생성
        User user = snsSaveOrLogin(attributes);
        System.out.println("CustomOAuth2UserService|loadUser|user = " + user);

        // 5. 세션에 사용자 정보 저장 (선택적)
        // SessionUser sessionUser = new SessionUser(user);
        // httpSession.setAttribute("user", sessionUser); // SessionUser는 직렬화 가능한 별도 DTO 권장

        // 6. Spring Security의 OAuth2User 객체 반환
//        return new DefaultOAuth2User(
//                Collections.singleton(new SimpleGrantedAuthority(user.getAuthCd())), // 사용자의 권한 정보
//                attributes.getAttributes(),          // 소셜 서비스에서 받은 원본 속성 맵
//                attributes.getNameAttributeKey());   // 사용자 이름 속성 키 (Naver: "response" 안의 "id")

        return user;
    }

    // 소셜로그인 시 사용자 존재 유무 파악 후 없으면 신규 생성
    private User snsSaveOrLogin(OAuthAttributes attributes) {
        System.out.println("CustomOAuth2UserService|saveOrUpdate|Start ===========> attributes = " + attributes);

        // 1. 이메일로 사용자 조회(없으면 null 리턴)
        Optional<User> optionalUser = userRepository.findByEmailAndPhoneNo(attributes.getEmail(), attributes.getMobile().replaceAll("-", ""));

        User user; // 최종적으로 저장할 User 객체를 담을 변수

        // 2. 사용자가 존재하는지 확인
        if (optionalUser.isPresent()) {
            // 3-1. 사용자가 존재하면 기존 User 객체를 반환 (로그인 처리)
            user = optionalUser.get();
            System.out.println("CustomOAuth2UserService|saveOrUpdate|기존 사용자 로그인: " + user.getEmail());

            return user;
        } else {
            // 3-2. 사용자가 존재하지 않으면 새로 생성
            user = attributes.toEntity();
            System.out.println("CustomOAuth2UserService|saveOrUpdate|신규 사용자 생성: " + user.getEmail());

            return userRepository.save(user);
        }


    /*
        람다식 사용 예시
        User user = userRepository.findByEmail(attributes.getEmail())
                // 사용자가 있으면 이름, 휴대폰 번호 등 변경사항 업데이트
//                .map(entity -> entity.update(attributes.getName(), attributes.getMobile())) // User 엔티티에 update 메소드 추가 필요
                // 사용자가 없으면(Optional 안에 User 객체가 존재하지 않을 경우) User 객체를 생성
                .orElse(attributes.toEntity());

        return userRepository.save(user);
    */

    }
}
