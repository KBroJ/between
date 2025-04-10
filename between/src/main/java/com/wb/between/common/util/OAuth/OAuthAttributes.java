package com.wb.between.common.util.OAuth;

import com.wb.between.user.domain.User;
import lombok.Builder;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.time.LocalDateTime;

/*
    네이버 소셜 로그인 서비스로부터 받은 사용자 정보를 표준화된 형식으로 변환하는 역할
*/
@Getter
public class OAuthAttributes {

    private Map<String, Object> attributes; // OAuth2User.getAttributes() 반환값
    private String nameAttributeKey;        // 사용자 이름 속성의 키 값 (Naver: "response")
    private String name;
    private String email;
    private String mobile; // 네이버에서 제공하는 휴대폰 번호

    @Builder
    public OAuthAttributes(Map<String, Object> attributes, String nameAttributeKey, String name, String email, String mobile) {
        this.attributes = attributes;
        this.nameAttributeKey = nameAttributeKey;
        this.name = name;
        this.email = email;
        this.mobile = mobile;
    }

    // registrationId(naver, google 등) 와 userNameAttributeName(provider 설정의 user-name-attribute) 을 통해
    // 어떤 소셜 로그인인지 판단하고, 해당 소셜의 attributes 맵에서 필요한 정보를 추출
    public static OAuthAttributes of(String registrationId, String userNameAttributeName, Map<String, Object> attributes) {

        if ("naver".equals(registrationId)) {
            return ofNaver("email", attributes); // 네이버는 user-name-attribute가 response지만, 실제 고유 ID는 response 내의 id 임
        }
        //카카오
        if("kakao".equals(registrationId)) {
            return ofKakao("email", attributes);
        }

        // 다른 소셜 로그인(Google, Kakao 등) 추가 시 여기에 분기 추가

        return null; // 지원하지 않는 소셜 로그인일 경우
    }


    // 네이버 사용자 정보 추출
    @SuppressWarnings("unchecked") // 형변환 경고 무시
    private static OAuthAttributes ofNaver(String userNameAttributeName, Map<String, Object> attributes) {

        // 네이버는 최상위에 resultcode, message, response 필드를 가짐
        // 실제 사용자 정보는 response 필드 값인 Map 안에 있음
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");

        System.out.println("OAuthAttributes|ofNaver|response = " + response);

        if (response == null) {
            // 예외 처리 또는 로깅: 네이버 응답 형식이 예상과 다름
            throw new IllegalArgumentException("Invalid Naver response structure.");
        }

        // 네이버에서 제공하는 사용자 정보 추출
        return OAuthAttributes.builder()
                .name((String) response.get("name"))
                .email((String) response.get("email"))
                .mobile((String) response.get("mobile"))    // 네이버는 mobile 제공
                .attributes(response)                       // attributes에는 response 맵 자체를 저장
                .nameAttributeKey(userNameAttributeName)    // 여기서는 response 맵 안의 고유 식별자 키 ('id')
                .build();
    }

    private static OAuthAttributes ofKakao(String userNameAttributeName, Map<String, Object> attributes) {

        // 카카오 최상위에 id, connected_at, properties, kakao_account 필드를 가짐
        // 이름 정보는 properties Map안에 있음
        Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");
        // 카카오 계정 정보는 kakao_account 필드 값인 Map 안에 있음
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");

        System.out.println("OAuthAttributes|ofKakao|kakaoAccount = " + kakaoAccount);

        // 수정 가능한 새 맵 생성 (원본 복사)
        Map<String, Object> mutableAttributes = new HashMap<>(attributes);

        if (properties == null || kakaoAccount == null) {
            // 예외 처리 또는 로깅: 네이버 응답 형식이 예상과 다름
            throw new IllegalArgumentException("Invalid Kakao response structure.");
        }

        mutableAttributes.put("email", kakaoAccount.get("email")); // 루트 레벨에 email 추가 (선택적)

        // 네이버에서 제공하는 사용자 정보 추출
        return OAuthAttributes.builder()
                .name((String) properties.get("nickname"))
                .email((String) kakaoAccount.get("email"))
                .mobile("")    // 카카오는 mobile 미제공
                .attributes(mutableAttributes)                       // attributes에는 response 맵 자체를 저장
                .nameAttributeKey(userNameAttributeName)    // 여기서는 response 맵 안의 고유 식별자 키 ('id')
                .build();
    }

    // OAuthAttributes 정보를 바탕으로 DB에 저장하기 위해 사용할 User 엔티티 생성
    public User toEntity() {

        // 휴대폰 번호에서 '-' 제거 (DB 저장 형식에 맞게)
        String cleanMobile = (this.mobile != null) ? this.mobile.replaceAll("-", "") : null;

        return User.builder()
                .name(name)
                .email(email)
                .phoneNo(cleanMobile) // - 제거한 휴대폰 번호
                .password("NAVER_USER_PASSWORD_" + System.currentTimeMillis()) // 소셜 로그인 사용자는 비밀번호 불필요. 임의 값 또는 Null 처리 필요 (DB 제약조건 확인)
                .userStts("일반") // 예: 활성 상태 (Active)
                .authCd("일반") // 예: 일반 사용자 권한
                .loginM("NAVER") // 어떤 소셜 로그인을 통해 가입했는지 구분
                .createDt(LocalDateTime.now())
                // updateDt는 @PreUpdate 등으로 자동 관리되므로 여기선 설정 불필요
                .build();
    }
}
