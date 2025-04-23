package com.wb.between.admin.user.repository;

import com.wb.between.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminUserRepository extends JpaRepository<User, Long> {



}
