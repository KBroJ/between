package com.wb.between.user.domain;

import com.wb.between.usercoupon.domain.UserCoupon;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.time.LocalDateTime;
import java.util.*;

@Entity
//@Table(name = "User") // 매핑할 테이블명 선언 (생략 시 클래스명을 테이블명으로 사용가능)
@Data
@Builder                // 빌더 패턴 클래스 생성
@NoArgsConstructor      // 인자가 없는 생성자 생성
@AllArgsConstructor     // 모든 필드를 인자로 받는 생성자 생성
@EqualsAndHashCode(exclude = "usercoupon") // 양방향 연관관계 시 순환 참조 방지 위해 추가 권장
public class User implements UserDetails, OAuth2User {

    @Id                                                                         // PK 필드 선언
    @GeneratedValue(strategy = GenerationType.IDENTITY)                         // 기본 키를 자동으로 생성(IDENTITY전략은 기본 키 생성을 데이터베이스에 위임)
    @Column(name = "userNo")                                                    // 매핑할 컬럼명 선언 (생략 시 필드명을 컬럼명으로 사용가능)
    private Long userNo;

    @Column(name = "email", length = 100, nullable = false, unique = true)
    private String email;

    @Column(name = "password", length = 255, nullable = false)
    private String password;

    @Column(name = "name", length = 50, nullable = false)
    private String name;

    @Column(name = "phoneNo", length = 11, nullable = false)
    private String phoneNo;

    @Column(name = "userStts", length = 10, nullable = false)
    private String userStts;

    @Column(name = "authCd", length = 10, nullable = false)
    private String authCd;

    @CreationTimestamp                                                          // 엔티티가 생성되어 저장될 때 시간이 자동 저장
    @Column(name = "createDt", nullable = false, updatable = false)
    private LocalDateTime createDt;

//    @UpdateTimestamp                                                            // 엔티티가 저장되거나 업데이트 될 때 시간이 자동 저장 => Insert시에도 값이 저장되어버리므로 주석처리
    @Column(name = "updateDt")
    private LocalDateTime updateDt;
    // 생성 시에는 updateDt에 값이 저장되지 않도록 처리
    @PrePersist
    public void prePersist() {
    }
    // 수정 시에 updateDt에 값이 저장되도록 처리
    @PreUpdate
    public void preUpdate() {
        this.updateDt = LocalDateTime.now();
    }

    @Column(name = "loginM", length = 10, nullable = false)
    private String loginM;

// === 소셜로그인(OAuth2) 정보 저장을 위한 필드 추가 ===
    @Transient // DB 컬럼으로 만들지 않음 (DB에 저장할 필요 없는 임시 데이터)
    private Map<String, Object> attributes;

    @Transient // DB 컬럼으로 만들지 않음
    private String nameAttributeKey; // 사용자 이름(고유 식별자)을 찾기 위한 키값 (예: naver="response")
// === 소셜로그인(OAuth2) 정보 저장을 위한 필드 추가 끝 ===


    @Override   // 권한
    public Collection<? extends GrantedAuthority> getAuthorities(){
        return List.of(new SimpleGrantedAuthority("user"));
    }

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude // Lombok이 생성하는 toString() 메소드에서 이 필드를 제외시킴!
    private Set<UserCoupon> usercoupon = new HashSet<>(); // 사용자가 가진 쿠폰 목록 (UserCoupon 객체들을 통해 접근)

    // 사용자의 id를 반환 (고유한 값)
    @Override
    public String getUsername(){
        return email;
    }

    // 사용자의 패스워드 반환
    @Override
    public String getPassword(){
        return password;
    }

    // 계정 만료 여부 반환
    @Override
    public boolean isAccountNonExpired(){
        return true;    // 계정 만료되었는지 확인하는 로직 (true면 만료되지 않았음)
    }

    // 계정 잠금 여부 반환
    @Override
    public boolean isAccountNonLocked(){
        return true;    // 패스워드 만료됐는지 확인하는 로직
    }

    @Override
    public boolean isEnabled(){
        return true;    // 계정 사용 가능 확인하는 로직
    }



    // === OAuth2User 인터페이스 메소드 구현 ===
    @Override
    public Map<String, Object> getAttributes() {
        return this.attributes;
    }

    /**
     * OAuth2 제공자로부터 받은 사용자 정보 중 이름(사용자 식별자)에 해당하는 값을 반환합니다.
     * Naver는 'response' 객체 안에 'id'가 있고, Google은 'sub' 필드를 사용합니다.
     * nameAttributeKey 필드를 사용하여 이 키를 구분합니다.
     */
    @Override
    public String getName() {
        if (this.nameAttributeKey == null || this.attributes == null) {
            return null; // 또는 userNo.toString() 등 대체 식별자 반환 고려
        }
        // Naver 특정 처리 (userNameAttributeName이 'response'일 경우)
        if ("response".equals(this.nameAttributeKey) && this.attributes.containsKey("response")) {
            Object responseObj = this.attributes.get("response");

            if (responseObj instanceof Map) {
                @SuppressWarnings("unchecked") // 타입 캐스팅 경고 무시
                Map<String, Object> responseMap = (Map<String, Object>) responseObj;
                // Naver 응답에서 실제 ID는 'id' 필드에 있음
                return (String) responseMap.get("id");
            }
        }
        // Google 등 다른 일반적인 경우 (userNameAttributeName이 'sub' 등일 경우)
        return (String) this.attributes.get(this.nameAttributeKey);
    }

    /**
     * CustomOAuth2UserService에서 호출하여 OAuth 관련 정보를 User 객체에 설정
     * @param attributes OAuth 제공자로부터 받은 속성 맵
     * @param nameAttributeKey 사용자 이름(고유 식별자) 속성의 키
     * @return this (메소드 체이닝 가능)
     */
    public User oauthUpdate(Map<String, Object> attributes, String nameAttributeKey) {
        this.attributes = attributes;
        this.nameAttributeKey = nameAttributeKey;
        return this;
    }


}
