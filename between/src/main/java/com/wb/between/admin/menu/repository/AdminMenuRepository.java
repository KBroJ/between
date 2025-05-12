package com.wb.between.admin.menu.repository;

import com.wb.between.banner.domain.Banner;
import com.wb.between.menu.domain.Menu;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminMenuRepository extends JpaRepository<Menu, Long> {

    /**
     * 메뉴 타입 조회
     * @return
     */
    @Query("SELECT DISTINCT m.menuType FROM Menu m")
    List<String> findDistinctByMenuType();

    /**
     * 메뉴 타입 조회
     * @return
     */
    @Query("SELECT m FROM Menu m WHERE m.menuType = :id")
    List<Menu> findDistinctByMenuType(@Param("id") String id);

    /**
     * 메뉴 단일 조회
     * @param menuNo
     * @return
     */
    @Query("SELECT m FROM Menu m " +
            "LEFT JOIN FETCH m.menuRoles mr " +
            "WHERE m.menuNo = :menuNo")
    Optional<Menu> findByMenuNo(@Param("menuNo") Long menuNo);

}
