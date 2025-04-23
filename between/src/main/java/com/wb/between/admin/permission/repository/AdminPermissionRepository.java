package com.wb.between.admin.permission.repository;

import com.wb.between.admin.permission.domain.Permission;
import com.wb.between.coupon.domain.Coupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminPermissionRepository extends JpaRepository<Permission, Long> {

    @Query("SELECT p FROM Permission p WHERE p.permissionName LIKE CONCAT('%', :searchPermissionName, '%')")
    Page<Permission> findPermissionWithFilter(Pageable pageable,
                                       @Param("searchPermissionName") String searchPermissionName);
}
