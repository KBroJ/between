package com.wb.between.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/*
    회원가입 요청 DTO(Data Transfer Object)
    - 회원가입 요청 시 클라이언트로부터 전달받는 데이터를 담는 객체
    - 데이터 검증: Bean Validation 어노테이션을 통해 입력값 유효성 검증
    - 관심사 분리: 엔티티(Entity)와 프레젠테이션 계층의 요구사항 분리

    입력값 검증 로직을 한 곳에 모아 관리
    명확한 API 계약: 클라이언트가 제공해야 할 데이터 형식을 명확히 정의
 */
@Data
public class SignupReqDto {

    @NotBlank(message = "이메일은 필수 입력값입니다.")   // 입력값이 null이거나 공백인 경우 유효성 검증 실패
    @Email(message = "유효한 이메일 형식이 아닙니다.")   // 이메일 형식이 아닌 경우 유효성 검증 실패
    private String email;

    /*
        - ^: 문자열의 시작
        - (?=.*[A-Za-z]): 영문자가 최소 1개 이상 포함
        - (?=.*\d): 숫자가 최소 1개 이상 포함
        - (?=.*[@$!%*#?&]): 특수문자(@, $, !, %, *, #, ?, &)가 최소 1개 이상 포함
        - [A-Za-z\d@$!%*#?&]{8,}: 영문자, 숫자, 특수문자(@, $, !, %, *, #, ?, &) 중 하나 이상을 포함하며, 8자 이상
        - $: 문자열의 끝
     */
    @NotBlank(message = "비밀번호는 필수 입력값입니다.")
    @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.") // 입력값의 길이가 8자 미만인 경우 유효성 검증 실패
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",   // 비밀번호 정규식
            message = "비밀번호는 영문자, 숫자, 특수문자를 포함해야 합니다.")
    private String password;

    @NotBlank(message = "이름은 필수 입력값입니다.")
    private String name;

    /*
        - ^01: 01로 시작
        - [0|1|6|7|8|9]: 0, 1, 6, 7, 8, 9 중 하나
        - ([0-9]{3,4}): 숫자 3~4자리
        - ([0-9]{4}): 숫자 4자리
        - $: 문자열의 끝

        - [-]?: 하이픈(-)이 0개 또는 1개 포함
     */
    @NotBlank(message = "휴대폰 번호는 필수 입력값입니다.")
//    @Pattern(regexp = "^01([0|1|6|7|8|9])([0-9]{3,4})([0-9]{4})$",
    @Pattern(regexp = "^01([0|1|6|7|8|9])[-]?([0-9]{3,4})[-]?([0-9]{4})$",
            message = "유효한 휴대폰 번호 형식이 아닙니다.")
    private String phoneNo;

}
