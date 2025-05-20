package com.wb.between.banner.service;

import com.wb.between.banner.domain.Banner;
import com.wb.between.banner.dto.BannerListResponseDto;
import com.wb.between.banner.repository.BannerRepository;
import com.wb.between.common.exception.CustomException;
import com.wb.between.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BannerService {

    private final BannerRepository bannerRepository;

    /**
     * 배너목록 조회
     * @return
     */
    public List<BannerListResponseDto> findBannerList() {

        //배너목록 조회
        List<Banner> bannerList = bannerRepository.findByUseAt("Y", Sort.by(Sort.Direction.ASC, "sortOrder"));

        return bannerList.stream().map(BannerListResponseDto::from).toList();
    }
}
