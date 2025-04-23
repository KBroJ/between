package com.wb.between.admin.repository;

import com.wb.between.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminRepository extends JpaRepository<User, Long> {



}
