package com.wb.between.mypage.service;

import com.wb.between.common.exception.CustomException;
import com.wb.between.common.exception.ErrorCode;
import com.wb.between.mypage.dto.MypageCouponResDto;
import com.wb.between.mypage.dto.MypageUserInfoResDto;
import com.wb.between.mypage.dto.UserInfoEditReqDto;
import com.wb.between.mypage.dto.UserPasswordEditReqDto;
import com.wb.between.user.domain.User;
import com.wb.between.user.repository.UserRepository;
import com.wb.between.usercoupon.domain.UserCoupon;
import com.wb.between.usercoupon.repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MypageService {

    private final UserRepository userRepository;

    private final UserCouponRepository userCouponRepository;

    private final PasswordEncoder passwordEncoder;

    /**
     * 유저 조회
     */
    public MypageUserInfoResDto findUserbyId(Long userNo) {

        User user = userRepository.findById(userNo).orElseThrow(()-> new CustomException(ErrorCode.USER_NOT_FOUND));
        log.debug("findUserbyId|user = {}", user);
        return MypageUserInfoResDto.from(user);
    }

    /**
     * 유저 정보 수정
     */
    @Transactional
    public MypageUserInfoResDto updateUserInfo(Long userNo, UserInfoEditReqDto userInfoEditReqDto) {

        //1. 회원정보 조회
        User user = userRepository.findById(userNo).orElseThrow(()-> new CustomException(ErrorCode.USER_NOT_FOUND));

        log.debug("user = {}", user);
        log.debug("userInfoEditReqDto = {}", userInfoEditReqDto);
        log.debug("userInfoEditReqDto.getName() = {}", userInfoEditReqDto.getName());
        //이름
        if(userInfoEditReqDto.getName() != null) {
            user.setName(userInfoEditReqDto.getName());
        }

        //전화번호
        if(userInfoEditReqDto.getPhoneNo() != null) {
            user.setPhoneNo(userInfoEditReqDto.getPhoneNo());
        }

        return MypageUserInfoResDto.from(user);
    }

    /**
     * 비밀번호 수정
     */
    @Transactional
    public void changePassword(Long userNo, UserPasswordEditReqDto userPasswordEditReqDto) {

        //1. 회원정보 조회
        User user = userRepository.findById(userNo).orElseThrow(()-> new CustomException(ErrorCode.USER_NOT_FOUND));

        log.debug("changePassword|user = {}", user);

        String currentPassword = userPasswordEditReqDto.getCurrentPassword();
        String newPassword = userPasswordEditReqDto.getNewPassword();
        String encodedPassword = user.getPassword();

        //2. 현재 비밀번호 검증
        //텍스트 존재 여부
        if(!StringUtils.hasText(currentPassword) || !StringUtils.hasText(newPassword)) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        
        //현재 비밀번호 일치 여부
        if(!passwordEncoder.matches(currentPassword, encodedPassword)) {
            //TODO: 새 에러코드 생성 고려
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        //새 비밀번호가 현재 비밀번호와 일치 여부
        if(passwordEncoder.matches(newPassword, encodedPassword)) {
            //TODO: 새 에러코드 생성 고려
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        //3. 비밀번호 수정
        String encodedNewPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedNewPassword);
    }

    /**
     * 마이페이지 > 쿠폰목록
     * @param userNo
     * @return
     */
    public  List<MypageCouponResDto> findCouponListById(Long userNo,
                                                        String tab,
                                                        LocalDate startDate,
                                                        LocalDate endDate) {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDateTime = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTimePlusOne = (endDate != null) ? endDate.plusDays(1).atStartOfDay() : null;

        //1. 회원정보 조회
        User user = userRepository.findById(userNo).orElseThrow(()-> new CustomException(ErrorCode.USER_NOT_FOUND));
        log.debug("MypageService|findCouponListById => {}", user);

        //사용 가능 쿠폰, 기간만료 쿠폰 조회 처리
        //추후에 queryDsl 변경 고려
        List<UserCoupon> userCouponList;

        //사용 가능
        if ("available".equalsIgnoreCase(tab)) {
            //2. 회원의 쿠폰 목록 조회
            userCouponList = userCouponRepository.findByUserCoupon(user.getUserNo(),
                    "N",
                    startDateTime,
                    endDateTimePlusOne);

        } else if ("expired".equalsIgnoreCase(tab)) {
            //만료
            userCouponList = userCouponRepository.findExpiredCouponsWithDateFilter(user.getUserNo(),
                    "Y",
                    startDateTime,
                    endDateTimePlusOne);
        } else {
            userCouponList = Collections.emptyList();
        }

        return userCouponList.stream().map(MypageCouponResDto::from).toList();
    }

    /**
     * 마이페이지 > 쿠폰목록
     * @param userNo
     * @return
     */
    public  Page<MypageCouponResDto> findCouponListByIdPage(Long userNo,
                                                            String tab,
                                                            LocalDate startDate,
                                                            LocalDate endDate,
                                                            Pageable pageable) {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDateTime = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTimePlusOne = (endDate != null) ? endDate.plusDays(1).atStartOfDay() : null;

        //1. 회원정보 조회
        User user = userRepository.findById(userNo).orElseThrow(()-> new CustomException(ErrorCode.USER_NOT_FOUND));
        log.debug("MypageService|findCouponListByIdPage => {}", user);
        log.debug("MypageService|findCouponListByIdPage|tab => {}", tab);
        //사용 가능 쿠폰, 기간만료 쿠폰 조회 처리
        //추후에 queryDsl 변경 고려
        Page<UserCoupon> userCouponList;

        //사용 가능
        if ("available".equalsIgnoreCase(tab)) {
            //2. 회원의 쿠폰 목록 조회
            userCouponList = userCouponRepository.findByUserCouponPage(user.getUserNo(),
                    "N",
                    now,
                    startDateTime,
                    endDateTimePlusOne,
                    pageable);

        } else if ("expired".equalsIgnoreCase(tab)) {
            //만료
            userCouponList = userCouponRepository.findExpiredCouponsWithDateFilterPage(user.getUserNo(),
                    now,
                    startDateTime,
                    endDateTimePlusOne,
                    pageable);
        } else {
            userCouponList = Page.empty();
        }

        return userCouponList.map(MypageCouponResDto::from);
    }

    /**
     * 비밀번호 일치 여부 확인 (기존과 유사)
     */
    public boolean verifyPassword(Long userNo, String rawPassword) {
        User user = userRepository.findById(userNo)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT));
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }

    /**
     * 회원 탈퇴
     * @param userNo
     */
    @Transactional
    public void accountDeletion(Long userNo) {
        userRepository.deleteById(userNo);
    }
}
