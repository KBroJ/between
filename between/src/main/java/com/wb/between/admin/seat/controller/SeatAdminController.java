package com.wb.between.admin.seat.controller;

import com.wb.between.admin.seat.dto.SeatRequestDto;
import com.wb.between.admin.seat.dto.SeatResponseDto;
import com.wb.between.admin.seat.service.SeatAdminService;
import com.wb.between.admin.user.repository.AdminUserRepository;
import jakarta.persistence.EntityNotFoundException;

import java.net.URI;
import java.util.Map;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/admin/seats")
public class SeatAdminController {

    @Autowired
    private SeatAdminService seatAdminService;

    @Autowired
    private AdminUserRepository adminUserRepository;

    // 모든 좌석 목록 조회
    @GetMapping
    public ResponseEntity<List<SeatResponseDto>> getAllSeat(@RequestParam(required = false) Integer floor){
        System.out.println("[Controller] getAllSeats 요청 수신 - 요청된 층: " + floor);
        try {
            List<SeatResponseDto> seats = seatAdminService.getAllSeats(floor); // Service 호출 시 floor 전달
            if (seats.isEmpty() && floor != null) { // 특정 층 조회했는데 결과 없을 때
                System.out.println("해당 층(" + floor + ")에 좌석 없음");
                return ResponseEntity.ok(List.of()); // 빈 리스트와 200 OK
            }
            if (seats.isEmpty()) {
                return ResponseEntity.noContent().build(); // 204 No Content
            }
            return ResponseEntity.ok(seats);
        } catch (Exception e){
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    // 특정 ID의 좌석 상세 정보 조회
    @GetMapping("/{seatNo}")
    public ResponseEntity<?> getSeatById(@PathVariable Long seatNo){
        try{
            SeatResponseDto seatDto = seatAdminService.getSeatById(seatNo);
            return ResponseEntity.ok(seatDto);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            System.err.println("좌석(" + seatNo + ") 상세 조회 중 오류: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "좌석 조회 중 오류 발생"));
        }
    }

    @PostMapping
    public ResponseEntity<?> createSeat(  @Valid @RequestBody SeatRequestDto requestDto,
                                          BindingResult bindingResult,
                                          @AuthenticationPrincipal UserDetails adminDetails) {
        System.out.println("Controller가 받은 SeatRequestDto 내용:");
        System.out.println("  seatNm: " + requestDto.getSeatNm());
        System.out.println("  floor: " + requestDto.getFloor());
        System.out.println("  seatSort: " + requestDto.getSeatSort());
        System.out.println("  gridRow: " + requestDto.getGridRow());
        System.out.println("  gridColumn: " + requestDto.getGridColumn());
        System.out.println("  useAt: " + requestDto.isUseAt());
        if (requestDto.getPrices() != null) {
            System.out.println("  Prices 수: " + requestDto.getPrices().size());
        } else {
            System.out.println("  Prices: null");
        }


        if (bindingResult.hasErrors()) {
            Map<String, String> errors = bindingResult.getFieldErrors().stream()
                    .collect(Collectors.toMap(fe -> fe.getField(), fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "유효성 오류"));
            return ResponseEntity.badRequest().body(Map.of("success", false, "errors", errors));
        }

        if(adminDetails == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "관리자 로그인이 필요합니다."));
        }
        String registrantUsername = adminDetails.getUsername(); // 등록자 정보


        try {
            SeatResponseDto createdSeatDto = seatAdminService.createSeat(requestDto, registrantUsername);
            // 생성된 리소스의 URI를 Location 헤더에 포함
            URI location = ServletUriComponentsBuilder
                    .fromCurrentRequest().path("/{seatNo}")
                    .buildAndExpand(createdSeatDto.getSeatNo())
                    .toUri();
            return ResponseEntity.created(location).body(createdSeatDto); // 201 Created
        } catch (Exception e) {
            System.err.println("좌석 등록 중 오류: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "좌석 등록 중 오류 발생"));
        }
    }

    // 수정
    @PutMapping("/{seatNo}")
    public ResponseEntity<?> updateSeat(
            @PathVariable Long seatNo,
            @Valid @RequestBody SeatRequestDto requestDto,
            BindingResult bindingResult,
            @AuthenticationPrincipal UserDetails adminDetails){
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = bindingResult.getFieldErrors().stream().collect(Collectors.toMap(fe -> fe.getField(), fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "유효성 오류"));
            return ResponseEntity.badRequest().body(Map.of("success", false, "errors", errors));
        }

        if (adminDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "관리자 로그인이 필요합니다."));
        }
        String registrantUsername = adminDetails.getUsername();

        try {
            SeatResponseDto updatedSeatDto = seatAdminService.updateSeat(seatNo, requestDto, registrantUsername);
            return ResponseEntity.ok(updatedSeatDto); // 200 OK
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            System.err.println("좌석(" + seatNo + ") 수정 중 오류: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "좌석 수정 중 오류 발생"));
        }
    }

    // 삭제
    @DeleteMapping("/{seatNo}")
    public ResponseEntity<?> deleteSeat(@PathVariable Long seatNo,
                                        @AuthenticationPrincipal UserDetails adminDetails){
        if (adminDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "관리자 로그인이 필요합니다."));
        }
        try {
            seatAdminService.deleteSeat(seatNo);
            return ResponseEntity.ok(Map.of("success", true, "message", "좌석(ID: " + seatNo + ")이 성공적으로 삭제되었습니다."));
            // 또는 return ResponseEntity.noContent().build(); // 204 No Content
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", e.getMessage()));
        } catch (IllegalStateException e) { // Service에서 삭제 불가 조건 시 발생 가능
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            System.err.println("좌석(" + seatNo + ") 삭제 중 오류: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "좌석 삭제 중 오류 발생"));
        }


    }
}
