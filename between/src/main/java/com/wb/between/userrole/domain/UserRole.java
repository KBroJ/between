package com.wb.between.userrole.domain;

import com.wb.between.admin.role.domain.Role;
import com.wb.between.user.domain.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "userRoleId")
@Table(name = "userrole")
public class UserRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userRoleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userNo", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roleId", nullable = false)
    private Role role;

    @CreationTimestamp
    @Column(name = "createDt",
            nullable = false,
            updatable = false,
            insertable = false)    // ← INSERT 시 컬럼 제외
    private LocalDateTime createDt;

}
