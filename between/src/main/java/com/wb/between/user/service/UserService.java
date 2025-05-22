package com.wb.between.user.service;

import com.wb.between.admin.role.domain.Role;
import com.wb.between.common.util.SmsUtil;
import com.wb.between.role.repository.RoleRepository;
import com.wb.between.user.domain.User;
import com.wb.between.user.dto.SignupReqDto;
import com.wb.between.user.dto.VerificationResult;
import com.wb.between.user.repository.UserRepository;
import com.wb.between.userrole.domain.UserRole;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Enumeration;
import java.util.Optional;
import java.util.Random;

@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SmsUtil smsUtil;
    private final RoleRepository roleRepository;

    // 인증번호
    private static final String OTP_PREFIX = "OTP_";
    private static final int EXPIRATION_TIME = 180; // 3분

    // 이메일 인증번호
    private final JavaMailSender mailSender;
    @Value("${spring.mail.username}") // application.properties/yml 값 주입
    private String fromEmail;

    // 기본 역할 코드 상수 정의 (설정 파일 등으로 관리하는 것이 더 좋음)
    private static final String DEFAULT_ROLE_CODE = "ROLE_USER"; // 일반 사용자 역할 코드
    private static final String STAFF_ROLE_CODE = "ROLE_STAFF";   // 임직원 역할 코드 (예시)

    // 생성자 주입
    @Autowired
    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder, SmsUtil smsUtil, JavaMailSender mailSender) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.smsUtil = smsUtil;
        this.mailSender = mailSender;
    }

    // 회원가입
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

                // 1. 부여할 역할 코드 결정
        String targetRoleCode = DEFAULT_ROLE_CODE; // 기본값: 일반 사용자
//        String email = signupReqDto.getEmail();
        if (email != null && email.endsWith("@winbit.kr")) { // 도메인 체크 (null 체크 추가)
            targetRoleCode = STAFF_ROLE_CODE; // 특정 도메인이면 임직원 역할
        }

//        // 2. 역할(Role) 엔티티 조회
//        // 시스템에 기본 역할('ROLE_USER', 'ROLE_STAFF' 등)은 반드시 미리 등록되어 있어야 함
        Role assignedRole = roleRepository.findByRoleCode(targetRoleCode)
                .orElseThrow(() -> new IllegalStateException("역할을 찾을 수 없습니다. 시스템 설정 오류입니다."));
