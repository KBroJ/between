package com.wb.between.admin.faq.controller;


import com.wb.between.admin.faq.domain.adminFaQ;
import com.wb.between.admin.faq.dto.adminFaQListViewResponse;
import com.wb.between.admin.faq.dto.adminFaqCreateRequestDto;
import com.wb.between.admin.faq.dto.adminFaqUpdateReqDto;
import com.wb.between.admin.faq.service.adminFaQService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class adminFaQAPIController {

    private final adminFaQService adminFaQService;

    @GetMapping("/api/adminFaQList")
    // @PreAuthorize("hasRole('ADMIN')") // 관리자 권한 관리
    public ResponseEntity<List<adminFaQListViewResponse>> findAll(){
        List<adminFaQ> faqList = adminFaQService.findAll();
        try{

        List<adminFaQListViewResponse> faqLists = faqList.stream()
                .map(adminFaQListViewResponse::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok()
                .body(faqLists);

        } catch (Exception e) {
            System.err.println("[Controller] FAQ 목록 조회 중 심각한 오류 발생: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/api/adminFaQCreate")
    public  ResponseEntity<?> createFaq (
            @Valid @RequestBody adminFaqCreateRequestDto requestDto,
            BindingResult bindingResult) {
        // 입력 데이터 유효성 검사 로직
        if(bindingResult.hasErrors()){
            Map<String, String> errors = bindingResult.getFieldErrors().stream().collect(Collectors.toMap(fe -> fe.getField(), fe -> fe.getDefaultMessage()));
            return ResponseEntity.badRequest().body(Map.of("success", false, "errors", errors));
        }

        try{
           adminFaQ createFaq = adminFaQService.createFaq(requestDto);

            URI location = ServletUriComponentsBuilder
                    .fromCurrentRequest().path("/{id}")
                    .buildAndExpand(createFaq.getQNo()).toUri();
            return ResponseEntity.created(location).body(Map.of(
                    "success", true,
                    "message", "FAQ가 성공적으로 등록되었습니다.",
                    "faqId", createFaq.getQNo()
            ));
        } catch (Exception e){
            System.err.println("FAQ 등록 중 서버 오류 발생: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "FAQ 등록 중 오류가 발생했습니다."));
        }
    }

    @PutMapping("/api/admin/faqs/{qNo}")
    public  ResponseEntity<?> updateFaQ(
            @PathVariable Long qNo,
            @Valid @RequestBody adminFaqUpdateReqDto requestDto,
            BindingResult bindingResult
    ){
       if (bindingResult.hasErrors()) {
            Map<String, String> errors = bindingResult.getFieldErrors().stream()
                    .collect(Collectors.toMap( fe -> fe.getField(), fe -> fe.getDefaultMessage()));
            return ResponseEntity.badRequest().body(Map.of("success", false, "errors", errors)); // 400 Bad Request
        }

        try{
            adminFaQ updateFaq = adminFaQService.updateFaQ(qNo, requestDto);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "FAQ가 성공적으로 수정되었습니다.",
                    "faqId", updateFaq.getQNo()
                    // "data", new adminFaQListViewResponse(updatedFaq) // 필요시 응답에 데이터 포함
            ));
        } catch (EntityNotFoundException e) {
            // 4. 수정할 FAQ ID가 없는 경우 (404 Not Found)
            System.err.println("FAQ 수정 실패 (Not Found): " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            // 5. 기타 서버 오류 (DB 오류 등)
            System.err.println("FAQ 수정 중 서버 오류 발생: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "FAQ 수정 중 오류가 발생했습니다."));
        }
    }
    
    // FAQ 상세 조회
    @GetMapping("/api/admin/faqs/{qNo}")
    public ResponseEntity<?> getFaqById(@PathVariable Long qNo){
        try{
            adminFaQ faqNo = adminFaQService.findFaqById(qNo);

            adminFaQListViewResponse faqresDto = new adminFaQListViewResponse(faqNo);

            return ResponseEntity.ok(faqresDto);

        } catch (EntityNotFoundException e) {
            System.err.println("FAQ 조회 실패 (Not Found): " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) { // 기타 예외
            System.err.println("FAQ 조회 중 서버 오류 발생: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "FAQ 조회 중 오류 발생"));
        }
    }

    // FAQ 삭제
    @DeleteMapping("/api/admin/faqs/{qNo}")
    public ResponseEntity<?> deleteFaq(@PathVariable Long qNo){
        try{
            adminFaQService.deleteFaq(qNo);

            return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "FAQ가 성공적으로 삭제되었습니다."));
        } catch (EntityNotFoundException e) {
            System.err.println("FAQ 삭제 실패 (Not Found): " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            // 기타 서버 오류
            System.err.println("FAQ 삭제 중 서버 오류 발생: " + e.getMessage());
            e.printStackTrace(); // 개발 중 상세 에러 확인
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "FAQ 삭제 중 오류가 발생했습니다."));
        }
    }

}
