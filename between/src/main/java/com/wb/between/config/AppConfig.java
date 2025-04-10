package com.wb.between.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration // 이 클래스가 Spring 설정 파일임을 알림
public class AppConfig { // 클래스 이름은 자유롭게 지정 가능

    @Bean // 이 메소드가 반환하는 객체를 Spring Bean으로 등록
    public RestTemplate restTemplate() {
        // RestTemplate 객체를 생성해서 반환
        RestTemplate restTemplate = new RestTemplate();

        // (선택 사항) 필요하다면 여기서 RestTemplate 설정을 추가할 수 있습니다.    // 예: 타임아웃 설정
        //        // org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        //        // factory.setConnectTimeout(5000); // 연결 타임아웃 5초
        //        // factory.setReadTimeout(5000);    // 읽기 타임아웃 5초
        //        // restTemplate.setRequestFactory(factory);


        return restTemplate;
    }

    // 다른 @Bean 설정들이 있을 수 있음...
}