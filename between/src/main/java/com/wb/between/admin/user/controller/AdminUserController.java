package com.wb.between.admin.user.controller;

import com.wb.between.admin.user.dto.UserDetailDto;
import com.wb.between.admin.user.dto.UserFilterParamsDto;
import com.wb.between.admin.user.dto.UserListDto;
import com.wb.between.admin.user.service.AdminUserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor // final 필드(AdminUserService)에 대한 생성자 자동 생성 및 주입
@RequestMapping("/admin")
public class AdminUserController {

    private final AdminUserService adminUserService; // 주입받은 서비스 객체

    /**
     * 회원 관리 목록 페이지 요청 처리 (검색 조건 및 페이징 포함)
     * @param filterParams 검색 조건 DTO (@ModelAttribute 를 통해 HTTP 파라미터를 객체에 바인딩)
     * @param pageable 페이징/정렬 정보 (@PageableDefault 로 기본값 설정)
     * @param model View 로 전달할 데이터 모델
     * @return 렌더링할 뷰의 논리적 이름
     */
    // 회원 관리 - 회원 목록 페이지
    @GetMapping("/userList")
    public String userListPage(@ModelAttribute UserFilterParamsDto filterParams,
                                 @PageableDefault(size = 10, sort = "createDt", direction = Sort.Direction.DESC) Pageable pageable, // 페이징 기본값 설정
                                 Model model) {
        log.info("AdminUserController|userListPage|관리자 - 사용자 목록 조회 요청 시작 ==================");
        log.info("AdminUserController|userListPage|=========> 1. filterParams: {}", filterParams);
        log.info("AdminUserController|userListPage|=========> 2. Pageable: {}", pageable);
        log.info("AdminUserController|userListPage|관리자 - 사용자 목록 조회 요청 끝   ==================");

        // 서비스 호출하여 필터링/페이징된 사용자 목록 조회
        Page<UserListDto> userPage = adminUserService.getUsers(filterParams, pageable);

        // Model에 데이터 추가 -> Thymeleaf 템플릿에서 사용 가능
        model.addAttribute("userPage", userPage);           // 페이징된 사용자 목록 데이터
        model.addAttribute("filterParams", filterParams);   // 검색 조건 유지를 위해 전달

        log.info("조회 완료. 총 페이지: {}, 총 회원 수: {}", userPage.getTotalPages(), userPage.getTotalElements());

        return "admin/user/user-list";
    }

    /**
     * 특정 사용자의 상세 정보 페이지를 조회하는 요청 처리
     * @param userNo URL 경로에서 추출한 사용자 ID
     * @param model View로 데이터를 전달할 모델 객체
     * @return 렌더링할 뷰의 이름
     */
    @GetMapping("/users/{userNo}") // 요청 경로 변경: /admin/users/{userNo} 형태
    public String userDetailPage(@PathVariable Long userNo, Model model) { // 메소드명 변경 및 파라미터 추가
        log.info("AdminUserController|userDetailPage|관리자 - 사용자 상세 정보 조회 요청. userNo={}", userNo);
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

            return "error/500"; // 임시 에러 페이지
        }
    }

    // TODO: 사용자 목록 페이지, API 엔드포인트(수정/삭제) 등 필요시 추가 구현
    /**
     * 관리자에 의한 회원 탈퇴 처리 (계정 비활성화)
     * @param userNo 탈퇴 처리할 사용자 번호
     * @param userDetails 현재 로그인한 관리자 정보
     * @return 처리 결과 ResponseEntity
     */
    @DeleteMapping("/user/{userNo}/withdraw") // RESTful하게 DELETE 사용, 또는 @PostMapping("/{userNo}/withdraw")
    public ResponseEntity<?> withdrawUserByAdmin(
            @PathVariable Long userNo,
            @AuthenticationPrincipal UserDetails userDetails) {

        String adminUsername = userDetails.getUsername();

        log.info("관리자 {}에 의한 회원 {} 탈퇴 처리 요청 시작", userDetails.getUsername(), userNo);

    /*
        // 관리자 권한 확인
        if (!isAdmin(userDetails)) {
            log.warn("권한 없는 사용자 {}의 회원 {} 탈퇴 시도", userDetails.getUsername(), userNo);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", "회원 탈퇴 처리 권한이 없습니다."));
        }
    */
        try {
//            adminUserService.deactivateUserAccount(userNo, userDetails.getUsername());    // Soft delete
            adminUserService.deleteUserAccountPermanently(userNo, adminUsername);           // Hard delete


            // 성공 시 204 No Content 또는 메시지와 함께 200 OK 반환 가능
            return ResponseEntity.ok(Map.of("success", true, "message", "회원이 성공적으로 탈퇴 처리되었습니다."));

        } catch (EntityNotFoundException e) {
            log.warn("회원 {} 탈퇴 처리 실패: 사용자를 찾을 수 없음", userNo, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", e.getMessage()));
        } catch (IllegalStateException e) { // 이미 탈퇴된 경우 등
            log.warn("회원 {} 탈퇴 처리 실패: {}", userNo, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("회원 {} 탈퇴 처리 중 예기치 않은 오류 발생 (관리자: {})", userNo, userDetails.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", "회원 탈퇴 처리 중 오류가 발생했습니다."));
        }
    }






    // 관리자 권한 확인을 위한 헬퍼 메소드 (실제 구현은 Spring Security 설정에 따름)
    private boolean isAdmin(UserDetails userDetails) {
        if (userDetails == null) return false;

        return userDetails.getAuthorities().stream()
//                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("관리자"));
    }

}
