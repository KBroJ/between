package com.wb.between.user.service;

import com.wb.between.user.domain.User;
import com.wb.between.user.dto.SignupReqDto;
import com.wb.between.user.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Enumeration;
import java.util.Random;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 휴대폰 인증번호
    private static final String OTP_PREFIX = "OTP_";
    private static final int EXPIRATION_TIME = 180; // 3분

    // 생성자 주입
    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // 회원가입 내 이메일중복 확인
    public boolean checkEmail(String email) {
//        return !userRepository.existsByEmail(email);
        return !userRepository.checkEmail(email);
    }

    @Transactional  // 트랜잭션 처리
    public User registerUser(SignupReqDto signupReqDto) {

        // 이메일 중복 확인
        if (!checkEmail(signupReqDto.getEmail())) {
            throw new RuntimeException("이미 사용 중인 이메일입니다.");
        }

        // 비밀번호 암호화
        System.out.println("UserService|registerUser|비밀번호 암호화 전|signupRequest.getPassword() = " + signupReqDto.getPassword());
        String encodedPassword = passwordEncoder.encode(signupReqDto.getPassword());
        System.out.println("UserService|registerUser|비밀번호 암호화 후|encodedPassword = " + encodedPassword);

        // User 엔티티 생성(DB 저장을 위한 객체)
        User user = User.builder()
                .email(signupReqDto.getEmail())
                .password(encodedPassword)
                .name(signupReqDto.getName())
                .phoneNo(signupReqDto.getPhoneNo().replaceAll("-", "")) // 하이픈 제거
                .userStts("일반")
                .authCd("일반")
                .loginM("일반")
                .build();

        // 저장 및 반환
        return userRepository.save(user);
    }

    // 휴대폰 인증번호 생성 및 SMS전송(API연동)
    public String generateAndSendVerificationCode(HttpSession session, String phoneNo) {
        System.out.println("UserService|generateAndSendVerificationCode|시작 ==========> phoneNo : " + phoneNo);

    /*
        // 실제 구현에서는 SMS 서비스를 통해 인증번호를 발송
        int code = (int)(Math.random() * 9000) + 1000; // 랜덤 4자리 숫자 생성
        return String.valueOf(code);
     */

        // 6자리 랜덤 숫자 생성
        String code = String.format("%06d", new Random().nextInt(1000000));
        System.out.println("UserService|generateAndSendVerificationCode|인증번호 6자리 생성 :  " + code);

        // 세션에 저장
        session.setAttribute(OTP_PREFIX + phoneNo, code);    // 세션에 인증번호 저장
        session.setMaxInactiveInterval(EXPIRATION_TIME);        // 세션 유효시간 설정

        System.out.println("================ 세션에 저장된 정보 ================");
        Enumeration<String> attributeNames = session.getAttributeNames();
        while (attributeNames.hasMoreElements()) {
            String attributeName = attributeNames.nextElement();
            Object attributeValue = session.getAttribute(attributeName);
            System.out.println("키: " + attributeName + ", 값: " + attributeValue);
        }
        System.out.println("================================================");
        System.out.println("UserService|generateAndSendVerificationCode|session|인증번호 요청 휴대폰번호 :  " + session.getAttribute(OTP_PREFIX + phoneNo));

        // 인증번호 SMS 발송 - 작업필요(API)
        // sendSMS(phoneNumber, code);

        return code;

    }

    public boolean verifyCode(HttpSession session, String phoneNumber, String code) {

        String storedCode = (String) session.getAttribute(OTP_PREFIX + phoneNumber);

        if (storedCode != null && storedCode.equals(code)) {
            // 세션에 저장된 인증번호 삭제
            session.removeAttribute(OTP_PREFIX + phoneNumber);
            return true;
        }

        return false;
    }



}
