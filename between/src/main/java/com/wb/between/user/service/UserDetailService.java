package com.wb.between.user.service;


import com.wb.between.admin.role.domain.Role;
import com.wb.between.admin.rolepermission.domain.RolePermission;
import com.wb.between.user.domain.User;
import com.wb.between.user.repository.UserRepository;
import com.wb.between.userrole.domain.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserDetailService implements UserDetailsService {

//    private final UserRepositoryBM userRepositoryBM;
    private final UserRepository userRepositoryBM;

    @Override
//    public UserBM loadUserByUsername(String email){
    public User loadUserByUsername(String email){

        User user = userRepositoryBM.findByUsernameWithRolesAndPermissions(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다. - " + email));

        log.debug("user with Role => {}", user);

        log.debug("user with Role|getUserRole => {}", user.getUserRole());

        for(UserRole userRole : user.getUserRole()) {
            log.debug("role => {}", userRole.getRole());
            log.debug("getRolePermissions => {}", userRole.getRole().getRolePermissions());
           for(RolePermission rp : userRole.getRole().getRolePermissions()) {
               log.debug("rp.permission => {}", rp.getPermission());
           }
        }


//        return userRepositoryBM.findByEmail(email)
//                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다. - " + email));

        return user;
    }
}
