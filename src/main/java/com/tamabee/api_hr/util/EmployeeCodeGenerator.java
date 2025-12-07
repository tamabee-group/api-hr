package com.tamabee.api_hr.util;

public class EmployeeCodeGenerator {
    
    /**
     * Tạo mã nhân viên từ companyId và số thứ tự nhân viên trong company
     * - Tamabee (companyId = 0): 2025 + 2 số tăng dần (ví dụ: 202501, 202502, 202525)
     * - Company khác: companyId + 3 số tăng dần (ví dụ: companyId=5 -> 5001, 5002)
     */
    public static String generate(Long companyId, Long employeeSequence) {
        if (companyId == 0) {
            // Tamabee: 2025 + 2 số
            return String.format("2025%02d", employeeSequence);
        } else {
            // Company: companyId + 3 số
            return String.format("%d%03d", companyId, employeeSequence);
        }
    }
}
