package com.wb.between.admin.faq.dto;


import com.wb.between.admin.faq.domain.adminFaQ;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class adminFaQListViewResponse {
    private Long qNo;
    private String question;
    private String answer;
    private LocalDateTime createdAt;

    public adminFaQListViewResponse(adminFaQ faq) { //
        this.qNo = faq.getQNo();
        this.question = faq.getQuestion();
        this.answer = faq.getAnswer();
        this.createdAt = faq.getCreateDt();
    }
}
