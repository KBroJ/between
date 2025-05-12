package com.wb.between.menurole;

import com.wb.between.admin.role.domain.Role;
import com.wb.between.menu.domain.Menu;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "menurole")
@Getter
@Setter
public class MenuRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long menuRoleId;

    // Menu 엔티티와의 다대일(N:1) 관계
    @ManyToOne(fetch = FetchType.LAZY) // 지연 로딩 권장
    @JoinColumn(name = "menuNo", nullable = false) // DB 컬럼명 명시
    private Menu menu;

    // Role 엔티티와의 다대일(N:1) 관계
    @ManyToOne(fetch = FetchType.LAZY) // 지연 로딩 권장
    @JoinColumn(name = "roleId", nullable = false) // DB 컬럼명 명시
    private Role role; // Role 엔티티 클래스가 정의되어 있어야 함

    @CreationTimestamp // JPA/Hibernate가 INSERT 시 자동으로 현재 시간 설정
    @Column(nullable = false, updatable = false) // DDL에 맞게 nullable=false, 업데이트 불가 설정
    private LocalDateTime createDt; // 타입 LocalDateTime으로 변경 (Timestamp 매핑)

}
