package com.wb.between.admin.banner.service;

import com.wb.between.admin.banner.dto.AdminBannerEditReqDto;
import com.wb.between.admin.banner.dto.AdminBannerRegistReqDto;
import com.wb.between.admin.banner.dto.AdminBannerResDto;
import com.wb.between.admin.banner.repository.AdminBannerRepository;
import com.wb.between.banner.domain.Banner;
import com.wb.between.common.exception.CustomException;
import com.wb.between.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminBannerService {

    private final AdminBannerRepository adminBannerRepository;

    /**
     * 관리자 > 배너 조회
     * @return
     */
    public List<AdminBannerResDto> findAll() {

        List<Banner> sortedBannerList = adminBannerRepository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder"));

        return sortedBannerList.stream()
                .map(AdminBannerResDto::from)
                .toList();
    }

    /**
     * 배너 등록
     */
    @Transactional
    public void registMainBanner(AdminBannerRegistReqDto adminBannerRegistReqDto) throws IOException {
        //배너 이미지 파일
        MultipartFile bannerImgFile = adminBannerRegistReqDto.getBannerImgFile();
        log.debug("bannerImgFile = {}", bannerImgFile);
        String imageUrlForDb = adminBannerRegistReqDto.getBImageUrl(); // DTO의 bImageUrl을 기본값으로 사용
        log.debug("imageUrlForDb = {}", imageUrlForDb);
        String originalFileName = null;

        // 개발 단계용 임시 업로드 경로 (주의: 실제 운영에서는 외부 설정으로 관리해야 합니다)
        String tempUploadPath = "/tmp/banner_uploads"; // 예시 경로 (Windows: "C:/temp/banner_uploads")
        Path uploadDirectory = Paths.get(tempUploadPath);

        if(bannerImgFile != null && !bannerImgFile.isEmpty()) {
            // 업로드 디렉토리가 없으면 생성
            if (!Files.exists(uploadDirectory)) {
                try {
                    Files.createDirectories(uploadDirectory);
                    log.info("임시 업로드 디렉토리 생성: {}", uploadDirectory);
                } catch (IOException e) {
                    log.error("임시 업로드 디렉토리 생성 실패: {}", uploadDirectory, e);
                    throw new IOException("업로드 디렉토리 생성에 실패했습니다: " + uploadDirectory, e);
                }
            }
            // 파일 이름 중복 방지 및 원본 파일명 유지 (안전하게)
            originalFileName = StringUtils.cleanPath(bannerImgFile.getOriginalFilename());
            log.debug("originalFileName = {}", originalFileName);
            String fileExtension = StringUtils.getFilenameExtension(originalFileName);
            log.debug("fileExtension = {}", fileExtension);
            String storedFileName = UUID.randomUUID() + (fileExtension != null ? "." + fileExtension : "");
            log.debug("storedFileName = {}", storedFileName);

            // 파일 저장
            Path targetFilePath = uploadDirectory.resolve(storedFileName);
            try (InputStream inputStream = bannerImgFile.getInputStream()) {
                Files.copy(inputStream, targetFilePath, StandardCopyOption.REPLACE_EXISTING);
                log.info("배너 이미지 파일 저장 성공: {}", targetFilePath);
            } catch (IOException e) {
                log.error("배너 이미지 파일 저장 실패: {}", targetFilePath, e);
                // 파일 저장 실패 시 명확한 예외 메시지 제공
                throw new IOException("배너 이미지 파일 '" + originalFileName + "' 저장에 실패했습니다.", e);
            }

            // DB에 저장할 이미지 URL 업데이트 (실제 서비스에서는 웹 접근 가능한 URL이어야 함)
            // 개발 단계에서는 저장된 파일 경로 또는 가상의 웹 경로를 사용할 수 있습니다.
            // 예: imageUrlForDb = "/uploaded-banners/" + storedFileName; (웹 서버 설정 필요)
            imageUrlForDb = targetFilePath.toString(); // 여기서는 로컬 파일 시스템 경로를 그대로 사용 (개발용)
            log.info("DB에 저장될 이미지 경로: {}", imageUrlForDb);
        }


        //배너 객체 생성
        Banner banner = Banner.builder()
                .bTitle(adminBannerRegistReqDto.getBTitle())
                .bImageUrl(imageUrlForDb)
                .startDt(adminBannerRegistReqDto.getStartDt())
                .endDt(adminBannerRegistReqDto.getEndDt())
                .register(adminBannerRegistReqDto.getRegister())
                .createDt(LocalDateTime.now())
                .useAt(adminBannerRegistReqDto.getUseAt())
                .sortOrder(adminBannerRegistReqDto.getSortOrder())
                .originalFileName(originalFileName)
                .build();

        adminBannerRepository.save(banner);
    }

    /**
     * 배너 단일 조회
     * @param bNo
     * @return
     */
    @Transactional(readOnly = true)
    public AdminBannerResDto findBannerById(Long bNo) {
        Banner banner = adminBannerRepository.findById(bNo).orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT));
        return AdminBannerResDto.from(banner);
    }

    @Transactional
    public void editBanner(Long bNo, AdminBannerEditReqDto adminBannerEditReqDto) {
        Banner banner = adminBannerRepository.findById(bNo).orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT));

        //배너 이미지 파일
        MultipartFile bannerImgFile = adminBannerEditReqDto.getBannerImgFile();
        String imageUrlForDb = adminBannerEditReqDto.getBImageUrl(); // DTO의 bImageUrl을 기본값으로 사용
        log.debug("imageUrlForDb = {}", imageUrlForDb);
        // 개발 단계용 임시 업로드 경로 (주의: 실제 운영에서는 외부 설정으로 관리해야 합니다)
        String tempUploadPath = "/tmp/banner_uploads"; // 예시 경로 (Windows: "C:/temp/banner_uploads")
        Path uploadDirectory = Paths.get(tempUploadPath);


        banner.setBTitle(adminBannerEditReqDto.getBTitle());
        banner.setBImageUrl(adminBannerEditReqDto.getBImageUrl());
        banner.setStartDt(adminBannerEditReqDto.getStartDt());
        banner.setEndDt(adminBannerEditReqDto.getEndDt());
        banner.setUseAt(adminBannerEditReqDto.getUseAt());

    }

    /**
     * 배너 삭제
     * @param bNo
     */
    @Transactional
    public void deleteBanner(Long bNo) {
        adminBannerRepository.deleteById(bNo);
    }



}
