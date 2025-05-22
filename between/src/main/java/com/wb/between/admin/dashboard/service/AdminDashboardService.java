package com.wb.between.admin.dashboard.service;

import com.wb.between.admin.dashboard.dto.DashboardResDto;
import com.wb.between.admin.reservation.dto.ReservationListDto;
import com.wb.between.admin.reservation.service.AdminReservationService;
import com.wb.between.admin.seat.service.SeatAdminService;
import com.wb.between.admin.user.dto.UserListDto;
import com.wb.between.admin.user.service.AdminUserService;
import com.wb.between.coupon.service.CouponService;
import com.wb.between.reservation.reserve.service.ReservationService;
import com.wb.between.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    //예약서비스
    private final AdminReservationService adminReservationService;

    //좌석서비스
    private final SeatAdminService seatAdminService;

    //회원서비스
    private final AdminUserService adminUserService;

    //쿠폰서비스
    private final CouponService couponService;

    /**
     * 대시보드 데이터
     * @return
     */
    public DashboardResDto getDashboardData() {

        //총 좌석 건수
        long totalSeats = seatAdminService.countByUseAt();

        //현재 예약 건수
        long occupiedSeats = adminReservationService.countReservationNow();

        //현재 예약 건수
        long availableSeats = totalSeats - occupiedSeats;
        if (availableSeats < 0) availableSeats = 0; // 혹시 모를 계산 오류 방지

        //비율 계산
        double occupancyRateDouble = (totalSeats == 0) ? 0.0 : ((double) occupiedSeats / totalSeats) * 100.0;
        int occupancyRate = (int) Math.round(occupancyRateDouble);

        //오늘 예약 건수
        long todayCount = adminReservationService.countReservationByResDt();

        //오늘 수익
        BigDecimal revenue = adminReservationService.revenueByToday();


        //최근 5개 예약목록
        List<ReservationListDto> reservationList = adminReservationService.dashboardReservation();

        //최근 회원 목록
        List<UserListDto> userList = adminUserService.dashboadUser();


        return DashboardResDto.builder()
                .todayCount(todayCount)
                .reservationList(reservationList)
                .userList(userList)
                .totalSeats(totalSeats)
                .occupiedSeats(occupiedSeats)
                .occupancyRate(occupancyRate)
                .revenue(revenue)
                .build();
    }

}
