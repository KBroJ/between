package com.wb.between.user.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
//@Table(name = "User") // 매핑할 테이블명 선언 (생략 시 클래스명을 테이블명으로 사용가능)
@Data
@Builder                // 빌더 패턴 클래스 생성
@NoArgsConstructor      // 인자가 없는 생성자 생성
@AllArgsConstructor     // 모든 필드를 인자로 받는 생성자 생성
public class User {

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

    @UpdateTimestamp                                                            // 엔티티가 수정되어 저장될 때 시간이 자동 저장
    @Column(name = "updateDt")
    private LocalDateTime updateDt;

    @Column(name = "loginM", length = 10, nullable = false)
    private String loginM;
}
