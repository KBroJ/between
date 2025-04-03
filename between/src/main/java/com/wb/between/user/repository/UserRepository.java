package com.wb.between.user.repository;

import com.wb.between.user.domain.User;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String eamil);   // email로 사용자 정보 가지고 옴

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

}
