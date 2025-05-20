package com.wb.between.admin.seat.service;

import com.wb.between.admin.price.domain.Price;
import com.wb.between.admin.price.dto.PriceDto;
import com.wb.between.admin.price.repository.PriceRepository;
import com.wb.between.admin.seat.domain.adminSeat;
import com.wb.between.admin.seat.dto.SeatRequestDto;
import com.wb.between.admin.seat.dto.SeatResponseDto;
import com.wb.between.admin.seat.repository.adminSeatRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SeatAdminService {

    @Autowired
    private adminSeatRepository adminSeatRepository;

    @Autowired
    private PriceRepository priceRepository;

    // 좌석 등록
    @Transactional
    public SeatResponseDto createSeat(SeatRequestDto requestDto, String registrant) {

        adminSeat seat = new adminSeat();
        seat.setSeatNm(requestDto.getSeatNm());
        seat.setFloor(requestDto.getFloor());
        seat.setSeatSort(requestDto.getSeatSort());
        seat.setGridColumn(requestDto.getGridColumn());
        seat.setGridRow(requestDto.getGridRow());
        seat.setUseAt(requestDto.isUseAt());
        seat.setRegister(registrant);

        System.out.println("DB 저장 직전 Entity의 gridRow: " + seat.getGridRow());

        if(requestDto.getPrices() != null && !requestDto.getPrices().isEmpty()){
            for(PriceDto priceDto : requestDto.getPrices()){
                Price priceEntity = new Price(seat, priceDto.getType(), priceDto.getPrice());
                priceEntity.setType(priceDto.getType());
                priceEntity.setPrice(priceDto.getPrice());
                seat.addPrice(priceEntity);
            }
        }

        adminSeat saveSeat = adminSeatRepository.save(seat);

        return new SeatResponseDto(saveSeat);
    }
    
    // 모든 좌석 정보를 조회한다
    @Transactional(readOnly = true)
    public List<SeatResponseDto> getAllSeats(Integer floor){
        List<adminSeat> seats;
        if (floor != null && floor > 0) { // 특정 층 번호가 유효하게 넘어오면
            seats = adminSeatRepository.findByFloorAndUseAtTrueWithPrices(floor); // 해당 층의 활성 좌석만 조회
            System.out.println("[Service] seatRepository.findByFloorAndUseAtTrue(" + floor + ") 호출 예정");
        } else { // 층 번호가 없거나 유효하지 않으면 전체 활성 좌석 조회
            seats = adminSeatRepository.findByUseAtTrueWithPrices(); // 또는 findAll() 후 useAt 필터링
            System.out.println("[Service] seatRepository.findByUseAtTrue() (전체 층) 호출 예정");
        }
        System.out.println("[Service] Repository로부터 받은 좌석 수: " + seats.size());
        return seats.stream()
                .map(SeatResponseDto::new)
                .collect(Collectors.toList());
    }

    // 특정 좌석 상세 정보 조회
    @Transactional(readOnly = true)
    public SeatResponseDto getSeatById(Long seatNo){
        adminSeat seat = adminSeatRepository.findById(seatNo).orElseThrow(() -> new EntityNotFoundException("좌석을 찾을 수 없습니다. ID: " + seatNo));
        return new SeatResponseDto(seat);
    }

    // 좌석 정보 수정
    @Transactional
    public SeatResponseDto updateSeat(Long SeatNo, SeatRequestDto requestDto, String registrant){
        adminSeat seat = adminSeatRepository.findById(SeatNo)
                .orElseThrow(() -> new EntityNotFoundException("수정할 좌석을 찾을 수 없습니다. ID: " + SeatNo));

        seat.setSeatNm(requestDto.getSeatNm());
        seat.setFloor(requestDto.getFloor());
        seat.setSeatSort(requestDto.getSeatSort());
        seat.setGridRow(requestDto.getGridRow());
        seat.setGridColumn(requestDto.getGridColumn());
        seat.setUseAt(requestDto.isUseAt());
        seat.setRegister(registrant);

        seat.clearPrices();

        if(requestDto.getPrices() != null && !requestDto.getPrices().isEmpty()){
            for(PriceDto priceDto : requestDto.getPrices()){
                Price priceEntity = new Price(seat, priceDto.getType(), priceDto.getPrice());
                priceEntity.setType(priceDto.getType());
                priceEntity.setPrice(priceDto.getPrice());
                seat.addPrice(priceEntity);
            }
        }

        adminSeat updateSeat = adminSeatRepository.save(seat);

        return new SeatResponseDto(updateSeat);
    }
    
    // 좌석 삭제
    @Transactional
    public void deleteSeat(Long SeatNo){
        if (!adminSeatRepository.existsById(SeatNo)) {
            throw new EntityNotFoundException("삭제할 좌석을 찾을 수 없습니다. ID: " + SeatNo);
        }
        adminSeatRepository.deleteById(SeatNo);
    }

    /**
     * 전체 좌석 수 조회
     */
    @Transactional(readOnly = true)
    public long countByUseAt(){
        LocalDateTime now = LocalDateTime.now();

        //1. 전체 운영 가능한 좌석 수
        long totalSeat = adminSeatRepository.countByUseAt(true);
      
        return totalSeat;
    }
}
