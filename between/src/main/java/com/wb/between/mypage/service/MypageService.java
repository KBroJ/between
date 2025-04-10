package com.wb.between.mypage.service;

import com.wb.between.common.exception.CustomException;
import com.wb.between.common.exception.ErrorCode;
import com.wb.between.mypage.dto.MypageCouponResDto;
import com.wb.between.mypage.dto.MypageResponseDto;
import com.wb.between.mypage.dto.UserInfoEditReqDto;
import com.wb.between.user.domain.User;
import com.wb.between.user.repository.UserRepository;
import com.wb.between.usercoupon.domain.UserCoupon;
import com.wb.between.usercoupon.repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
     * @param userNo
     * @return
     */
    public MypageResponseDto findUserbyId(Long userNo) {

        User user = userRepository.findById(userNo).orElseThrow(()-> new CustomException(ErrorCode.USER_NOT_FOUND));

        return MypageResponseDto.from(user);
    }

    /**
     * 유저 정보 수정
     * @param userNo
     * @param userInfoEditReqDto
     * @return
     */
    @Transactional
    public MypageResponseDto updateUserInfo(Long userNo, UserInfoEditReqDto userInfoEditReqDto) {

        //1. 회원정보 조회
        User user = userRepository.findById(userNo).orElseThrow(()-> new CustomException(ErrorCode.USER_NOT_FOUND));

        log.debug("user = {}", user);
        log.debug("userInfoEditReqDto = {}", userInfoEditReqDto);

        //이름
        if(userInfoEditReqDto.getName() != null) {
            user.setName(userInfoEditReqDto.getName());
        }

        if(userInfoEditReqDto.getPhoneNo() != null) {
            user.setPhoneNo(userInfoEditReqDto.getPhoneNo());
        }

        return MypageResponseDto.from(user);
    }

    /**
     * 비밀번호 수정
     */
    @Transactional
    public void changePassword(Long userNo, String currentPassword, String newPassword) {

        //1. 회원정보 조회
        User user = userRepository.findById(userNo).orElseThrow(()-> new CustomException(ErrorCode.USER_NOT_FOUND));

        log.debug("changePassword|user = {}", user);
        
        //2. 현재 비밀번호 검증
        //텍스트 존재 여부
        if(!StringUtils.hasText(currentPassword) || !StringUtils.hasText(newPassword)) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        
        //현재 비밀번호 일치 여부
        if(!passwordEncoder.matches(currentPassword, user.getPassword())) {
            log.debug("현재 비밀번호 일치 여부 = {}", passwordEncoder.matches(currentPassword, user.getPassword()));
            //TODO: 새 에러코드 생성 고려
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        //새 비밀번호가 현재 비밀번호와 일치 여부
        if(passwordEncoder.matches(newPassword, user.getPassword())) {
            log.debug("새 비밀번호가 현재 비밀번호와 일치 여부 = {}", passwordEncoder.matches(newPassword, user.getPassword()));
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
    public  List<MypageCouponResDto> findCouponListById(Long userNo) {
        //1. 회원정보 조회
        User user = userRepository.findById(userNo).orElseThrow(()-> new CustomException(ErrorCode.USER_NOT_FOUND));
        log.debug("MypageService|findCouponListById => {}", user);
        //2. 회원의 쿠폰 목록 조회
        List<UserCoupon> userCouponList = userCouponRepository.findByUserCoupon(user.getUserNo());
        log.debug("MypageService|findByUserCoupon|size => {}", userCouponList.size());

        return userCouponList.stream().map(MypageCouponResDto::from).toList();
    }

    /**
     * 탈퇴 비밀번호 체크
     * @param userNo
     * @param currentPassword
     */
    public void resignCheckPassword(Long userNo, String currentPassword) {
        //1. 회원정보 조회
        User user = userRepository.findById(userNo).orElseThrow(()-> new CustomException(ErrorCode.USER_NOT_FOUND));

        //현재 비밀번호 일치 여부
        if(!passwordEncoder.matches(currentPassword, user.getPassword())) {
            //TODO: 새 에러코드 생성 고려
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
//        // 탈퇴처리 ..
//        user.setUserStts("탈퇴");
//
//        userRepository.save(user);

    }
}
