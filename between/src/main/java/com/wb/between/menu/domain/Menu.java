package com.wb.between.menu.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigInteger;
import java.util.Date;

@Entity
@Data
@Table(name = "menu")
public class Menu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long menuNo;

    private Long upperMenuNo;

    private String menuNm;

    private String menuDsc;

    private String menuUrl;

    @Column(name = "useAt", length = 10)
    private String useAt;

    private Date createDt;

    private int sortOrder;
}
