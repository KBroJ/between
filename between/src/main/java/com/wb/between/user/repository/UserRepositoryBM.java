package com.wb.between.user.repository;

import com.wb.between.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String eamil);   // email로 사용자 정보 가지고 옴
}
