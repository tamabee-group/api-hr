package com.tamabee.api_hr.repository;

import com.tamabee.api_hr.entity.leave.HolidayEntity;
import com.tamabee.api_hr.enums.HolidayType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository quản lý ngày nghỉ lễ.
 */
@Repository
public interface HolidayRepository extends JpaRepository<HolidayEntity, Long> {

        /**
         * Tìm ngày nghỉ theo ID (chưa bị xóa)
         */
        Optional<HolidayEntity> findByIdAndDeletedFalse(Long id);

        /**
         * Lấy danh sách ngày nghỉ của công ty (phân trang)
         */
        Page<HolidayEntity> findByCompanyIdAndDeletedFalse(Long companyId, Pageable pageable);

        /**
         * Lấy danh sách ngày nghỉ của công ty trong khoảng thời gian (phân trang)
         */
        @Query("SELECT h FROM HolidayEntity h WHERE h.deleted = false " +
                        "AND h.companyId = :companyId " +
                        "AND h.date BETWEEN :startDate AND :endDate")
        Page<HolidayEntity> findByCompanyIdAndDateBetweenAndDeletedFalse(
                        @Param("companyId") Long companyId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        Pageable pageable);

        /**
         * Lấy danh sách ngày nghỉ của công ty trong khoảng thời gian
         */
        @Query("SELECT h FROM HolidayEntity h WHERE h.deleted = false " +
                        "AND h.companyId = :companyId " +
                        "AND h.date BETWEEN :startDate AND :endDate " +
                        "ORDER BY h.date ASC")
        List<HolidayEntity> findByCompanyIdAndDateBetween(
                        @Param("companyId") Long companyId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        /**
         * Lấy danh sách ngày lễ quốc gia trong khoảng thời gian
         */
        @Query("SELECT h FROM HolidayEntity h WHERE h.deleted = false " +
                        "AND h.companyId IS NULL " +
                        "AND h.type = 'NATIONAL' " +
                        "AND h.date BETWEEN :startDate AND :endDate " +
                        "ORDER BY h.date ASC")
        List<HolidayEntity> findNationalHolidaysByDateBetween(
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        /**
         * Lấy tất cả ngày nghỉ (công ty + quốc gia) trong khoảng thời gian
         */
        @Query("SELECT h FROM HolidayEntity h WHERE h.deleted = false " +
                        "AND (h.companyId = :companyId OR h.companyId IS NULL) " +
                        "AND h.date BETWEEN :startDate AND :endDate " +
                        "ORDER BY h.date ASC")
        List<HolidayEntity> findAllHolidaysByCompanyIdAndDateBetween(
                        @Param("companyId") Long companyId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        /**
         * Kiểm tra ngày có phải ngày nghỉ không (công ty hoặc quốc gia)
         */
        @Query("SELECT COUNT(h) > 0 FROM HolidayEntity h WHERE h.deleted = false " +
                        "AND (h.companyId = :companyId OR h.companyId IS NULL) " +
                        "AND h.date = :date")
        boolean isHoliday(@Param("companyId") Long companyId, @Param("date") LocalDate date);

        /**
         * Kiểm tra ngày nghỉ đã tồn tại cho công ty chưa
         */
        boolean existsByCompanyIdAndDateAndDeletedFalse(Long companyId, LocalDate date);

        /**
         * Lấy danh sách ngày nghỉ theo loại
         */
        @Query("SELECT h FROM HolidayEntity h WHERE h.deleted = false " +
                        "AND h.companyId = :companyId " +
                        "AND h.type = :type " +
                        "ORDER BY h.date ASC")
        List<HolidayEntity> findByCompanyIdAndType(
                        @Param("companyId") Long companyId,
                        @Param("type") HolidayType type);
}
