package com.wb.between.admin.faq.service;

import com.wb.between.admin.faq.domain.adminFaQ;
import com.wb.between.admin.faq.dto.adminFaqCreateRequestDto;
import com.wb.between.admin.faq.dto.adminFaqUpdateReqDto;
import com.wb.between.admin.faq.repository.adminFaQRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class adminFaQService {

    private final adminFaQRepository adminFaQRepository;

    // FAQ 목록 조회
    public List<adminFaQ> findAll() {return adminFaQRepository.findAll();}

    // FAQ 생성
    @Transactional
    public adminFaQ createFaq(adminFaqCreateRequestDto requestDto){

        adminFaQ adminfaQ = new adminFaQ();
        adminfaQ.setQuestion(requestDto.getQuestion());
        adminfaQ.setAnswer(requestDto.getAnswer());

        adminFaQ saveFaq = adminFaQRepository.save(adminfaQ);

        return saveFaq;
    }

    // FAQ 수정
    @Transactional
    public adminFaQ updateFaQ(Long qNo, adminFaqUpdateReqDto requestDto){
        adminFaQ updateFaq = adminFaQRepository.findById(qNo)
                .orElseThrow(() -> new EntityNotFoundException("수정할 FAQ를 찾을 수 없습니다. ID: " + qNo));

        updateFaq.setQuestion(requestDto.getQuestion());
        updateFaq.setAnswer(requestDto.getAnswer());

        adminFaQRepository.save(updateFaq);

        return updateFaq;
    }

    // FAQ qNo 상세 조회
    @Transactional(readOnly = true)
    public adminFaQ findFaqById(Long qNo){
        adminFaQ adminFaqId = adminFaQRepository.findById(qNo)
                .orElseThrow(() -> {
            System.err.println("[Service] ID로 FAQ 조회 실패: " + qNo + " 에 해당하는 데이터를 찾을 수 없음");
            return new EntityNotFoundException("해당 ID의 FAQ를 찾을 수 없습니다: " + qNo);
        });
        return adminFaqId;
    }
    
    // FAQ 삭제
    @Transactional
    public void deleteFaq(Long qNo){
        adminFaQRepository.deleteById(qNo);
    }





}
