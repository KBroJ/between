package com.wb.between.user.service;


import com.wb.between.user.domain.User;
import com.wb.between.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class UserDetailService implements UserDetailsService {

//    private final UserRepositoryBM userRepositoryBM;
    private final UserRepository userRepositoryBM;

    @Override
//    public UserBM loadUserByUsername(String email){
    public User loadUserByUsername(String email){
        return userRepositoryBM.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다. - " + email));
    }
}
