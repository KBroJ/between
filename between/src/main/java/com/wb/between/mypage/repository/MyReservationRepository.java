package com.wb.between.mypage.repository;

import com.wb.between.reservation.reserve.domain.Reservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface MyReservationRepository extends JpaRepository<Reservation, Long>, JpaSpecificationExecutor<Reservation> {

    // Specification을 사용하는 findAll 메서드에 @EntityGraph 적용
    // attributePaths 에 함께 로딩할 연관 필드명을 명시 ("seat"은 Reservation 엔티티의 Seat 필드명)
    @Override
    @EntityGraph(attributePaths = {"seat"}) // Seat 정보 Eager 로딩(즉시로딩) 설정
    Page<Reservation> findAll(org.springframework.data.jpa.domain.Specification<Reservation> spec, Pageable pageable);

    // 필요하다면 다른 기본 JpaRepository 메서드들을 오버라이드 하거나 추가 쿼리 메서드를 정의할 수 있습니다.


}
