package com.wb.between.faq.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.jdbc.object.UpdatableSqlQuery;

import java.time.LocalDateTime;
import java.util.Date;

@Table(name = "Faq")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FaQ {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 기본키를 자동으로 1씩 증가
    @Column(name = "qNo", updatable = false)
    private int qNo;    //  식별 번호

    @Column(name = "question", nullable = false)
    private String question;    // 질문

    @Column(name = "answer", nullable = false)
    private String answer;  // 답변

    @Column(name = "createDt", nullable = false)
    private LocalDateTime createDt;  //  작성 시간

    @Builder
    public FaQ(int qNo, String question, String answer, LocalDateTime createDt){
        this.qNo = qNo;
        this.question = question;
        this.answer = answer;
        this.createDt = createDt;
    }


}
