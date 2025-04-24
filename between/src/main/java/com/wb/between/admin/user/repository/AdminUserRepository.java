package com.wb.between.admin.user.repository;

import com.wb.between.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/*
    동적 쿼리 생성을 위해 JpaSpecificationExecutor를 상속
        findAll(Specification<T> spec, Pageable pageable)과 같은 메소드를 사용할 수 있게 됨
        이 메소드는 동적으로 생성된 WHERE 절(Specification)과 페이징/정렬 정보(Pageable)를 사용하여 데이터를 조회
*/
public interface AdminUserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    // 사용자 이메일로 사용자 조회
    User findByEmail(String email);

    // 사용자 전화번호로 사용자 조회
    User findByPhoneNo(String phoneNo);

}
