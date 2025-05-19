package com.wb.between.admin.seat.dto;

import com.wb.between.admin.price.dto.PriceDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class SeatRequestDto {

    @NotBlank(message = "좌석 이름은 필수입니다.")
    @Size(max = 100, message = "좌석 이름은 100자를 초과할 수 없습니다.")
    private String seatNm;

    @NotNull(message = "층 정보는 필수입니다.")
    private Integer floor;

    @NotBlank(message = "좌석 종류는 필수입니다.")
    @Size(max = 50, message = "좌석 종류는 50자를 초과할 수 없습니다.")
    private String seatSort;


    @Size(max = 50, message = "그리드 행 정보는 50자를 초과할 수 없습니다.")
    private String gridRow;

    @Size(max = 50, message = "그리드 열 정보는 50자를 초과할 수 없습니다.")
    private String gridColumn;

    private boolean useAt = true; // 기본값 true

    @Valid // PriceDto 내부의 유효성 검사도 함께 실행
    @NotNull
    @Size(min = 0, max = 3, message = "가격 정보는 최대 3개(시간/일/월)까지 입력 가능합니다.")
    private List<PriceDto> prices;


}
