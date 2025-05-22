package com.wb.between.admin.popup.service;

import com.wb.between.admin.popup.dto.AdminPopupEditReqDto;
import com.wb.between.admin.popup.dto.AdminPopupRegReqDto;
import com.wb.between.admin.popup.dto.AdminPopupResDto;
import com.wb.between.admin.popup.repository.AdminPopupRepository;
import com.wb.between.common.exception.CustomException;
import com.wb.between.common.exception.ErrorCode;
import com.wb.between.popup.domain.Popups;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPopupService {

    private final AdminPopupRepository adminPopupRepository;

    /**
     * 팝업 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<AdminPopupResDto> findPopupList(Pageable pageable, String searchPopupName) {
        Page<Popups> popupsList = adminPopupRepository.findPopupWithFilter(pageable, searchPopupName);
        return popupsList.map(AdminPopupResDto::from);
    }

    /**
     * 팝업등록
     */
    @Transactional
    public void registPopup(AdminPopupRegReqDto adminPopupRegReqDto) {
        Popups popups = Popups.builder()
                .title(adminPopupRegReqDto.getTitle())
                .contentType(adminPopupRegReqDto.getContentType())
                .contentBody(adminPopupRegReqDto.getContentBody())
                .startDt(adminPopupRegReqDto.getStartDt())
                .endDt(adminPopupRegReqDto.getEndDt())
                .useAt(adminPopupRegReqDto.getUseAt())
                .linkUrl(adminPopupRegReqDto.getLinkUrl())
                .displayOrder(adminPopupRegReqDto.getDisplayOrder())
                .showOnceCookieName(adminPopupRegReqDto.getShowOnceCookieName())
                .build();

        log.debug("registPopup => {}", popups.getUseAt());

        adminPopupRepository.save(popups);
    }

    /**
     * 팝업 단일 조회
     */
    @Transactional(readOnly = true)
    public AdminPopupResDto findPopup(Long popupId) {
        Popups popups = adminPopupRepository.findById(popupId).orElseThrow(()-> new CustomException(ErrorCode.INVALID_INPUT));
        return AdminPopupResDto.from(popups);
    }

    /**
     * 팝업 수정
     */
    @Transactional
    public void editPopup(Long popupId, AdminPopupEditReqDto adminPopupEditReqDto) {
        Popups popups = adminPopupRepository.findById(popupId).orElseThrow(()-> new CustomException(ErrorCode.INVALID_INPUT));

        popups.setTitle(adminPopupEditReqDto.getTitle());
        popups.setContentType(adminPopupEditReqDto.getContentType());
        popups.setContentBody(adminPopupEditReqDto.getContentBody());
        popups.setStartDt(adminPopupEditReqDto.getStartDt());
        popups.setEndDt(adminPopupEditReqDto.getEndDt());
        popups.setUseAt(adminPopupEditReqDto.getUseAt());
        popups.setLinkUrl(adminPopupEditReqDto.getLinkUrl());
        popups.setDisplayOrder(adminPopupEditReqDto.getDisplayOrder());
        popups.setShowOnceCookieName(adminPopupEditReqDto.getShowOnceCookieName());

        //수정
        adminPopupRepository.save(popups);
    }

    /**
     * 삭제
     * @param popupId
     */
    public void deletePopup(Long popupId) {
        adminPopupRepository.deleteById(popupId);
    }


}
