package com.wb.between.admin.coupon.service;

import com.wb.between.admin.coupon.dto.AdminCouponEditReqDto;
import com.wb.between.admin.coupon.dto.AdminCouponRegistReqDto;
import com.wb.between.admin.coupon.dto.AdminCouponResDto;
import com.wb.between.admin.coupon.repository.AdminCouponRepository;
import com.wb.between.common.exception.CustomException;
import com.wb.between.common.exception.ErrorCode;
import com.wb.between.coupon.domain.Coupon;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCouponService {

    private final AdminCouponRepository adminCouponRepository;

    /**
     * 관리자 > 쿠폰 목록
     * @param pageable
     * @param searchCouponName
     * @return
     */
    public Page<AdminCouponResDto> findAdminCouponList(Pageable pageable, String searchCouponName) {
        Page<Coupon> couponPageList = adminCouponRepository.findCouponsWithFilter(pageable, searchCouponName);
        return couponPageList.map(AdminCouponResDto::from);
    }

    /**
     * 관리자 > 쿠폰 등록
     * @param adminCouponRegistReqDto
     */
    @Transactional
    public void registAdminCoupon(AdminCouponRegistReqDto adminCouponRegistReqDto) {
        log.debug("registAdminCoupon|adminCoupon = {}", adminCouponRegistReqDto);

        //쿠폰 객체 생성
        Coupon coupon = Coupon.builder()
                .cpnNm(adminCouponRegistReqDto.getCpnNm())
                .discount(adminCouponRegistReqDto.getDiscount()) // DTO와 Entity의 타입이 맞아야 함
                .discountAt(adminCouponRegistReqDto.getDiscountAt())
                .cpnStartDt(adminCouponRegistReqDto.getCpnStartDt())
                .cpnEndDt(adminCouponRegistReqDto.getCpnEndDt())
                .cpnDsc(adminCouponRegistReqDto.getCpnDsc())
                // activeYn은 DTO에서 받거나, 서비스에서 기본값 설정 가능
                .activeYn(adminCouponRegistReqDto.getActiveYn() != null ? adminCouponRegistReqDto.getActiveYn() : "Y") // 예: 기본값 'Y'
                .createDate(LocalDateTime.now())
                .build();

        // === 데이터 영속화 ===
        // 리포지토리를 통해 Coupon 엔티티를 DB에 저장
        adminCouponRepository.save(coupon);

    }

    /**
     * 쿠폰 상세 조회
     * @param cpNo
     * @return
     */
    @Transactional(readOnly = true)
    public AdminCouponResDto findAdminCoupon(Long cpNo) {
        Coupon coupon = adminCouponRepository.findById(cpNo).orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT));
        log.debug("findAdminCoupon|coupon => {}", coupon);
        return AdminCouponResDto.from(coupon);
    }

    /**
     * 쿠폰 수정
     * @param cpNo
     * @return
     */
    @Transactional
    public AdminCouponResDto updateAdminCoupon(Long cpNo, AdminCouponEditReqDto adminCouponEditReqDto) {

        //1. 쿠폰정보 조회
        Coupon coupon = adminCouponRepository.findById(cpNo).orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT));
        log.debug("findAdminCoupon|coupon => {}", coupon);

        coupon.setCpnNm(adminCouponEditReqDto.getCpnNm());
        //사용여부 수정
        coupon.setActiveYn(adminCouponEditReqDto.getActiveYn());
        coupon.setDiscount(adminCouponEditReqDto.getDiscount());
        coupon.setCpnDsc(adminCouponEditReqDto.getCpnDsc());
        coupon.setCpnStartDt(adminCouponEditReqDto.getCpnStartDt());
        coupon.setCpnEndDt(adminCouponEditReqDto.getCpnEndDt());
        
        
        return AdminCouponResDto.from(coupon);
    }


}
