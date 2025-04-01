package com.wb.between.banner.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigInteger;
import java.util.Date;

@Entity
@Data
@Table(name = "banner")
public class Banner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bNo;

    private String bTitle;

    private String bImageUrl;

    private Date startDt;

    private Date endDt;

    private String register;

    private Date createDt;

    @Column(name = "useAt", length = 10)
    private String useAt;
}
