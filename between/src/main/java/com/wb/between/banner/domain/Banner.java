package com.wb.between.banner.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "banner")
public class Banner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bNo;

    private String bTitle;

    private String bImageUrl;

    private LocalDateTime startDt;

    private LocalDateTime endDt;

    private String register;

    private LocalDateTime createDt;

    @Column(name = "useAt", length = 10)
    private String useAt;

    private Integer sortOrder;

    private String originalFileName;

    private Long fileSize;

    String mimeType;
}
