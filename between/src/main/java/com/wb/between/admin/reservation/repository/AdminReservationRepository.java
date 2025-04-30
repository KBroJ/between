package com.wb.between.admin.reservation.repository;

import com.wb.between.reservation.reserve.domain.Reservation;
import com.wb.between.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AdminReservationRepository extends JpaRepository<Reservation, Long>, JpaSpecificationExecutor<Reservation> {

    /*
        @EntityGraph : 예약 목록 조회 시, 연관된 User와 Seat 엔티티를 지정해 함께 로딩하여
                       N+1 문제를 방지하기 위한 메소드
            주요 목적:
                지연 로딩(Lazy Loading)으로 인해 발생하는 N+1 쿼리 문제를 해결하는 것입니다.
                필요한 연관 엔티티 데이터를 처음 조회할 때 한 번의 쿼리(주로 JOIN 사용)로 함께 가져와, 이후 연관 엔티티에 접근할 때 추가 쿼리가 발생하는 것을 방지합니다.
            적용 위치:
                주로 Repository 인터페이스의 메소드에 적용합니다.
            attributePaths 속성:
                함께 로딩할 연관관계 필드의 이름을 문자열 배열로 지정합니다.
                예를 들어 Reservation 엔티티의 user 필드와 seat 필드를 함께 로딩하고 싶다면 attributePaths = {"user", "seat"} 와 같이 지정합니다.
    */
    @Override
    @EntityGraph(attributePaths = {"user", "seat"}) // 특정 조회 시 함께 가져올 연관 엔티티 속성들을 지정(user와 seat 필드를 Eager 로딩하도록 지정)
    Page<Reservation> findAll(Specification<Reservation> spec, Pageable pageable);

}
