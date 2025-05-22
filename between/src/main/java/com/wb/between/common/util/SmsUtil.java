package com.wb.between.common.util;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.nurigo.sdk.NurigoApp;
import net.nurigo.sdk.message.model.Message;
import net.nurigo.sdk.message.request.SingleMessageSendingRequest;
import net.nurigo.sdk.message.response.SingleMessageSentResponse;
import net.nurigo.sdk.message.service.DefaultMessageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SmsUtil {

    @Value("${coolsms.api.key}")
    private String apiKey;
    @Value("${coolsms.api.secret}")
    private String apiSecretKey;
    @Value("${coolsms.api.from}")
    private String fromPhoneNo;

    private DefaultMessageService messageService;

    @PostConstruct // 의존성 주입이 이루어진 후 초기화 작업을 수행
    private void init(){
        this.messageService = NurigoApp.INSTANCE.initialize(apiKey, apiSecretKey, "https://api.coolsms.co.kr");
    }

    // 단일 메시지 발송 예제
    public SingleMessageSentResponse sendSms(String to, String verificationCode) {
        System.out.println("SmsUtil|sendSms|시작 ===================> ");
        log.info("apiKey : {}, apiSecret : {}, apiFrom : {}", apiKey, apiSecretKey, fromPhoneNo);

        Message message = new Message();

        // 발신번호 및 수신번호는 반드시 01012345678 형태로 입력되어야 합니다.
        message.setFrom(fromPhoneNo);
        message.setTo(to);
        message.setText("[betWeen] 아래의 인증번호를 입력해주세요\n" + verificationCode);
        System.out.println("SmsUtil|sendSms|message: " + message);

        System.out.println("SmsUtil|sendSms|sendOne ===================> ");
        SingleMessageSentResponse response = this.messageService.sendOne(new SingleMessageSendingRequest(message));
        System.out.println("SmsUtil|sendSms|response = " + response);
        /* response 예시
            SingleMessageSentResponse(
                groupId=G4V20250402111513BZAWOBXUO5UFTE9, to=01012345678, from=01011112222,
                type=SMS, statusMessage=정상 접수(이통사로 접수 예정) , country=82,
                messageId=M4V20250402111513EXOYNXYVHNGW7UY, statusCode=2000, accountId=25040205571835
            )
        */
        return response;
    }

}
