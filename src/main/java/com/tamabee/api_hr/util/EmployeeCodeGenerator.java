package com.tamabee.api_hr.util;

import java.time.LocalDate;

import com.tamabee.api_hr.repository.user.UserRepository;

/**
 * Utility class để tạo mã nhân viên 8 số
 * - Admin (đăng ký công ty): yyyymmdd (ngày đăng ký)
 * - User thường: yyyy (năm đăng ký) + mm (tháng sinh) + dd (ngày sinh)
 * Nếu trùng thì năm + 1
 */
public class EmployeeCodeGenerator {

    /**
     * Tạo mã nhân viên cho admin: yyyymmdd (ngày đăng ký)
     * Nếu trùng thì tăng năm
     */
    public static String generateForAdmin(UserRepository userRepository) {
        LocalDate today = LocalDate.now();
        int year = today.getYear();
        String monthDay = String.format("%02d%02d", today.getMonthValue(), today.getDayOfMonth());
        
        for (int i = 0; i < 100; i++) {
            String employeeCode = String.valueOf(year + i) + monthDay;
            if (!userRepository.existsByEmployeeCodeAndDeletedFalse(employeeCode)) {
                return employeeCode;
            }
        }
        
        // Fallback: thêm random suffix (rất hiếm)
        return String.valueOf(year) + monthDay;
    }

    /**
     * Tạo mã nhân viên cho user thường: năm đăng ký + tháng sinh + ngày sinh
     * Nếu trùng thì năm + 1
     * 
     * @param dateOfBirth format yyyy-MM-dd hoặc dd/MM/yyyy
     * @param userRepository Repository để kiểm tra trùng mã
     * @return Mã nhân viên 8 số duy nhất
     */
    public static String generateForUser(String dateOfBirth, UserRepository userRepository) {
        LocalDate today = LocalDate.now();
        int year = today.getYear();
        
        // Parse ngày sinh
        String month = "01";
        String day = "01";
        
        if (dateOfBirth != null && !dateOfBirth.isEmpty()) {
            try {
                if (dateOfBirth.contains("-")) {
                    // Format yyyy-MM-dd
                    String[] parts = dateOfBirth.split("-");
                    if (parts.length >= 3) {
                        month = String.format("%02d", Integer.parseInt(parts[1]));
                        day = String.format("%02d", Integer.parseInt(parts[2]));
                    }
                } else if (dateOfBirth.contains("/")) {
                    // Format dd/MM/yyyy
                    String[] parts = dateOfBirth.split("/");
                    if (parts.length >= 2) {
                        day = String.format("%02d", Integer.parseInt(parts[0]));
                        month = String.format("%02d", Integer.parseInt(parts[1]));
                    }
                }
            } catch (NumberFormatException e) {
                // Giữ giá trị mặc định
            }
        }
        
        // Thử từ năm hiện tại, nếu trùng thì tăng năm
        for (int i = 0; i < 100; i++) {
            String employeeCode = String.valueOf(year + i) + month + day;
            if (!userRepository.existsByEmployeeCodeAndDeletedFalse(employeeCode)) {
                return employeeCode;
            }
        }
        
        // Fallback
        return String.valueOf(year) + month + day;
    }

    /**
     * Tạo mã nhân viên duy nhất (backward compatible)
     * @deprecated Dùng generateForAdmin() hoặc generateForUser() thay thế
     */
    @Deprecated
    public static String generateUnique(Long companyId, String dateOfBirth, UserRepository userRepository) {
        if (dateOfBirth == null || dateOfBirth.isEmpty()) {
            return generateForAdmin(userRepository);
        }
        return generateForUser(dateOfBirth, userRepository);
    }
}
