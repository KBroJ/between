package com.wb.between.admin.faq.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Table(name = "Faq")
@Entity
@Getter
@Setter
@NoArgsConstructor
public class adminFaQ {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 기본키를 자동으로 1씩 증가
    @Column(name = "qNo", updatable = false)
    private Long qNo;    //  식별 번호

    @Column(name = "question", nullable = false)
    private String question;    // 질문

    @Column(name = "answer", nullable = false)
    private String answer;  // 답변

    @CreationTimestamp
    @Column(name = "createDt", nullable = false)
    private LocalDateTime createDt;  //  작성 시간

    @Builder
    public adminFaQ(Long qNo, String question, String answer, LocalDateTime createDt){
        this.qNo = qNo;
        this.question = question;
        this.answer = answer;
        this.createDt = createDt;
    }


}