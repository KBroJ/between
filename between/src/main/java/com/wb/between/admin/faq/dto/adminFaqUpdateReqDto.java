package com.wb.between.admin.faq.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class adminFaqUpdateReqDto {

    @NotBlank(message = "질문 내용은 필수입니다.")
    private String question;

    @NotBlank(message = "답변 내용은 필수입니다.")
    private String answer;
}
