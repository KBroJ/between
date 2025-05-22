package com.wb.between.menurole.repository;

import com.wb.between.menurole.domain.MenuRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MenuRoleRepository extends JpaRepository<MenuRole, Long> {

    @Modifying
    @Query("DELETE FROM MenuRole mr WHERE mr.menu.menuNo = :menuNo")
    void deleteByRoleByMenuNo(@Param("menuNo") Long menuNo);

}
