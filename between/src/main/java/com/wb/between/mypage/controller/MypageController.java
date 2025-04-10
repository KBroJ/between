package com.wb.between.mypage.controller;

import com.wb.between.mypage.dto.MypageResponseDto;
import com.wb.between.mypage.service.MypageService;
import com.wb.between.mypage.dto.MyReservationDto;
import com.wb.between.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/mypage")
@RequiredArgsConstructor
public class MypageController {

    private final MypageService mypageService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");


    /**
     * 마이페이지 조회
     */
    @GetMapping
    public String mypage(@AuthenticationPrincipal User user, Model model) {

        log.debug("user = {}", user);
        MypageResponseDto mypageResponseDto = mypageService.findUserbyId(user.getUserNo());

        model.addAttribute("userInfo", mypageResponseDto);

        return "mypage/dashboard";
    }

    /**
     * 마이페이지 > 정보 수정 조회
     */
    @GetMapping("/edit")
    public String editProfile(@AuthenticationPrincipal User user, Model model) {

        log.debug("user = {}", user);
        MypageResponseDto mypageResponseDto = mypageService.findUserbyId(user.getUserNo());
        log.debug("mypageResponseDto = {}", mypageResponseDto);
        log.debug("mypageResponseDto.getEmail = {}", mypageResponseDto.getEmail());

        model.addAttribute("userInfo", mypageResponseDto);

        return "mypage/edit-profile";
    }

    /**
     * 마이페이지 > 비밀번호 수정 조회
     */
    @GetMapping("/changePassword")
    public String changePasswordPage(@AuthenticationPrincipal User user,

                                 Model model) {
        MypageResponseDto mypageResponseDto = mypageService.findUserbyId(user.getUserNo());
        model.addAttribute("userInfo", mypageResponseDto);
        return "mypage/change-password";
    }

    /**
     * 비밀번호 수정 처리
     */
    @PostMapping("/changePassword")
    public String changePassword(@AuthenticationPrincipal User user,
                                 @RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 Model model) {
        log.debug("currentPassword = {}", currentPassword);
        log.debug("newPassword = {}", newPassword);
        MypageResponseDto mypageResponseDto = mypageService.findUserbyId(user.getUserNo());
        model.addAttribute("userInfo", mypageResponseDto);
        return "redirect:/mypage";
    }

    /**
     * 마이페이지 > 탈퇴 비밀번호 확인 화면 이동
     * @param model
     * @return
     */
    @GetMapping("/confirmResign")
    public String confirmResign(Model model) {
        return "mypage/confirmResign";
    }

    /**
     * 탈퇴 비밀번호 확인 처리
     * @param currentPassword
     * @param model
     * @return
     */
    @PostMapping("/resignCheckPassword")
    public String resignCheckPassword(@RequestParam String currentPassword,Model model) {
        return "confirmResign";
    }

    /**
     * 탈퇴 처리
     * @param currentPassword
     * @param model
     * @return
     */
    @GetMapping("/resign")
    public String resign(@RequestParam String currentPassword,Model model) {
        return "/";
    }

    /**
     * 마이페이지 > 예약 내역 조회
     */
    @GetMapping("/reservations")
    public String reservations(
            @AuthenticationPrincipal User user,                                     //  @AuthenticationPrincipal 어노테이션을 사용하여 현재 로그인한 사용자 정보를 가져옵니다.
            @RequestParam(name = "tab", defaultValue = "upcoming") String tab,      // 예약 내역 탭 (upcoming, past)
            @RequestParam(name = "startDate", required = false) String startDate,   // 예약필터 시작일
            @RequestParam(name = "endDate", required = false) String endDate,       // 예약필터 종료일
            @RequestParam(name = "page", defaultValue = "0") int page,              // 페이지 번호
            Model model
    ) {

        log.debug("MyPageController|reservations|START ================> : {}, tab: {}, startDate: {}, endDate: {}, page: {}",
                   user.getEmail(), tab, startDate, endDate, page);

    // 날짜 필터 기본값 설정
        String currentStartDate;
        String currentEndDate;

        // startDate 이 비어있거나 null 이면 3개월 전 날짜로 설정
        if (!StringUtils.hasText(startDate)) {
            currentStartDate = LocalDate.now().minusMonths(3).format(DATE_FORMATTER);
        } else {
            currentStartDate = startDate;
        }

        // endDate 이 비어있거나 null 이면 오늘 날짜로 설정
        if (!StringUtils.hasText(endDate)) {
            currentEndDate = LocalDate.now().format(DATE_FORMATTER);
        } else {
            currentEndDate = endDate;
        }


        // --- !!! 중요: 실제로는 ReservationService를 통해 데이터를 조회해야 함 !!! ---
        // --- 여기서는 화면 확인을 위한 Mock 데이터 생성 ---

        // 1. 페이징 정보 생성 (기본값: page:0 = 첫번째 페이지, 페이지당 예약내역 10개 표시, 예약일(resDt) 내림차순)
        Pageable pageable = PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "resDt"));

//  Mock 데이터 생성 =================================================================================================
        // 2. 가짜(Mock) 예약 데이터 리스트 생성 (실제로는 DB 조회 결과)
        List<MyReservationDto> mockList = new ArrayList<>();

        // 예시 데이터 (탭이나 필터와 무관하게 몇 개만 생성)
        mockList.add(new MyReservationDto(
                        1L,                                             // resNo : 예약 번호
                        LocalDateTime.now().minusDays(1),                     // resDt : 예약일자
                        "개인석 A-1",                                          // seatNm : 좌석 이름 (Seat 엔티티와 조인해서 가져와야 함)
                        LocalDateTime.now().plusDays(1),                      // resStart : 예약 시작일자
                        LocalDateTime.now().plusDays(1).plusHours(3),         // resEnd : 예약 종료일자
                        "6000",                                               // totalPrice : 총 결제 금액
                        "6000",                                               // resPrice : 좌석 예약 비용
                        "0",                                                  // dcPrice : 할인 비용
                        "1"                                                   // resStatus : 예약상태
                )
        );
        mockList.add(new MyReservationDto(2L,LocalDateTime.now().minusDays(2),"회의실 B (4인)",LocalDateTime.now().plusDays(1),LocalDateTime.now().plusDays(1).plusHours(2), "4000","4000","0","2"));
        mockList.add(new MyReservationDto(3L,LocalDateTime.now().minusDays(3),"개인석 C-5",LocalDateTime.now().plusDays(1),LocalDateTime.now().plusDays(1).plusHours(1), "2000","2000","0","3"));

        // 3. Page 객체 생성 (Mock 데이터와 페이징 정보 사용)
        // new PageImpl<>(내용 리스트, 요청한 Pageable, 전체 데이터 개수)
        // 실제 구현 시에는 Service에서 반환된 Page 객체를 그대로 사용
        Page<MyReservationDto> reservationsPage = new PageImpl<>(mockList, pageable, mockList.size());

// Mock 데이터 생성 끝 ===============================================================================================

        // 모델에 필요한 데이터 추가
        model.addAttribute("currentTab", tab);                   // 현재 활성 탭
        model.addAttribute("currentStartDate", currentStartDate);       // 현재 시작 날짜 필터 값
        model.addAttribute("currentEndDate", currentEndDate);         // 현재 종료 날짜 필터 값
        model.addAttribute("reservationsPage", reservationsPage); // 페이징된 예약 목록

        return "mypage/my-reservations";
    }

}
