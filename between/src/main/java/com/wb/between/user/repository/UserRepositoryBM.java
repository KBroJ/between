package com.wb.between.user.repository;

import com.wb.between.user.domain.User;
import com.wb.between.user.domain.UserBM;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepositoryBM extends JpaRepository<User, Long> {
    Optional<UserBM> findByEmail(String eamil);   // email로 사용자 정보 가지고 옴
}
