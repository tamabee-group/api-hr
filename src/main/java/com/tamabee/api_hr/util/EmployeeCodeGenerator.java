package com.tamabee.api_hr.util;

import com.tamabee.api_hr.repository.user.UserRepository;

/**
 * Utility class để tạo mã nhân viên
 * Format: prefix (25-99) + 2 số cuối companyId + 2 số ngày sinh + 2 số tháng
 * sinh (8 số)
 * Ví dụ: companyId=5, ngày sinh 15/03 -> 25050315
 * Nếu trùng thì tăng prefix: 26050315, 27050315, ...
 */
public class EmployeeCodeGenerator {

    private static final int START_PREFIX = 25;
    private static final int MAX_PREFIX = 99;

    /**
     * Tạo mã nhân viên duy nhất từ companyId và ngày sinh
     * 
     * @param companyId      ID của công ty
     * @param dateOfBirth    Ngày sinh format "yyyy-MM-dd" hoặc "dd/MM/yyyy"
     * @param userRepository Repository để kiểm tra trùng mã
     * @return Mã nhân viên 8 số duy nhất
     */
    public static String generateUnique(Long companyId, String dateOfBirth, UserRepository userRepository) {
        String baseCode = generateBaseCode(companyId, dateOfBirth);

        // Thử từ prefix 25 đến 99
        for (int prefix = START_PREFIX; prefix <= MAX_PREFIX; prefix++) {
            String employeeCode = String.format("%02d", prefix) + baseCode;
            if (!userRepository.existsByEmployeeCodeAndDeletedFalse(employeeCode)) {
                return employeeCode;
            }
        }

        // Nếu hết prefix (rất hiếm), thêm timestamp
        return START_PREFIX + baseCode + System.currentTimeMillis() % 1000;
    }

    /**
     * Tạo mã nhân viên cơ bản (không kiểm tra trùng)
     * Dùng cho trường hợp đã biết chắc không trùng
     */
    public static String generate(Long companyId, String dateOfBirth) {
        return START_PREFIX + generateBaseCode(companyId, dateOfBirth);
    }

    /**
     * Tạo phần base code: 2 số cuối companyId + ngày + tháng
     */
    private static String generateBaseCode(Long companyId, String dateOfBirth) {
        // Lấy 2 số cuối của companyId (nếu < 10 thì thêm 0 phía trước)
        String companyPart = String.format("%02d", companyId % 100);

        // Parse ngày sinh
        String day = "01";
        String month = "01";

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
                // Giữ giá trị mặc định nếu parse lỗi
            }
        }

        // Format: companyId (2 số) + ngày (2 số) + tháng (2 số)
        return companyPart + day + month;
    }
}
