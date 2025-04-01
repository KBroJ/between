package com.wb.between.faq.dto;

import com.wb.between.faq.domain.FaQ;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class AddFaQRequest {

    private  String question;
    private  String answer;
    private LocalDateTime createDt;

    public FaQ toEntity(){
        return FaQ.builder()
                .question(question)
                .answer(answer)
                .createDt(createDt)
                .build();
    }
}
