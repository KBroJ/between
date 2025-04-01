package com.wb.between.user.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Table(name = "User")
@NoArgsConstructor(access =  AccessLevel.PROTECTED)
@Getter
@Entity
public class UserBM implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "userNo", updatable = false)
    private Long userNo;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password")
    private String password;

    @Column(name = "name")
    private String name;

    @Column(name = "phoneNo")
    private String phoneNo;

    @Column(name = "userStts")
    private String userStts;

    @Column(name = "authCd")
    private String authCd;

    @CreatedDate
    @Column(name = "createDt")
    private LocalDateTime createDt;

    @LastModifiedDate
    @Column(name = "updateDt")
    private LocalDateTime updateDt;

    @Column(name = "loginM")
    private String loginM;

    @Builder
    public UserBM(String email, String password){
        this.email = email;
        this.password = password;
    }

    @Override   // 권한
    public Collection<? extends GrantedAuthority> getAuthorities(){
        return List.of(new SimpleGrantedAuthority("user"));
    }

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
