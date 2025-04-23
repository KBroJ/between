package com.wb.between.admin.role.repository;

import com.wb.between.admin.role.domain.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminRoleRepository extends JpaRepository<Role, Long> {

    @Query("SELECT c FROM Role c WHERE c.roleName LIKE CONCAT('%', :searchRoleName, '%')")
    Page<Role> findRoleWithFilter(Pageable pageable,
                                       @Param("searchRoleName") String searchRoleName);
}
