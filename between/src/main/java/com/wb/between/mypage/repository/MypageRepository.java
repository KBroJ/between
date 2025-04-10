package com.wb.between.mypage.repository;

import com.wb.between.reservation.reserve.domain.Reservation;
import com.wb.between.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface MypageRepository extends JpaRepository<User, Long> {

    @Override
    Optional<User> findById(Long userNo);

}
