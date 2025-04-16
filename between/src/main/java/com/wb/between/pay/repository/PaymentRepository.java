package com.wb.between.pay.repository;

import com.wb.between.pay.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, String> {
    Optional<Payment> findByResNo(Long resNo); // 예약 번호로 결제 정보 찾기
}
