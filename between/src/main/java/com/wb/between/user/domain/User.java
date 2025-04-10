package com.wb.between.user.domain;

import com.wb.between.usercoupon.domain.UserCoupon;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
//@Table(name = "User") // 매핑할 테이블명 선언 (생략 시 클래스명을 테이블명으로 사용가능)
@Data
@Builder                // 빌더 패턴 클래스 생성
@NoArgsConstructor      // 인자가 없는 생성자 생성
@AllArgsConstructor     // 모든 필드를 인자로 받는 생성자 생성
@EqualsAndHashCode(exclude = "usercoupon") // 양방향 연관관계 시 순환 참조 방지 위해 추가 권장
public class User implements UserDetails {

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

}
