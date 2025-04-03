package com.wb.between.mypage.service;

import com.wb.between.common.exception.CustomException;
import com.wb.between.common.exception.ErrorCode;
import com.wb.between.mypage.dto.MypageResponseDto;
import com.wb.between.mypage.repository.MypageRepository;
import com.wb.between.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MypageService {

    private final MypageRepository mypageRepository;

    public MypageResponseDto findUserbyId(Long userNo) {
        User user = mypageRepository.findById(userNo).orElseThrow(()-> new CustomException(ErrorCode.USER_NOT_FOUND));
        return MypageResponseDto.from(user);
    }

}
