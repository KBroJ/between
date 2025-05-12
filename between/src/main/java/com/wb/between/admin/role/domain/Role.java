package com.wb.between.admin.role.domain;

import com.wb.between.admin.rolepermission.domain.RolePermission;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 위한 기본 생성자 + 무분별한 생성 방지
@AllArgsConstructor // 빌더 사용 위해 추가 (혹은 Builder 직접 정의)
@Builder // 빌더 패턴 사용
@Table(name = "role")
@ToString(exclude = {"rolePermissions"}) // 순환 참조 방지
@EqualsAndHashCode(of = "roleId") // ID 기반 동등성 비교
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long roleId;

    @Column(unique = true, nullable = false, length = 50)
    private String roleCode;

    @Column(nullable = false, length = 100)
    private String roleName;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createDt;

    @Column(nullable = false)
    private LocalDateTime updateDt;

    // Role과 RolePermission의 일대다(1:N) 관계
    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default // 빌더 사용 시 초기화 보장
    private Set<RolePermission> rolePermissions = new HashSet<>();

}
