package com.wb.between.admin.popup.repository;

import com.wb.between.popup.domain.Popups;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminPopupRepository extends JpaRepository<Popups, Long> {

    @Query("SELECT p FROM Popups p WHERE p.title LIKE CONCAT('%', :searchPopupName, '%')")
    Page<Popups> findPopupWithFilter(Pageable pageable,
                                     @Param("searchPopupName") String searchPopupName);
}
