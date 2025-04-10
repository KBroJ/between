package com.wb.between.common.exception;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 커스텀 예외
     */
    @ExceptionHandler(CustomException.class)
    public String handleCustomException(CustomException e, Model model) {

        //에러코드
        ErrorCode errorCode = e.getErrorCode();
        model.addAttribute("status", errorCode.getStatus());
        model.addAttribute("message", errorCode.getMessage());
        model.addAttribute("code", errorCode.getCode());
        return "error/custom-error"; // 모든 에러 기본 처리
    }

    /**
     * 일발적인 런타임 에러 처리
     */
    @ExceptionHandler(RuntimeException.class)
    public String handleRuntimeErrors(RuntimeException ex, Model model) {

        //에러코드
        model.addAttribute("code", "COMMON_001");
        model.addAttribute("message", "예기치 않은 오류가 발생했습니다.");
        model.addAttribute("status", 500);
        return "error/custom-error"; // 모든 에러 기본 처리
    }

    /**
     * 일발적인 에러 처리
     */
    @ExceptionHandler(Exception.class)
    public String handleGeneralErrors(Exception ex, Model model) {

        //에러코드
        model.addAttribute("code", "COMMON_001");
        model.addAttribute("message", "알 수 없는 오류가 발생했습니다.");
        model.addAttribute("status", 500);
        return "error/custom-error"; // 모든 에러 기본 처리
    }
}
