package com.wb.between.common.util.OAuth;

import com.wb.between.admin.role.domain.Role;
import com.wb.between.common.util.MaskingUtils;
import com.wb.between.role.repository.RoleRepository;
import com.wb.between.user.domain.User;
import com.wb.between.user.repository.UserRepository;
import com.wb.between.userrole.domain.UserRole;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // 추가

import java.util.Collections;
import java.util.HashSet;
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
@Slf4j
@RequiredArgsConstructor
@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final HttpSession httpSession;

    // 기본 역할 코드 상수 정의 (설정 파일 등으로 관리하는 것이 더 좋음)
    private static final String DEFAULT_ROLE_CODE = "ROLE_USER"; // 일반 사용자 역할 코드
    private static final String STAFF_ROLE_CODE = "ROLE_STAFF";   // 임직원 역할 코드 (예시)

    // 계정 연결 시 세션에 저장할 소셜 정보 키
    public static final String PENDING_SOCIAL_ATTRIBUTES_SESSION_KEY = "pendingSocialAttributes";
    // 세션에 저장될 키 이름들 (계정 연결 흐름용)
    public static final String LINK_TARGET_EMAIL_SESSION_KEY = "linkTargetEmail"; // 연결할 기존 계정 이메일
    public static final String LINK_VERIFICATION_PHONE_SESSION_KEY = "linkVerificationPhone"; // 인증에 사용할 전화번호
    public static final String CONFLICTING_ACCOUNT_EMAIL_SESSION_KEY = "conflictingAccountEmail"; // 충돌 계정 이메일 (폰 중복 시)
    public static final String CONFLICTING_ACCOUNT_PHONE_SESSION_KEY = "conflictingAccountPhone"; // 충돌 계정 전화번호 (폰 중복 시)
    public static final String LINK_CONTEXT_SESSION_KEY = "accountLinkingContext"; // 연결 흐름 구분용

    // 새로운 에러 코드 정의
    public static final String ERROR_CODE_EMAIL_EXISTS_PHONE_MISMATCH_LINK = "EMAIL_EXISTS_PHONE_MISMATCH_LINK";
    public static final String ERROR_CODE_NEW_EMAIL_PHONE_CONFLICT_LINK = "NEW_EMAIL_PHONE_CONFLICT_LINK";


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
        User user = snsSaveOrLogin(attributes, registrationId);
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

    // 소셜로그인 시 사용자 존재 유무 및 휴대번호 중복 여부 파악 후 없으면 저장하거나 업데이트
    private User snsSaveOrLogin(OAuthAttributes attributes, String registrationId) {
        log.info("CustomOAuth2UserService|saveOrUpdate|Start ===========> attributes : {}, registrationId : {}} ", attributes.getAttributes(), registrationId);

        String socialEmail = attributes.getEmail();
        String rawSocialMobile = attributes.getMobile(); // 소셜 로그인 시 제공되는 휴대폰 번호(null 일 수 있음)
        String cleanSocialMobile = (rawSocialMobile != null) ? rawSocialMobile.replaceAll("-", "") : null;

        // 소셜 이메일로 기존 사용자 있는지 여부 조회
//        Optional<User> userOptionalByEmail = userRepository.findByEmail(socialEmail);
        Optional<User> userOptionalByEmail = userRepository.findByUsernameWithRolesAndPermissions(socialEmail);


        // 1. 소셜 이메일이 같은 기존 사용자가 있는 경우
        if (userOptionalByEmail.isPresent()) {
            User existingUser = userOptionalByEmail.get();
            log.info("CustomOAuth2UserService|snsSaveOrLogin|기존 사용자 발견 (이메일 일치): {}", existingUser.getEmail());

            // 1-1. 이메일 일치, 휴대폰 번호 일치 - 기존 사용자로 판단하여 로그인 방법(LoginM) 업데이트 후 로그인 진행
            if(java.util.Objects.equals(existingUser.getPhoneNo(), cleanSocialMobile)) {
                log.info("CustomOAuth2UserService|snsSaveOrLogin|이메일 및 휴대폰 번호 일치.");
                log.info("CustomOAuth2UserService|snsSaveOrLogin|회원 loginM {} : ", existingUser.getLoginM());

                // 계정 연동 안되어 있을 때(LoginM이 null 이거나 다른 값일 때)
                if(existingUser.getLoginM() == null || !existingUser.getLoginM().equals(registrationId.toUpperCase())) {
                    log.info("CustomOAuth2UserService|snsSaveOrLogin|loginM 업데이트 시도: {}", cleanSocialMobile);
                    existingUser.setLoginM(registrationId.toUpperCase());
                    return userRepository.save(existingUser);
                } else {
                // 계정 연동 되어 있을 때
                    log.info("CustomOAuth2UserService|snsSaveOrLogin|기존 사용자 로그인 방법 업데이트: {}", existingUser.getLoginM());
                    log.info("CustomOAuth2UserService|snsSaveOrLogin|기존 사용자 로그인 진행 | existingUser : {}", existingUser);
                    return existingUser;
                }

            } else {
            // 1-2. 이메일 일치, 휴대폰 번호 불일치
                log.warn("이메일 {}은(는) 일치하나 휴대폰 번호가 다릅니다 (기존: {}, 소셜: {}). " +
                                "기존 계정의 휴대폰 인증 후 소셜 계정 연결을 유도합니다.",
                        existingUser.getEmail(), existingUser.getPhoneNo(), cleanSocialMobile);

                httpSession.setAttribute(PENDING_SOCIAL_ATTRIBUTES_SESSION_KEY, attributes); // pendingSocialAttributes : 소셜 정보 세션 저장
                httpSession.setAttribute(LINK_TARGET_EMAIL_SESSION_KEY, existingUser.getEmail()); // linkTargetEmail : 기존 회원 계정 이메일
                httpSession.setAttribute(LINK_VERIFICATION_PHONE_SESSION_KEY, cleanSocialMobile);   // linkVerificationPhone : 기존 회원 계정 전화번호
                httpSession.setAttribute(LINK_CONTEXT_SESSION_KEY, "LINK_SOCIAL_TO_EXISTING_EMAIL_VERIFY_EXISTING_PHONE");  // accountLinkingContext : 계정 연결 흐름 구분

                // 안내 및 인증 페이지로 이동 설정
                throw new OAuth2AuthenticationException(
                        new OAuth2Error(
                                ERROR_CODE_EMAIL_EXISTS_PHONE_MISMATCH_LINK,
                                "이 이메일(" + socialEmail + ")은 이미 가입되어 있습니다. " +
                                        registrationId.toUpperCase() + " 계정을 연결하려면 기존에 등록된 휴대폰 번호로 인증을 완료해주세요.",
                                null
                        ),
                        "Email matches, but phone number differs. Verification of existing account's phone needed to link social account."
                );
            }

        } else {
        // 2. 이메일이 불일치
            log.info("CustomOAuth2UserService|snsSaveOrLogin|이메일이 같은 기존 사용자가 없는 경우");

            // 2-1. 다른 계정이 이미 소셜 로그인 휴대폰 번호를 사용 중일 경우

            // 휴대폰 번호로 사용자 조회
            Optional<User> userOptionalByPhone = userRepository.findByPhoneNo(cleanSocialMobile);

            // 이메일 불일치, 휴대폰 번호 일치
            if (userOptionalByPhone.isPresent()) {
                User existingUserWithThisPhone = userOptionalByPhone.get();
                log.warn("소셜 프로필의 휴대폰 번호 ({})가 기존 다른 계정 (이메일: {})에 이미 등록되어 있습니다. 계정 연결을 유도합니다.",
                        cleanSocialMobile, existingUserWithThisPhone.getEmail());

                httpSession.setAttribute(PENDING_SOCIAL_ATTRIBUTES_SESSION_KEY, attributes);
                httpSession.setAttribute(CONFLICTING_ACCOUNT_EMAIL_SESSION_KEY, existingUserWithThisPhone.getEmail());
                httpSession.setAttribute(CONFLICTING_ACCOUNT_PHONE_SESSION_KEY, cleanSocialMobile);
                httpSession.setAttribute(LINK_CONTEXT_SESSION_KEY, "LINK_SOCIAL_TO_EXISTING_PHONE");

                throw new OAuth2AuthenticationException(
                        new OAuth2Error(
                                ERROR_CODE_NEW_EMAIL_PHONE_CONFLICT_LINK,
                                "이 휴대폰 번호(" + rawSocialMobile + ")는 이미 다른 계정(" + MaskingUtils.maskEmail(existingUserWithThisPhone.getEmail()) +")에 등록되어 있습니다. 해당 계정에 소셜 계정을 연결하시겠습니까?",
                                null
                        ),
                        "New social email, but phone number conflicts with another account. Account linking suggested."
                );
            } else {
                // 2-2. 이메일, 휴대번호가 중복되지 않을 경우 > 신규 가입

                User newUser = attributes.toEntity();
                newUser.setLoginM(registrationId.toUpperCase()); // 가입 경로 설정 (NAVER, KAKAO 등)

                log.info("CustomOAuth2UserService|snsSaveOrLogin|신규 사용자 엔티티 생성: email={}, phoneNo={}", newUser.getEmail(), newUser.getPhoneNo());

                // 4. 역할 할당
                String targetRoleCode = DEFAULT_ROLE_CODE;
                if (newUser.getEmail() != null && newUser.getEmail().endsWith("@winbit.kr")) {
                    targetRoleCode = STAFF_ROLE_CODE;
                }
                Role assignedRole = roleRepository.findByRoleCode(targetRoleCode)
                        .orElseThrow(() -> {
//                            log.error("CustomOAuth2UserService|snsSaveOrLogin|역할 코드 {}를 찾을 수 없습니다.", targetRoleCode);
                            return new IllegalStateException("기본 사용자 역할을 찾을 수 없습니다. 시스템 설정을 확인해주세요.");
                        });

                UserRole ur = new UserRole();
                ur.setUser(newUser);
                ur.setRole(assignedRole);

                if (newUser.getUserRole() == null) { // User 엔티티에서 @Builder.Default로 초기화 권장
                    newUser.setUserRole(new HashSet<>());
                }
                newUser.getUserRole().add(ur);

                // 5. 사용자 저장
                User savedUser = userRepository.save(newUser);
                log.info("CustomOAuth2UserService|snsSaveOrLogin|신규 사용자 저장 완료: {}", savedUser.getEmail());

                // 저장된 사용자의 ID를 사용하여, 모든 연관 관계(권한 포함)가 로드된 User 객체를 다시 조회하여 반환합니다.
                return userRepository.findByIdWithAuthorities(savedUser.getUserNo())
                        .orElseThrow(() -> {
                            log.error("CustomOAuth2UserService|snsSaveOrLogin|신규 저장 후 사용자 ID [{}] 조회 실패 (authorities 포함).", savedUser.getUserNo());
                            return new IllegalStateException("저장 후 사용자 정보를 가져오는 데 실패했습니다: " + savedUser.getUserNo());
                        });

            }

        }

// =====================================================================================================================
    /*
        // 1. 이메일, 휴대번호로 사용자 조회(없으면 null 리턴)
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

            //1. 부여할 역할 코드 결정
            String targetRoleCode = DEFAULT_ROLE_CODE; // 기본값: 일반 사용자
            if (user.getEmail() != null && user.getEmail().endsWith("@winbit.kr")) { // 도메인 체크 (null 체크 추가)
                targetRoleCode = STAFF_ROLE_CODE; // 특정 도메인이면 임직원 역할
            }

            //2. 역할(Role) 엔티티 조회
            Role assignedRole = roleRepository.findByRoleCode(targetRoleCode)
                    .orElseThrow(() -> new IllegalStateException("역할을 찾을 수 없습니다. 시스템 설정 오류입니다."));

            //3. UserRole 조인 엔티티 생성 & 연결
            UserRole ur = new UserRole();
            ur.setUser(user);
            ur.setRole(assignedRole);

            //4. User 컬렉션에 추가
            // user.getUserRole()이 null일 수 있으므로 초기화 확인
            if (user.getUserRole() == null) {
                user.setUserRole(new HashSet<>());
            }

            user.getUserRole().add(ur);
            //5. 신규 저장
            userRepository.save(user);

            //6.
            User savedUser = userRepository.findByIdWithAuthorities(user.getUserNo())
                    .orElseThrow(() -> new IllegalStateException("저장 후 사용자 조회 실패: " + user.getUserNo()));

            return savedUser;
        }
    */

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
