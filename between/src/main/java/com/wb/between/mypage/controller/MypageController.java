package com.wb.between.mypage.controller;

import com.wb.between.common.exception.CustomException;
import com.wb.between.mypage.dto.*;
import com.wb.between.mypage.service.MyReservationService;
import com.wb.between.mypage.service.MypageService;
import com.wb.between.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

@Slf4j
@Controller
@RequestMapping("/mypage")
@RequiredArgsConstructor
public class MypageController {

    private final MypageService mypageService;
    private final MyReservationService myReservationService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");


    /**
     * 마이페이지 조회
     */
    @GetMapping
    public String mypage(
            //  @AuthenticationPrincipal : SecurityContext에 저장된 Authentication 객체에서 Principal 객체를 꺼내어 지정된 타입(여기서는 User)으로 변환하여 주
            @AuthenticationPrincipal User user,
            Model model
    ) {

        log.debug("user = {}", user);
        MypageResponseDto mypageResponseDto = mypageService.findUserbyId(user.getUserNo());

        model.addAttribute("userInfo", mypageResponseDto);

        return "mypage/dashboard";
    }

    /**
     * 마이페이지 > 정보 수정 조회
     */
    @GetMapping("/edit")
    public String editProfilePage(@AuthenticationPrincipal User user, Model model) {

        log.debug("user = {}", user);
        MypageResponseDto mypageResponseDto = mypageService.findUserbyId(user.getUserNo());
        log.debug("mypageResponseDto = {}", mypageResponseDto);
        log.debug("mypageResponseDto.getEmail = {}", mypageResponseDto.getEmail());

        model.addAttribute("userInfo", mypageResponseDto);

        return "mypage/edit-profile";
    }

    /**
     * 정보 수정 처리
     */
    @PutMapping("/edit")
    public String editProfile(@AuthenticationPrincipal User user,
                              @ModelAttribute("userInfo") UserInfoEditReqDto userInfoEditReqDto,
                              Model model) {

        try {
            //정보 수정
            MypageResponseDto mypageResponseDto = mypageService.updateUserInfo(user.getUserNo(), userInfoEditReqDto);
            model.addAttribute("userInfo", mypageResponseDto);

            return "redirect:/mypage/edit";
        } catch (CustomException ex) {
            log.error("error = {}", ex.getMessage());
            return "mypage/edit-profile";
        } catch (Exception e) {
            // 예상치 못한 다른 종류의 예외 처리
            log.error("예상치 못한 오류 발생", e);
            return "mypage/edit-profile";
        }

    }

    /**
     * 마이페이지 > 비밀번호 수정 조회
     */
    @GetMapping("/editPassword")
    public String editPasswordPage(@AuthenticationPrincipal User user,
                                 Model model) {
        MypageResponseDto mypageResponseDto = mypageService.findUserbyId(user.getUserNo());
        model.addAttribute("userInfo", mypageResponseDto);
        return "mypage/edit-password";
    }

    /**
     * 비밀번호 수정 처리
     */
    @PutMapping("/editPassword")
    public String editPassword(@AuthenticationPrincipal User user,
                                 @RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 Model model) {
        log.debug("currentPassword = {}", currentPassword);
        log.debug("newPassword = {}", newPassword);

        try {
             mypageService.changePassword(user.getUserNo(),
                    currentPassword,
                    newPassword);
            model.addAttribute("result", "success");
            return "redirect:/mypage";

        } catch (CustomException ex) {
            log.error("changePassword|error = {}", ex.getMessage());
            return "mypage/edit-password";
        } catch (RuntimeException e) {
            // 예상치 못한 다른 종류의 예외 처리
            log.error("예상치 못한 오류 발생", e);
            return "mypage/edit-password";
        }
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
    public String resignCheckPassword(@AuthenticationPrincipal User user,
                                      @RequestParam String currentPassword,
                                      Model model) {

        try {
            //비밀번호 확인 요청
            mypageService.resignCheckPassword(user.getUserNo(), currentPassword);
            return "mypage/resign";
        } catch (CustomException ex) {
            //비밀번호 불일치시 리다이렉트
            return "redirect:/mypage/confirmResign";
        } catch (RuntimeException e) {
            // 예상치 못한 다른 종류의 예외 처리
            log.error("예상치 못한 오류 발생", e);
            return "redirect:/mypage/confirmResign";
        }
      }

    /**
     * 마이페이지 > 쿠폰 목록
     */
    @GetMapping("/coupon")
    public String coupon(@AuthenticationPrincipal User user,Model model) {

        List<MypageCouponResDto> userCouponList = mypageService.findCouponListById(user.getUserNo());
        model.addAttribute("userCouponList", userCouponList);
        model.addAttribute("couponCount", userCouponList.size());
        return "/mypage/coupon";
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
        String currentStartDate = StringUtils.hasText(startDate) ? startDate : LocalDate.now().minusMonths(3).format(DATE_FORMATTER);
        String currentEndDate = StringUtils.hasText(endDate) ? endDate : LocalDate.now().format(DATE_FORMATTER);


    // 페이징 정보 생성 (기본값: page:0 = 첫번째 페이지, 페이지당 예약내역 10개 표시, 예약일(resDt) 내림차순)
        Pageable pageable = PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "resDt"));


