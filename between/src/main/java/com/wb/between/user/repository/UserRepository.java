package com.wb.between.user.repository;

import com.wb.between.user.domain.User;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // 방법1. User 엔티티에 선언된 변수명을 기반으로 메소드명 짓기
    boolean existsByEmail(String email);

    // 방법2. 메소드 생성 후 @Query 어노테이션을 이용하여 쿼리문 작성
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.email = :email")
    boolean checkEmail(@Param("email") String email);

}
