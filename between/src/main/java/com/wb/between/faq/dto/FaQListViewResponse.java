package com.wb.between.faq.dto;

import com.wb.between.faq.domain.FaQ;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Date;


@Getter
public class FaQListViewResponse {
    private final int qNo;
   private final String question;
   private final String answer;

   public FaQListViewResponse(FaQ faq){
       this.qNo = faq.getQNo();
       this.question = faq.getQuestion();
       this.answer = faq.getAnswer();
   }


}
