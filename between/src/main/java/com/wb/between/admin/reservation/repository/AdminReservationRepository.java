package com.wb.between.admin.reservation.repository;

import com.wb.between.reservation.reserve.domain.Reservation;
import com.wb.between.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AdminReservationRepository extends JpaRepository<Reservation, Long>, JpaSpecificationExecutor<Reservation> {

}
