package com.wb.between.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // 공통
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_001", "서버 오류입니다."),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON_002", "잘못된 요청입니다"),

    //권한
    UNAUTHORIZED_USER(HttpStatus.UNAUTHORIZED , "AUTH_001", "로그인 후 이용해주세요."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN , "AUTH_002", "권한이 없습니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_003", "토큰이 만료되었습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_004", "유효하지 않은 토큰입니다."),

    //사용자
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "사용자를 찾을 수 없습니다."),

    //메뉴
    MENU_NOT_FOUND(HttpStatus.NOT_FOUND, "MENU_001", "메뉴를 찾을 수 없습니다."),

    //배너
    BANNER_NOT_FOUND(HttpStatus.NOT_FOUND, "BANNER_001", "배너를 찾을 수 없습니다."),
  ;


    private final HttpStatus status;
    private final String code;
    private final String message;

}
