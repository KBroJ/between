package com.wb.between.admin.popup.repository;

import com.wb.between.popup.domain.Popups;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminPopupRepository extends JpaRepository<Popups, Long> {
}
