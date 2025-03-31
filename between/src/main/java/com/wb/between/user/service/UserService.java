package com.wb.between.user.service;

import com.wb.between.user.domain.User;
import com.wb.between.user.dto.SignupRequest;
import com.wb.between.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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
    public User registerUser(SignupRequest signupRequest) {

        // 이메일 중복 확인
        if (!checkEmail(signupRequest.getEmail())) {
            throw new RuntimeException("이미 사용 중인 이메일입니다.");
        }

        // 비밀번호 암호화
        System.out.println("UserService|registerUser|비밀번호 암호화 전|signupRequest.getPassword() = " + signupRequest.getPassword());
        String encodedPassword = passwordEncoder.encode(signupRequest.getPassword());
        System.out.println("UserService|registerUser|비밀번호 암호화 후|encodedPassword = " + encodedPassword);

        // User 엔티티 생성
        User user = User.builder()
                .email(signupRequest.getEmail())
                .password(encodedPassword)
                .name(signupRequest.getName())
                .phoneNo(signupRequest.getPhoneNo().replaceAll("-", "")) // 하이픈 제거
                .userStts("일반")
                .authCd("일반")
                .loginM("일반")
                .build();

        // 저장 및 반환
        return userRepository.save(user);
    }

    public String generateAndSendVerificationCode(String phoneNumber) {
        // 실제 구현에서는 SMS 서비스를 통해 인증번호를 발송
        // 여기서는 간단히 랜덤 4자리 숫자 생성
        int code = (int)(Math.random() * 9000) + 1000;
        return String.valueOf(code);
    }



}
