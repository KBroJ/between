package com.wb.between.reservation.seat.dto;

import com.wb.between.admin.price.domain.Price;
import com.wb.between.reservation.seat.domain.Seat;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class SeatResponseDto {

    private String id;
    private String name;
    private Integer floor;
    private String type;
    private String status;
    private String gridRow;
    private String gridColumn;
    private boolean useAt;

    // --- 가격 정보 필드 (Price Entity 리스트에서 추출) ---
    private String hourlyPrice;  // 시간당 가격 (문자열)
    private String dailyPrice;   // 일일권 가격 (문자열)
    private String monthlyPrice; // 월정액권 가격 (문자열)

    public SeatResponseDto(Seat seat, String calculatedStatus) {
        this.id = String.valueOf(seat.getSeatNo());
        this.name = seat.getSeatNm();
        this.floor = seat.getFloor();
        this.type = mapSeatSortToType(seat.getSeatSort()); // SeatService의 헬퍼 메소드 재사용 또는 여기에 구현
        this.status = calculatedStatus; // 서비스에서 계산된 상태 직접 사용
        this.gridRow = seat.getGridRow();
        this.gridColumn = seat.getGridColumn();
        this.useAt = seat.isUseAt(); // 보통 서비스에서 useAt=true인 것만 가져옴

        // Price 리스트에서 가격 정보 추출하여 설정
        extractPrices(seat.getPrices());
    }

    private String mapSeatSortToType(String seatSort) {
        if (seatSort == null) return "SEAT"; // 기본값
        switch (seatSort) {
            case "개인": return "SEAT";
            case "회의실": return "ROOM";
            case "AREA": return "AREA";
            default: return seatSort; // 알 수 없는 타입은 그대로
        }
    }

    private void extractPrices(List<Price> prices) {
        if (prices != null && !prices.isEmpty()) {
            for (Price p : prices) {
                if (p.getType() == null || p.getPrice() == null) continue; // 타입이나 가격 없으면 건너뜀

                // !!! Price Entity의 type 필드 값과 정확히 일치해야 함 !!!
                if ("HOURLY".equalsIgnoreCase(p.getType()) || "H".equalsIgnoreCase(p.getType())) {
                    this.hourlyPrice = p.getPrice();
                } else if ("DAILY".equalsIgnoreCase(p.getType()) || "D".equalsIgnoreCase(p.getType())) {
                    this.dailyPrice = p.getPrice();
                } else if ("MONTHLY".equalsIgnoreCase(p.getType()) || "M".equalsIgnoreCase(p.getType())) {
                    this.monthlyPrice = p.getPrice();
                }
            }
        }
        // 가격 정보가 없는 경우 해당 DTO 필드는 null로 유지됨
    }



}