        try {

    // 예약 내역 조회
            Page<MyReservationDto> reservationsPage = myReservationService.findMyReservations(
                    user.getUserNo(), // 현재 사용자 번호
                    tab,              // 선택된 탭
                    currentStartDate, // 조회 시작일
                    currentEndDate,   // 조회 종료일
                    pageable          // 페이징 및 정렬 정보
            );

    // View(HTML)에 필요한 데이터 세팅
            model.addAttribute("currentTab", tab);                    // 현재 활성 탭 유지
            model.addAttribute("currentStartDate", currentStartDate); // 현재 시작 날짜 필터 값 유지
            model.addAttribute("currentEndDate", currentEndDate);     // 현재 종료 날짜 필터 값 유지
            model.addAttribute("reservationsPage", reservationsPage); // 실제 조회된 Page 객체 전달

            log.info("Successfully fetched {} reservations for user {}, tab {}",
                    reservationsPage.getTotalElements(), user.getEmail(), tab);

        } catch (CustomException e) {
            // 예상된 비즈니스 오류 처리
            log.error("Error fetching reservations for user {}: {}", user.getEmail(), e.getMessage());

            model.addAttribute("errorMessage", "예약 내역 조회 중 오류가 발생했습니다: " + e.getMessage());
            model.addAttribute("reservationsPage", Page.empty(pageable)); // 오류 시 빈 페이지 전달

            // 필터 유지를 위해 다른 속성들도 추가
            model.addAttribute("currentTab", tab);
            model.addAttribute("currentStartDate", currentStartDate);
            model.addAttribute("currentEndDate", currentEndDate);

        } catch (Exception e) {
            // 예상치 못한 서버 오류 처리
            log.error("Unexpected error fetching reservations for user {}: {}", user.getEmail(), e.getMessage(), e);

            model.addAttribute("errorMessage", "예상치 못한 오류가 발생했습니다. 관리자에게 문의하세요.");
            model.addAttribute("reservationsPage", Page.empty(pageable)); // 오류 시 빈 페이지 전달
            model.addAttribute("currentTab", tab);
            model.addAttribute("currentStartDate", currentStartDate);
            model.addAttribute("currentEndDate", currentEndDate);
        }

        return "mypage/my-reservations";
    }



    /**
     * 마이페이지 > 예약내역 > 예약 상세 내역 조회
     * @param resNo 조회할 예약 번호 (경로 변수)
     * @param user 현재 사용자
     * @param model View 에 전달할 모델
     * @return 상세 페이지 템플릿 경로
     */
    @GetMapping("/reservations/detail/{resNo}")
    public String reservationDetail(
            @AuthenticationPrincipal User user,
            @PathVariable("resNo") Long resNo, // 경로 변수 {resNo} 값 받기
            Model model
    ) {
        log.debug("MyPageController|reservationDetail|START ================> resNo: {}, user: {}", resNo, user.getEmail());

        try {

            // Service 호출하여 예약 상세 정보 DTO 조회
            MyReservationDetailDto reservationDetail = myReservationService.findMyReservationDetail(user.getUserNo(), resNo);

            if (reservationDetail == null) {
                log.error("Reservation not found or access denied for resNo: {}", resNo);
                return "redirect:/mypage/reservations"; // 목록으로 리다이렉트
            }

            // 모델에 DTO 추가
            model.addAttribute("reservationDetail", reservationDetail);

            // 상세 페이지 템플릿 반환
            return "mypage/my-reservation-detail";

        } catch (CustomException e) {
            // 서비스에서 예약 정보를 찾지 못하거나 권한이 없을 경우 예외 처리
            log.error("Error fetching reservation detail: {}", e.getMessage());


            return "redirect:/mypage/reservations"; // 목록 페이지로 리다이렉트
        } catch (Exception e) {

            // 기타 예상치 못한 예외 처리
            log.error("Unexpected error fetching reservation detail for resNo: {}", resNo, e);

            return "error/500";
        }
    }


}
