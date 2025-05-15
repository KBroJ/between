package com.wb.between.popup.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Builder
@Table(name = "popups")
@NoArgsConstructor
@AllArgsConstructor
public class Popups {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long popupId;

    private String title;

    private String contentType;

    private String contentBody;

    private LocalDateTime startDt;

    private LocalDateTime endDt;

    @Column(name = "useAt", length = 1)
    private String useAt;

    private String linkUrl;

    private int displayOrder;

    private String showOnceCookieName;

    @CreationTimestamp
    private LocalDateTime createDt;

    @CreationTimestamp
    private LocalDateTime updateDt;
}
