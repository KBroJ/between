package com.wb.between.admin.user.controller;

import com.wb.between.admin.user.dto.UserDetailDto;
import com.wb.between.admin.user.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequiredArgsConstructor // final 필드(AdminUserService)에 대한 생성자 자동 생성 및 주입
@RequestMapping("/admin")
public class AdminUserController {

    private final AdminUserService adminUserService; // 주입받은 서비스 객체

    // 회원 관리 - 회원 목록 페이지
    @GetMapping("/users")
    public String userManagement(Model model) {


        return "admin/user/users";
    }

    /**
     * 특정 사용자의 상세 정보 페이지를 조회하는 요청 처리
     * @param userNo URL 경로에서 추출한 사용자 ID
     * @param model View로 데이터를 전달할 모델 객체
     * @return 렌더링할 뷰의 이름
     */
    @GetMapping("/users/{userNo}") // 요청 경로 변경: /admin/users/{userNo} 형태
    public String showUserDetailPage(@PathVariable Long userNo, Model model) { // 메소드명 변경 및 파라미터 추가
        log.info("관리자 - 사용자 상세 정보 조회 요청. userNo={}", userNo);
        try {

            // userNo로 사용자 상세 정보 DTO 가져오기
            UserDetailDto userDetail = adminUserService.getUserDetail(userNo);
            log.info("userDetail={}", userDetail); // 로그 추가

            // 모델에 "user"라는 이름으로 DTO 추가
            model.addAttribute("user", userDetail);

            return "admin/user/user-detail";

        } catch (IllegalArgumentException e) {
            // 사용자를 찾을 수 없는 경우 에러 처리
            log.error("사용자 조회 실패: {}", e.getMessage());
            model.addAttribute("errorMessage", e.getMessage());
            return "error/404"; // 임시 에러 페이지
        } catch (Exception e) {
            // 기타 예외 처리
            log.error("사용자 상세 정보 조회 중 오류 발생", e);
            model.addAttribute("errorMessage", "사용자 정보를 불러오는 중 오류가 발생했습니다.");
            // TODO: 적절한 에러 페이지 경로 반환
            return "error/500"; // 임시 에러 페이지
        }
    }

    // TODO: 사용자 목록 페이지, API 엔드포인트(수정/삭제) 등 필요시 추가 구현


}
