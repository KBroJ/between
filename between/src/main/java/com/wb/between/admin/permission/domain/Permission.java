package com.wb.between.admin.permission.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "permission")
@EqualsAndHashCode(of = "permissionId")
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long permissionId;

    @Column(unique = true, nullable = false, length = 100)
    private String permissionCode; // 예: COUPON:CREATE

    @Column(nullable = false, length = 100)
    private String permissionName; // 예: 쿠폰 생성

    private String description;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createDt = LocalDateTime.now();

    @Column
    private LocalDateTime updateDt;

    // Role과의 관계는 Role 쪽에서 @JoinTable로 관리하므로 여기서는 명시적 매핑 불필요 (단방향 ManyToMany)
    // 양방향을 원하면 @ManyToMany(mappedBy = "permissions") 추가 가능
}
