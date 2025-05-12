package com.wb.between.admin.faq.dto;

import com.wb.between.admin.faq.domain.adminFaQ;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class adminFaQListViewRes {

    private Long qNo;
    private String question;
    private String answer;

    public adminFaQListViewRes(adminFaQ adminFaQ){
        this.qNo = adminFaQ.getQNo();
        this.question = adminFaQ.getQuestion();
        this.answer = adminFaQ.getAnswer();
    }
}
