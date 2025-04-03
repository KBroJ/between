package com.wb.between.user.service;

import com.wb.between.common.util.SmsUtil;
import com.wb.between.user.domain.User;
import com.wb.between.user.dto.SignupReqDto;
import com.wb.between.user.dto.VerificationResult;
import com.wb.between.user.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Enumeration;
import java.util.Optional;
import java.util.Random;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SmsUtil smsUtil;

    // 휴대폰 인증번호
    private static final String OTP_PREFIX = "OTP_";
    private static final int EXPIRATION_TIME = 180; // 3분

    // 생성자 주입
    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, SmsUtil smsUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.smsUtil = smsUtil;
    }

// 1. 회원가입
    @Transactional  // 트랜잭션 처리
    public User registerUser(SignupReqDto signupReqDto) {
        System.out.println("UserService|registerUser|회원가입|시작 ==========> ");

        // 이메일 중복 확인
        if (!checkEmail(signupReqDto.getEmail())) {
            throw new RuntimeException("이미 사용 중인 이메일입니다.");
        }

        // authCd 값 설정 (기본값: 일반)
        String authCd = "일반";
        // 이메일 도메인 확인
        String email = signupReqDto.getEmail();
        String domain = email.substring(email.indexOf("@") + 1); // @ 뒤의 도메인 추출
        if ("winbit.kr".equalsIgnoreCase(domain)) {
            authCd = "임직원";
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
                .authCd(authCd)
                .loginM("일반")
                .build();

        // 저장 및 반환
        return userRepository.save(user);
    }

// 2. 회원가입 내 이메일중복 확인
    public boolean checkEmail(String email) {
//        return !userRepository.existsByEmail(email);
        return !userRepository.checkEmail(email);
    }

// 3. 휴대폰 인증번호 생성 및 SMS전송(API연동)
    public String generateAndSendVerificationCode(HttpSession session, String phoneNo) {
        System.out.println("UserService|generateAndSendVerificationCode|시작 ==========> phoneNo : " + phoneNo);

//        int code = (int)(Math.random() * 9000) + 1000; // 랜덤 4자리 숫자 생성
        // 6자리 랜덤 숫자 생성
        String code = String.format("%06d", new Random().nextInt(1000000));
        System.out.println("UserService|generateAndSendVerificationCode|인증번호 6자리 생성 :  " + code);

        // 세션에 저장
        session.setAttribute(OTP_PREFIX + phoneNo, code);    // 세션에 인증번호 저장(OTP_휴대폰번호(숫자만) : 인증번호)

        // 세션 유효시간 설정
        long expiryTimeMillis = System.currentTimeMillis() + (EXPIRATION_TIME * 1000); // 만료 시간 계산 (현재 시각 + 3분)
        session.setAttribute(OTP_PREFIX + phoneNo + "_expiry", expiryTimeMillis);   // 만료 시간 저장 (별도 키 사용)

        System.out.println("키: " + OTP_PREFIX + phoneNo + "_expiry" + ", 값: " + expiryTimeMillis + " (Timestamp)");

            System.out.println("================ 세션에 저장된 정보 ================");
            Enumeration<String> attributeNames = session.getAttributeNames();
            while (attributeNames.hasMoreElements()) {
                String attributeName = attributeNames.nextElement();
                Object attributeValue = session.getAttribute(attributeName);
                System.out.println("키: " + attributeName + ", 값: " + attributeValue);
            }
            System.out.println("================================================");

        System.out.println("UserService|generateAndSendVerificationCode|session|인증번호 요청 휴대폰번호 :  " + session.getAttribute(OTP_PREFIX + phoneNo));

        // 인증번호 SMS 발송 - coolSMS API 연동
//        smsUtil.sendSms(phoneNo, code); // 인증번호 SMS 발송 시 건당 20원 발생으로 인해 주석처리

        return code;

    }

// 4. 회원가입 > 휴대폰 번호 인증 검증
    public boolean verifyCode(HttpSession session, String phoneNo, String code) {
        System.out.println("UserService|verifyCode| ===============> 시작" );

        String storedCode = (String) session.getAttribute(OTP_PREFIX + phoneNo);                // 세션에 저장되어 있던 인증번호 가져오기
        Long expiryTimeMillis = (Long) session.getAttribute(OTP_PREFIX + phoneNo + "_expiry");  // 만료 시간 가져오기

        System.out.println("UserService|verifyCode|session|storedCode :  " + storedCode);
        System.out.println("UserService|verifyCode|session|inputCode :  " + code);

        boolean isValid =
                (
                    storedCode != null && expiryTimeMillis != null &&   // 코드, 만료시간 존재 여부
                    System.currentTimeMillis() < expiryTimeMillis &&    // 만료 시간 이전인지
                    storedCode.equals(code)                             // 코드 일치 여부
                );
        System.out.println("UserService|verifyCode|isValid : " + isValid);

        return isValid; // 검증 결과만 반환
    }

// 5. 회원정보찾기 > 이메일 찾기 : 휴대번호로 인증 후 이메일 조회
    public VerificationResult verifyAndGetUserByPhone(HttpSession session, String phoneNo, String code) { // 새 메서드
        System.out.println("UserService|verifyAndGetUserByPhone| ===============> 시작" );

        // 1. 인증번호 검증
        boolean isValid = verifyCode(session, phoneNo, code); // 내부적으로 로그 출력됨

        if (isValid) {
            // 2. 인증 성공: 세션에 저장된 인증번호 제거
            session.removeAttribute(OTP_PREFIX + phoneNo);              // 인증번호 세션 제거
            session.removeAttribute(OTP_PREFIX + phoneNo + "_expiry");  // 인증번호 만료 시간 세션 제거

                    System.out.println("========== 세션에 저장된 인증번호 삭제 확인 ==========");
                    Enumeration<String> attributeNames = session.getAttributeNames();
                    while (attributeNames.hasMoreElements()) {
                        String attributeName = attributeNames.nextElement();
                        Object attributeValue = session.getAttribute(attributeName);
                        System.out.println("키: " + attributeName + ", 값: " + attributeValue);
                    }
                    System.out.println("=================================================");

            // 3. DB에서 해당 전화번호 사용자 조회
            Optional<User> userOptional = userRepository.findByPhoneNo(phoneNo);

            // 사용자 정보가 존재하는 경우
            if(userOptional.isPresent()) {

                User foundUser = userOptional.get();
                System.out.println("UserService|verifyAndGetUserByPhone| DB 사용자 조회 성공: " + foundUser.getEmail());
                return VerificationResult.success(foundUser.getEmail());

            }
            // 사용자가 없는 경우
            else {
                System.out.println("UserService|verifyAndGetUserByPhone| 인증 성공했으나 DB 사용자 조회 실패 (phoneNo: " + phoneNo + ")");
//                return Optional.empty(); // 빈 Optional 반환
                return VerificationResult.userNotFound(); // 사용자 없음 결과 반환
            }
        }
        // 인증번호 검증 실패
        else {
            System.out.println("UserService|verifyAndGetUserByPhone| 인증 실패");
            return VerificationResult.otpInvalid();
        }
    }

}
