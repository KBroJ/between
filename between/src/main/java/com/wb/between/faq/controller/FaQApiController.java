package com.wb.between.faq.controller;

import com.wb.between.faq.domain.FaQ;
import com.wb.between.faq.dto.AddFaQRequest;
import com.wb.between.faq.dto.FaQListViewResponse;
import com.wb.between.faq.service.FaQService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
public class FaQApiController {

    private final FaQService faQService;

    @PostMapping("/api/addFaQ")
    public ResponseEntity<FaQ> addFaQ(@RequestBody AddFaQRequest request){
        FaQ saveFaQ = faQService.save(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(saveFaQ);
    }


    @GetMapping("/api/FaQList")
    public ResponseEntity<List<FaQListViewResponse>> findAllFaQ(){
        List<FaQListViewResponse> faqList = faQService.findAll()
                .stream()
                .map(FaQListViewResponse::new)
                .toList();

        return ResponseEntity.ok()
                .body(faqList);
    }
}
