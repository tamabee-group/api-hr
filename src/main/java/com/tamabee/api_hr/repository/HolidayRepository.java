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
         * Lấy danh sách ngày nghỉ (phân trang)
         */
        Page<HolidayEntity> findByDeletedFalse(Pageable pageable);

        /**
         * Lấy danh sách ngày nghỉ trong khoảng thời gian (phân trang)
         */
        @Query("SELECT h FROM HolidayEntity h WHERE h.deleted = false " +
                        "AND h.date BETWEEN :startDate AND :endDate")
        Page<HolidayEntity> findByDateBetweenAndDeletedFalse(
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        Pageable pageable);

        /**
         * Lấy danh sách ngày nghỉ trong khoảng thời gian
         */
        @Query("SELECT h FROM HolidayEntity h WHERE h.deleted = false " +
                        "AND h.date BETWEEN :startDate AND :endDate " +
                        "ORDER BY h.date ASC")
        List<HolidayEntity> findByDateBetween(
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        /**
         * Lấy danh sách ngày lễ quốc gia trong khoảng thời gian
         */
        @Query("SELECT h FROM HolidayEntity h WHERE h.deleted = false " +
                        "AND h.type = 'NATIONAL' " +
                        "AND h.date BETWEEN :startDate AND :endDate " +
                        "ORDER BY h.date ASC")
        List<HolidayEntity> findNationalHolidaysByDateBetween(
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        /**
         * Kiểm tra ngày có phải ngày nghỉ không
         */
        @Query("SELECT COUNT(h) > 0 FROM HolidayEntity h WHERE h.deleted = false " +
                        "AND h.date = :date")
        boolean isHoliday(@Param("date") LocalDate date);

        /**
         * Kiểm tra ngày nghỉ đã tồn tại chưa
         */
        boolean existsByDateAndDeletedFalse(LocalDate date);

        /**
         * Lấy danh sách ngày nghỉ trong khoảng thời gian (phân trang) - alias cho
         * service cũ
         */
        @Query("SELECT h FROM HolidayEntity h WHERE h.deleted = false " +
                        "AND h.date BETWEEN :startDate AND :endDate")
        Page<HolidayEntity> findByDateBetweenAndDeletedFalsePageable(
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        Pageable pageable);

        /**
         * Lấy danh sách ngày nghỉ theo loại
         */
        @Query("SELECT h FROM HolidayEntity h WHERE h.deleted = false " +
                        "AND h.type = :type " +
                        "ORDER BY h.date ASC")
        List<HolidayEntity> findByType(@Param("type") HolidayType type);
}
