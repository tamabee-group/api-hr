package com.tamabee.api_hr.scheduler;

import com.tamabee.api_hr.entity.contract.EmploymentContractEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.ContractStatus;
import com.tamabee.api_hr.enums.UserStatus;
import com.tamabee.api_hr.repository.EmploymentContractRepository;
import com.tamabee.api_hr.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Scheduled job để xử lý hợp đồng hết hạn
 * Chạy vào 00:30 mỗi ngày để kiểm tra và cập nhật trạng thái
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ContractExpiryScheduler {

    private final EmploymentContractRepository contractRepository;
    private final UserRepository userRepository;

    /**
     * Xử lý hợp đồng hết hạn
     * Chạy vào 00:30 mỗi ngày (cron: giây phút giờ ngày tháng thứ)
     * - Cập nhật trạng thái hợp đồng từ ACTIVE sang EXPIRED
     * - Cập nhật trạng thái nhân viên sang INACTIVE nếu không có hợp đồng mới
     */
    @Scheduled(cron = "0 30 0 * * *")
    @Transactional
    public void processExpiredContracts() {
        log.info("=== BẮT ĐẦU SCHEDULED JOB: Contract Expiry Check ===");
        try {
            LocalDate today = LocalDate.now();

            // Tìm các hợp đồng đã hết hạn nhưng vẫn đang ACTIVE
            List<EmploymentContractEntity> expiredContracts = contractRepository.findExpiredActiveContracts(today);

            log.info("Tìm thấy {} hợp đồng đã hết hạn cần xử lý", expiredContracts.size());

            int contractsUpdated = 0;
            int employeesDeactivated = 0;

            for (EmploymentContractEntity contract : expiredContracts) {
                // Cập nhật trạng thái hợp đồng
                contract.setStatus(ContractStatus.EXPIRED);
                contractRepository.save(contract);
                contractsUpdated++;

                // Kiểm tra xem nhân viên có hợp đồng active khác không
                boolean hasActiveContract = contractRepository.hasActiveContract(contract.getEmployeeId(), today);

                if (!hasActiveContract) {
                    // Cập nhật trạng thái nhân viên sang INACTIVE
                    userRepository.findById(contract.getEmployeeId()).ifPresent(employee -> {
                        if (employee.getStatus() == UserStatus.ACTIVE) {
                            employee.setStatus(UserStatus.INACTIVE);
                            userRepository.save(employee);
                            log.info("Đã cập nhật nhân viên {} sang INACTIVE do hết hợp đồng",
                                    employee.getEmployeeCode());
                        }
                    });
                    employeesDeactivated++;
                }
            }

            log.info("=== KẾT THÚC SCHEDULED JOB: Contract Expiry Check - THÀNH CÔNG ===");
            log.info("Đã cập nhật {} hợp đồng, {} nhân viên chuyển sang INACTIVE",
                    contractsUpdated, employeesDeactivated);

        } catch (Exception e) {
            log.error("=== KẾT THÚC SCHEDULED JOB: Contract Expiry Check - LỖI: {} ===", e.getMessage(), e);
        }
    }
}
