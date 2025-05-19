package com.wb.between.admin.seat.dto;

import com.wb.between.admin.price.domain.Price;
import com.wb.between.admin.seat.domain.adminSeat;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class SeatResponseDto {
    private Long seatNo;
    private String seatNm;
    private Integer floor;
    private String seatSort;
    private String gridRow;
    private String gridColumn;
    private boolean useAt;

    // --- 가격 필드 (프론트 편의상 유지) ---
    private String hourlyPrice;
    private String dailyPrice;
    private String monthlyPrice;
    // ----------------------------------

    private String register;
    private LocalDateTime createDt;
    private LocalDateTime updateDt;

    public SeatResponseDto(adminSeat seat) {
        this.seatNo = seat.getSeatNo();
        this.seatNm = seat.getSeatNm();
        this.floor = seat.getFloor();
        this.seatSort = seat.getSeatSort();
        this.gridRow = seat.getGridRow();
        this.gridColumn = seat.getGridColumn();
        this.useAt = seat.isUseAt();
        this.register = seat.getRegister();
        this.createDt = seat.getCreateDt();
        this.updateDt = seat.getUpdateDt();

        //  prices 리스트에서 각 타입별 가격 추출
  /*      if (seat.getPrices() != null) {
            for (Price p : seat.getPrices()) {
                if ("HOURLY".equalsIgnoreCase(p.getType())) {
                    this.hourlyPrice = p.getPrice();
                } else if ("DAILY".equalsIgnoreCase(p.getType())) {
                    this.dailyPrice = p.getPrice();
                } else if ("MONTHLY".equalsIgnoreCase(p.getType())) {
                    this.monthlyPrice = p.getPrice();
                }
            }
        }*/

        System.out.println("--- SeatResponseDto 생성자 진입: seatNo=" + seat.getSeatNo() + " ---");
        if (seat.getPrices() != null && !seat.getPrices().isEmpty()) {
            System.out.println("  Prices 컬렉션 크기: " + seat.getPrices().size() + " records."); // <<<--- 로그 1
            for (Price p : seat.getPrices()) {
                // !!! 중요: p.getType()과 p.getPrice() 값이 로그에 어떻게 찍히는지 확인 !!!
                System.out.printf("    - Price 레코드: Type=[%s], PriceValue=[%s]%n", p.getType(), p.getPrice()); // <<<--- 로그 2
                if ("HOURLY".equalsIgnoreCase(p.getType()) || "H".equalsIgnoreCase(p.getType())) {
                    this.hourlyPrice = p.getPrice(); // DTO 필드가 String이면 그대로, Integer면 Integer.parseInt()
                    System.out.println("      >> hourlyPrice 설정: " + this.hourlyPrice); // <<<--- 로그 3
                } else if ("DAILY".equalsIgnoreCase(p.getType()) || "D".equalsIgnoreCase(p.getType())) {
                    this.dailyPrice = p.getPrice();
                    System.out.println("      >> dailyPrice 설정: " + this.dailyPrice); // <<<--- 로그 4
                } else if ("MONTHLY".equalsIgnoreCase(p.getType()) || "M".equalsIgnoreCase(p.getType())) {
                    this.monthlyPrice = p.getPrice();
                    System.out.println("      >> monthlyPrice 설정: " + this.monthlyPrice); // <<<--- 로그 5
                }
            }
        } else {
            System.out.println("  Prices 컬렉션이 null이거나 비어있음! (seatNo: " + seat.getSeatNo() + ")"); // <<<--- 로그 6
        }
        System.out.printf("  DTO 최종 가격: H=%s, D=%s, M=%s%n", this.hourlyPrice, this.dailyPrice, this.monthlyPrice); // <<<--- 로그 7
        System.out.println("--------------------------------------------------");

    }
}
