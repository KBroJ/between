package com.wb.between.popup.service;

import com.wb.between.popup.domain.Popups;
import com.wb.between.popup.dto.PopupResDto;
import com.wb.between.popup.repository.PopupRespository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PopupService {

    private final PopupRespository popupRespository;

    /**
     * 팝업 조회
     */
    public List<PopupResDto> popups() {
        LocalDateTime currentDt = LocalDateTime.now();
        List<Popups> popups = popupRespository.findByUseAt("Y", currentDt,Sort.by(Sort.Direction.ASC, "displayOrder"));

        return popups.stream().map(PopupResDto::from).toList();
    }

}
