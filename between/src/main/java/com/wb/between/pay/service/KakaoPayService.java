package com.wb.between.pay.service;

import com.wb.between.pay.dto.KakaoPayApproveResponseDto;
import com.wb.between.pay.dto.KakaoPayReadyRequestDto;
import com.wb.between.pay.dto.KakaoPayReadyResponseDto;

import com.wb.between.pay.repository.PaymentRepository;
import com.wb.between.reservation.reserve.repository.ReservationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;


import java.util.Map;

@Service
public class KakaoPayService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ReservationRepository reservationRepository;


    @Value("${kakao.pay.admin-key}")
    private String kakaoAdminKey;

    // 카카오페이 API 호스트
    private static final String KAKAO_PAY_HOST = "https://kapi.kakao.com";
    // API 엔드포인트
    private static final String READY_ENDPOINT = "/v1/payment/ready";
    private static final String APPROVE_ENDPOINT = "/v1/payment/approve";

    private final RestTemplate restTemplate;

    // RestTemplate 주입 (WebClient 사용 시 변경)
    public KakaoPayService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    // RestTemplate Bean 설정이 필요할 수 있음 (@Configuration 클래스에)



    /**
     * 카카오페이 결제 준비 API 호출
     * @param requestDto 프론트엔드에서 받은 결제 기본 정보
     * @param partnerOrderId 백엔드에서 생성/관리하는 고유 주문번호 (Controller에서 전달받음)
     * @param partnerUserId 백엔드에서 생성/관리하는 고유 사용자 ID (Controller에서 전달받음)
     * @return 카카오페이 준비 응답 DTO (tid, next_redirect_pc_url 등 포함)
     * @throws RuntimeException API 호출 실패 시
     */
    public KakaoPayReadyResponseDto readyPayment(KakaoPayReadyRequestDto requestDto, String partnerOrderId, String partnerUserId) { // <<<--- partnerOrderId, partnerUserId 파라미터 추가!
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "KakaoAK " + kakaoAdminKey); // 카카오 Admin 키
        headers.add("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");

        // 카카오페이 API 요청 파라미터 설정
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("cid", "TC0ONETIME"); // 테스트용 CID - 임시로 박아두기
      
        params.add("partner_order_id", partnerOrderId);   // Controller에서 생성/전달받은 주문번호
        params.add("partner_user_id", partnerUserId);     // Controller에서 생성/전달받은 사용자 ID
        // ---------------------------------------------
        params.add("item_name", requestDto.getItemName()); // 상품명 (예: "좌석 A01 시간제 예약")
        params.add("quantity", String.valueOf(requestDto.getQuantity())); // 수량 (예: 1)
        params.add("total_amount", String.valueOf(requestDto.getTotalAmount())); // 최종 결제 금액
        params.add("tax_free_amount", "0"); // 비과세 금액
        // --- !!! 리다이렉트 URL 확인 및 실제 주소로 변경 !!! ---
        params.add("approval_url", "http://localhost:8080/api/payment/kakao/approve"); // 결제 성공 시 돌아올 백엔드 주소
        params.add("cancel_url", "http://localhost:8080/api/payment/kakao/cancel");   // 결제 취소 시 돌아올 백엔드 주소
        params.add("fail_url", "http://localhost:8080/api/payment/kakao/fail");     // 결제 실패 시 돌아올 백엔드 주소
        // --------------------------------------------------

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(params, headers);

        // 카카오페이 결제 준비 API 호출 (POST)
        ResponseEntity<KakaoPayReadyResponseDto> responseEntity;
        try {
            responseEntity = restTemplate.postForEntity(
                    KAKAO_PAY_HOST + READY_ENDPOINT,
                    requestEntity,
                    KakaoPayReadyResponseDto.class // 응답 DTO 클래스
            );
        } catch (Exception e) {
            System.err.println("카카오페이 준비 API 호출 중 예외 발생: " + e.getMessage());
            throw new RuntimeException("카카오페이 통신 중 오류가 발생했습니다.", e);
        }


        // 응답 코드 확인 및 결과 반환
        if (responseEntity.getStatusCode() == HttpStatus.OK && responseEntity.getBody() != null) {
            System.out.println("카카오페이 준비 API 호출 성공: " + responseEntity.getBody());
            // 성공 시 Controller에서 이 응답의 tid 등을 세션에 저장해야 함
            return responseEntity.getBody();
        } else {
            // 카카오 API가 에러 코드를 보낸 경우 (4xx, 5xx 등)
            System.err.println("카카오페이 준비 API 호출 실패: " + responseEntity.getStatusCode() + ", Body: " + responseEntity.getBody());
            // 실제로는 에러 응답 Body를 파싱해서 더 구체적인 에러 메시지 전달 필요
            throw new RuntimeException("카카오페이 결제 준비 요청에 실패했습니다. 응답코드: " + responseEntity.getStatusCode());
        }
    }

    /**
     * 카카오페이 결제 승인 API 호출 (구현 필요)
     */
    public KakaoPayApproveResponseDto approvePayment(String tid, String partnerOrderId, String partnerUserId, String pgToken) {
        System.out.printf("카카오페이 승인 API 호출 시도: tid=%s, orderId=%s, userId=%s, pgToken=%s%n", tid, partnerOrderId, partnerUserId, pgToken);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "KakaoAK " + kakaoAdminKey);
        headers.add("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");


        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("cid", "TC0ONETIME");
        params.add("tid", tid);
        params.add("partner_order_id", partnerOrderId);
        params.add("partner_user_id", partnerUserId);
        params.add("pg_token", pgToken);

        // --- !!! API 호출 직전 로그 추가 !!! ---
        System.out.println("카카오페이 승인 API 요청 파라미터: " + params);
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(params, headers);


        ResponseEntity<KakaoPayApproveResponseDto> responseEntity = restTemplate.postForEntity(
                KAKAO_PAY_HOST + APPROVE_ENDPOINT,
                requestEntity,
                KakaoPayApproveResponseDto.class
        );
        // -------------------------------

        if (responseEntity.getStatusCode() == HttpStatus.OK && responseEntity.getBody() != null) {
            System.out.println("카카오페이 승인 API 성공: " + responseEntity.getBody());
            // !!! 중요: 여기서 최종 결제 금액(amount.total), 결제 상태 등 확인 및 검증 필요 !!!
            return responseEntity.getBody(); // 이제 KakaoPayApproveResponseDto 객체 반환
        } else {
            System.err.println("카카오페이 승인 API 실패: " + responseEntity.getStatusCode() + ", Body: " + responseEntity.getBody());
            throw new RuntimeException("카카오페이 결제 승인에 실패했습니다. 응답코드: " + responseEntity.getStatusCode());
        }
    }
}
