package com.wb.between.menu.repository;

import com.wb.between.menu.domain.Menu;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.util.List;

@Repository
public interface MenuRepository extends JpaRepository<Menu, Long> {

    //사용하는 메뉴 조회, Sort 기준은 sortOrder 오름차순
    List<Menu> findByUseAt(String useAt, Sort sort);
}