//        // 실제로는 RoleNotFoundException 같은 커스텀 예외 처리 권장

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

        // 3) UserRole 조인 엔티티 생성 & 연결
        UserRole ur = new UserRole();
        ur.setUser(user);               // 양방향이라면
        ur.setRole(assignedRole);
        // 만약 UserRole에 복합키나 빌더가 있으면 적절히 세팅

        // 4) User 쪽 컬렉션에 추가
        user.getUserRole().add(ur);

        // 저장 및 반환
        return userRepository.save(user);
    }

    // 회원가입 내 이메일중복 확인
    @Transactional(readOnly = true)
    public boolean checkEmail(String email) {
//        return !userRepository.existsByEmail(email);
        return !userRepository.checkEmail(email);
    }

    /**
     * 입력된 휴대폰 번호가 이미 시스템에 등록되어 있는지 확인합니다.
     * @param phoneNo (하이픈 포함 가능, 내부에서 제거)
     * @return 중복이면 true, 아니면 false
     */
    @Transactional(readOnly = true)
    public boolean isPhoneNumberDuplicated(String phoneNo) {
        log.info("UserService|isPhoneNumberDuplicated|휴대폰 번호 중복 확인 시작 ==========> phoneNo : " + phoneNo);

        String cleanPhoneNo = phoneNo.replaceAll("-", "");

//        userRepository.findByPhoneNo(cleanPhoneNo).isPresent(); // findByPhoneNo로 조회하면 JPA가 암묵적으로 최대 하나의 결과만 조회하도록 처리하므로 사용X
        boolean isDuplicated = userRepository.existsByPhoneNo(cleanPhoneNo);
        log.info("UserService|isPhoneNumberDuplicated|isDuplicated : " + isDuplicated);

        return isDuplicated;
    }

    // 휴대폰 인증번호 생성 및 SMS전송(API연동)
    @Transactional
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
        smsUtil.sendSms(phoneNo, code); // 인증번호 SMS 발송 시 건당 20원 발생으로 인해 주석처리

        return code;

    }

    // 회원가입 > 휴대폰 번호 인증 검증
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


    /**
     * [비밀번호 찾기 1단계] 이메일 존재 확인 및 OTP 발송 요청 처리
     * @param email 사용자 이메일
     * @param session HttpSession
     * @return 성공 여부 (사용자 존재 및 메일 발송 성공 시 true)
     */
    public boolean requestPasswordOtp(String email, HttpSession session) {

        // 이메일로 회원정보 조회
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isEmpty()) {
            System.out.println("UserService|requestPasswordOtp| 사용자를 찾을 수 없음: " + email);
            return false;
        }

        // 인증번호 생성
        String code = String.format("%06d", new Random().nextInt(1000000));
        System.out.println("UserService|requestPasswordOtp| 생성된 OTP: " + code);

        try {

            // 이메일 발송
            sendOtpEmail(email, code);
            System.out.println("UserService|requestPasswordOtp| 이메일 발송 성공: " + email);

            // 세션에 인증번호 및 만료 시간 저장
            long expiryTimeMillis = System.currentTimeMillis() + (EXPIRATION_TIME * 1000);
            String sessionKey = OTP_PREFIX + email;
            String expiryKey = sessionKey + "_expiry";
            session.setAttribute(sessionKey, code);
            session.setAttribute(expiryKey, expiryTimeMillis);

            System.out.println("UserService|requestPasswordOtp| 세션 저장 완료 - Key(Code): " + sessionKey + ", Key(Expiry): " + expiryKey);
            return true;

        } catch (MailException e) {
            System.err.println("UserService|requestPasswordOtp| 이메일 발송 실패: " + email);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 이메일 발송
     */
    private void sendOtpEmail(String toEmail, String otp) throws MailException {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setFrom(fromEmail);
        message.setSubject("[Between] 비밀번호 찾기 인증번호 안내");
        message.setText("요청하신 비밀번호 찾기 인증번호는 [" + otp + "] 입니다.\n" +
                "이 인증번호는 " + (EXPIRATION_TIME / 60) + "분 동안 유효합니다.");
        mailSender.send(message);
    }

    /**
     * [비밀번호 찾기 2단계] 이메일로 받은 OTP 검증
     * @param email 사용자 이메일
     * @param code 입력받은 OTP 코드
     * @param session HttpSession
     * @return OTP 유효 여부 (true/false)
     */
    public boolean verifyPasswordOtp(String email, String code, HttpSession session) {

        System.out.println("UserService|verifyPasswordOtp| ===============> 시작");
        String sessionKey = OTP_PREFIX + email;
        String expiryKey = sessionKey + "_expiry";

        String storedCode = (String) session.getAttribute(sessionKey);
        Long expiryTimeMillis = (Long) session.getAttribute(expiryKey);

        System.out.println("UserService|verifyPasswordOtp|session|storedCode :  " + storedCode);
        System.out.println("UserService|verifyPasswordOtp|session|inputCode :  " + code);
        System.out.println("UserService|verifyPasswordOtp|session|expiryTime : " + expiryTimeMillis);
        System.out.println("UserService|verifyPasswordOtp|currentTime : " + System.currentTimeMillis());

        boolean isValid = (
                storedCode != null && expiryTimeMillis != null &&
                System.currentTimeMillis() < expiryTimeMillis &&
                storedCode.equals(code)
        );

        System.out.println("UserService|verifyPasswordOtp| isValid: " + isValid);

        // 검증 시도 후 관련 세션 정보 제거
        if(isValid) {
            System.out.println("UserService|verifyPasswordOtp| 인증번호 검증 성공");
            session.removeAttribute(sessionKey);
            session.removeAttribute(expiryKey);
            System.out.println("UserService|verifyPasswordOtp| 세션 속성 제거 완료: " + sessionKey);
        }

        return isValid;
    }

    /**
     * [비밀번호 찾기 3단계] 비밀번호 재설정
     * @param email 대상 사용자 이메일
     * @param newPassword 새 비밀번호 (평문)
     * @return 성공 여부
     */
    @Transactional
    public boolean resetPassword(String email, String newPassword) {
        System.out.println("UserService|resetPassword| ===============> 시작 | email: " + email);

        // 이메일로 회원정보 조회
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            System.err.println("UserService|resetPassword| 사용자를 찾을 수 없음: " + email);
            return false;
        }

        // 새 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(newPassword);
        System.out.println("UserService|resetPassword| 새 비밀번호 해싱 완료");

        User user = userOptional.get();
        user.setPassword(encodedPassword); // User 엔티티에 setPassword 필요

        try {
            userRepository.save(user);
            System.out.println("UserService|resetPassword| 비밀번호 변경 및 저장 성공 | email: " + email);
            return true;
        } catch (Exception e) {
            System.err.println("UserService|resetPassword| DB 저장 중 오류 발생 | email: " + email);
            e.printStackTrace();
            return false;
        }
    }


}
