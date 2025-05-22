package com.wb.between.user.repository;

import com.wb.between.user.domain.User;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String eamil);   // email로 사용자 정보 가지고 옴

    Optional<User> findByUserNo(Long userNo);   // userNo로 사용자 정보 가지고 옴

    // 이메일과 휴대번호로 사용자 정보 조회
    @Query("SELECT DISTINCT u FROM User u " +
            "LEFT JOIN FETCH u.userRole ur " + // User와 UserRole 조인 및 즉시 로딩
            "LEFT JOIN FETCH ur.role r " +       // UserRole과 Role 조인 및 즉시 로딩
            "LEFT JOIN FETCH r.rolePermissions rp " + // Role과 RolePermission 조인 및 즉시 로딩
            "LEFT JOIN FETCH rp.permission p " +   // RolePermission과 Permission 조인 및 즉시 로딩
            "WHERE u.email = :email " +
            "AND u.phoneNo = :phoneNo")
    Optional<User> findByEmailAndPhoneNo(@Param("email") String email, @Param("phoneNo") String phoneNo);

    // 방법1. User 엔티티에 선언된 변수명을 기반으로 메소드명 짓기
    boolean existsByEmail(String email);
    // 방법2. 메소드 생성 후 @Query 어노테이션을 이용하여 쿼리문 작성
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.email = :email")
    boolean checkEmail(@Param("email") String email);

    /*
     * 회원정보 찾기 > 이메일 찾기
     * 휴대폰 번호로 사용자를 조회합니다.
     * @param phoneNo '-'가 제거된 순수 숫자 형태의 휴대폰 번호
     * @return 사용자 정보 Optional 객체
     */
    Optional<User> findByPhoneNo(String phoneNo);

    // Fetch Join을 사용하여 연관된 모든 정보를 한 번에 조회
    @Query("SELECT DISTINCT u FROM User u " +
            "LEFT JOIN FETCH u.userRole ur " + // User와 UserRole 조인 및 즉시 로딩
            "LEFT JOIN FETCH ur.role r " +       // UserRole과 Role 조인 및 즉시 로딩
            "LEFT JOIN FETCH r.rolePermissions rp " + // Role과 RolePermission 조인 및 즉시 로딩
            "LEFT JOIN FETCH rp.permission p " +   // RolePermission과 Permission 조인 및 즉시 로딩
            "WHERE u.email = :email")
    Optional<User> findByUsernameWithRolesAndPermissions(@Param("email") String email);

    @Query("SELECT DISTINCT u FROM User u " +
            "LEFT JOIN FETCH u.userRole ur " + // User와 UserRole 조인 및 즉시 로딩
            "LEFT JOIN FETCH ur.role r " +       // UserRole과 Role 조인 및 즉시 로딩
            "LEFT JOIN FETCH r.rolePermissions rp " + // Role과 RolePermission 조인 및 즉시 로딩
            "LEFT JOIN FETCH rp.permission p " +   // RolePermission과 Permission 조인 및 즉시 로딩
            "WHERE u.userNo = :userNo")
    Optional<User>  findByIdWithAuthorities(@Param("userNo") Long userNo);

}
