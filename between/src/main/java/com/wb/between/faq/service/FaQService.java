package com.wb.between.faq.service;

import com.wb.between.faq.domain.FaQ;
import com.wb.between.faq.dto.AddFaQRequest;
import com.wb.between.faq.repository.FnQRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class FaQService {

    private final FnQRepository fnQRepository;

    public FaQ save(AddFaQRequest request){
        return fnQRepository.save(request.toEntity());
    }

    public List<FaQ> findAll(){
        return fnQRepository.findAll();
    }
}
