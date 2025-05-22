package com.wb.between.admin.dashboard.dto;

import com.wb.between.admin.reservation.dto.ReservationListDto;
import com.wb.between.admin.user.dto.UserListDto;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class DashboardResDto {

    //오늘 신규 예약 건수
    private long todayCount;

    //최근 예약 목록
    private List<ReservationListDto> reservationList;

    //최근 회원 목록
    private List<UserListDto> userList;

    //총 좌석 건수
    private long totalSeats;

    //현재 예약 건수
    private long occupiedSeats;

    //점유 좌석 비율
    private int occupancyRate;

    //예상 수익
    private BigDecimal revenue;

}
