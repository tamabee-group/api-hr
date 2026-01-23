-- =====================================================
-- TEST DATA FOR PAYROLL PERIODS
-- Tạo dữ liệu test cho kỳ lương
-- =====================================================

-- Xóa dữ liệu cũ (theo thứ tự foreign key)
DELETE FROM payroll_items WHERE payroll_period_id IN (1, 2, 3, 4, 5, 6, 7, 8);
DELETE FROM payroll_periods WHERE id IN (1, 2, 3, 4, 5, 6, 7, 8);
DELETE FROM attendance_records WHERE employee_id IN (1, 2) AND work_date >= '2024-08-01';
DELETE FROM employee_deductions WHERE employee_id IN (1, 2);
DELETE FROM employee_allowances WHERE employee_id IN (1, 2);
DELETE FROM employee_salaries WHERE employee_id IN (1, 2);

-- 1. Tạo employee salaries (cần có trước khi tính lương)
INSERT INTO employee_salaries (employee_id, salary_type, monthly_salary, effective_from, effective_to, deleted, created_by, updated_by, created_at, updated_at)
VALUES 
    (1, 'MONTHLY', 5000000, '2025-01-01', NULL, false, '1', '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'MONTHLY', 3500000, '2025-01-01', NULL, false, '1', '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 2. Tạo allowances (phụ cấp)
INSERT INTO employee_allowances (employee_id, allowance_code, allowance_name, allowance_type, amount, effective_from, effective_to, deleted, created_at, updated_at)
VALUES 
    (1, 'TRANSPORT', 'Phụ cấp đi lại', 'FIXED', 500000, '2025-01-01', NULL, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, 'MEAL', 'Phụ cấp ăn trưa', 'FIXED', 300000, '2025-01-01', NULL, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'TRANSPORT', 'Phụ cấp đi lại', 'FIXED', 300000, '2025-01-01', NULL, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 3. Tạo deductions (khấu trừ)
INSERT INTO employee_deductions (employee_id, deduction_code, deduction_name, deduction_type, amount, effective_from, effective_to, deleted, created_at, updated_at)
VALUES 
    (1, 'INSURANCE', 'Bảo hiểm', 'FIXED', 250000, '2025-01-01', NULL, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, 'TAX', 'Thuế thu nhập', 'FIXED', 500000, '2025-01-01', NULL, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'INSURANCE', 'Bảo hiểm', 'FIXED', 175000, '2025-01-01', NULL, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 4. Tạo attendance records (chấm công - tháng 1/2025)
INSERT INTO attendance_records (employee_id, work_date, original_check_in, original_check_out, rounded_check_in, rounded_check_out, status, working_minutes, created_by, updated_by, created_at, updated_at)
VALUES 
    -- Employee 1 - 20 ngày làm việc
    (1, '2025-01-06', '2025-01-06 09:00:00', '2025-01-06 18:00:00', '2025-01-06 09:00:00', '2025-01-06 18:00:00', 'PRESENT', 480, '1', '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, '2025-01-07', '2025-01-07 09:00:00', '2025-01-07 18:00:00', '2025-01-07 09:00:00', '2025-01-07 18:00:00', 'PRESENT', 480, '1', '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, '2025-01-08', '2025-01-08 09:00:00', '2025-01-08 18:00:00', '2025-01-08 09:00:00', '2025-01-08 18:00:00', 'PRESENT', 480, '1', '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, '2025-01-09', '2025-01-09 09:00:00', '2025-01-09 18:00:00', '2025-01-09 09:00:00', '2025-01-09 18:00:00', 'PRESENT', 480, '1', '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, '2025-01-10', '2025-01-10 09:00:00', '2025-01-10 18:00:00', '2025-01-10 09:00:00', '2025-01-10 18:00:00', 'PRESENT', 480, '1', '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, '2025-01-13', '2025-01-13 09:00:00', '2025-01-13 18:00:00', '2025-01-13 09:00:00', '2025-01-13 18:00:00', 'PRESENT', 480, '1', '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, '2025-01-14', '2025-01-14 09:00:00', '2025-01-14 18:00:00', '2025-01-14 09:00:00', '2025-01-14 18:00:00', 'PRESENT', 480, '1', '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, '2025-01-15', '2025-01-15 09:00:00', '2025-01-15 18:00:00', '2025-01-15 09:00:00', '2025-01-15 18:00:00', 'PRESENT', 480, '1', '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, '2025-01-16', '2025-01-16 09:00:00', '2025-01-16 18:00:00', '2025-01-16 09:00:00', '2025-01-16 18:00:00', 'PRESENT', 480, '1', '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, '2025-01-17', '2025-01-17 09:00:00', '2025-01-17 18:00:00', '2025-01-17 09:00:00', '2025-01-17 18:00:00', 'PRESENT', 480, '1', '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, '2025-01-20', '2025-01-20 09:00:00', '2025-01-20 18:00:00', '2025-01-20 09:00:00', '2025-01-20 18:00:00', 'PRESENT', 480, '1', '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, '2025-01-21', '2025-01-21 09:00:00', '2025-01-21 18:00:00', '2025-01-21 09:00:00', '2025-01-21 18:00:00', 'PRESENT', 480, '1', '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, '2025-01-22', '2025-01-22 09:00:00', '2025-01-22 18:00:00', '2025-01-22 09:00:00', '2025-01-22 18:00:00', 'PRESENT', 480, '1', '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, '2025-01-23', '2025-01-23 09:00:00', '2025-01-23 19:00:00', '2025-01-23 09:00:00', '2025-01-23 19:00:00', 'PRESENT', 540, '1', '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP), -- có OT
    (1, '2025-01-24', '2025-01-24 09:00:00', '2025-01-24 18:00:00', '2025-01-24 09:00:00', '2025-01-24 18:00:00', 'PRESENT', 480, '1', '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, '2025-01-27', '2025-01-27 09:00:00', '2025-01-27 18:00:00', '2025-01-27 09:00:00', '2025-01-27 18:00:00', 'PRESENT', 480, '1', '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, '2025-01-28', '2025-01-28 09:00:00', '2025-01-28 18:00:00', '2025-01-28 09:00:00', '2025-01-28 18:00:00', 'PRESENT', 480, '1', '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, '2025-01-29', '2025-01-29 09:00:00', '2025-01-29 18:00:00', '2025-01-29 09:00:00', '2025-01-29 18:00:00', 'PRESENT', 480, '1', '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, '2025-01-30', '2025-01-30 09:00:00', '2025-01-30 18:00:00', '2025-01-30 09:00:00', '2025-01-30 18:00:00', 'PRESENT', 480, '1', '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, '2025-01-31', '2025-01-31 09:00:00', '2025-01-31 18:00:00', '2025-01-31 09:00:00', '2025-01-31 18:00:00', 'PRESENT', 480, '1', '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    -- Employee 2 - 18 ngày làm việc (nghỉ 2 ngày)
    (2, '2025-01-06', '2025-01-06 09:00:00', '2025-01-06 18:00:00', '2025-01-06 09:00:00', '2025-01-06 18:00:00', 'PRESENT', 480, '1', '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, '2025-01-07', '2025-01-07 09:00:00', '2025-01-07 18:00:00', '2025-01-07 09:00:00', '2025-01-07 18:00:00', 'PRESENT', 480, '1', '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, '2025-01-08', '2025-01-08 09:00:00', '2025-01-08 18:00:00', '2025-01-08 09:00:00', '2025-01-08 18:00:00', 'PRESENT', 480, '1', '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, '2025-01-09', NULL, NULL, NULL, NULL, 'ABSENT', 0, '1', '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, '2025-01-10', '2025-01-10 09:00:00', '2025-01-10 18:00:00', '2025-01-10 09:00:00', '2025-01-10 18:00:00', 'PRESENT', 480, '1', '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, '2025-01-13', '2025-01-13 09:00:00', '2025-01-13 18:00:00', '2025-01-13 09:00:00', '2025-01-13 18:00:00', 'PRESENT', 480, '1', '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, '2025-01-14', '2025-01-14 09:00:00', '2025-01-14 18:00:00', '2025-01-14 09:00:00', '2025-01-14 18:00:00', 'PRESENT', 480, '1', '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, '2025-01-15', '2025-01-15 09:00:00', '2025-01-15 18:00:00', '2025-01-15 09:00:00', '2025-01-15 18:00:00', 'PRESENT', 480, '1', '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, '2025-01-16', '2025-01-16 09:00:00', '2025-01-16 18:00:00', '2025-01-16 09:00:00', '2025-01-16 18:00:00', 'PRESENT', 480, '1', '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, '2025-01-17', '2025-01-17 09:00:00', '2025-01-17 18:00:00', '2025-01-17 09:00:00', '2025-01-17 18:00:00', 'PRESENT', 480, '1', '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, '2025-01-20', '2025-01-20 09:00:00', '2025-01-20 18:00:00', '2025-01-20 09:00:00', '2025-01-20 18:00:00', 'PRESENT', 480, '1', '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, '2025-01-21', '2025-01-21 09:00:00', '2025-01-21 18:00:00', '2025-01-21 09:00:00', '2025-01-21 18:00:00', 'PRESENT', 480, '1', '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, '2025-01-22', NULL, NULL, NULL, NULL, 'ABSENT', 0, '1', '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, '2025-01-23', '2025-01-23 09:00:00', '2025-01-23 18:00:00', '2025-01-23 09:00:00', '2025-01-23 18:00:00', 'PRESENT', 480, '1', '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, '2025-01-24', '2025-01-24 09:00:00', '2025-01-24 18:00:00', '2025-01-24 09:00:00', '2025-01-24 18:00:00', 'PRESENT', 480, '1', '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, '2025-01-27', '2025-01-27 09:00:00', '2025-01-27 18:00:00', '2025-01-27 09:00:00', '2025-01-27 18:00:00', 'PRESENT', 480, '1', '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, '2025-01-28', '2025-01-28 09:00:00', '2025-01-28 18:00:00', '2025-01-28 09:00:00', '2025-01-28 18:00:00', 'PRESENT', 480, '1', '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, '2025-01-29', '2025-01-29 09:00:00', '2025-01-29 18:00:00', '2025-01-29 09:00:00', '2025-01-29 18:00:00', 'PRESENT', 480, '1', '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, '2025-01-30', '2025-01-30 09:00:00', '2025-01-30 18:00:00', '2025-01-30 09:00:00', '2025-01-30 18:00:00', 'PRESENT', 480, '1', '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, '2025-01-31', '2025-01-31 09:00:00', '2025-01-31 18:00:00', '2025-01-31 09:00:00', '2025-01-31 18:00:00', 'PRESENT', 480, '1', '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 5. Tạo payroll period (8 kỳ lương gần nhất)
INSERT INTO payroll_periods (
    id,
    period_start, 
    period_end, 
    year, 
    month, 
    status, 
    created_by,
    total_gross_salary,
    total_net_salary,
    total_employees,
    created_at, 
    updated_at
)
VALUES 
    -- Kỳ DRAFT (tháng 1/2025 - có thể edit)
    (1, '2025-01-01', '2025-01-31', 2025, 1, 'DRAFT', 1, 8800000, 7625000, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    -- Kỳ REVIEWING (tháng 12/2024 - đang chờ duyệt)
    (2, '2024-12-01', '2024-12-31', 2024, 12, 'REVIEWING', 1, 8500000, 7400000, 2, CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP - INTERVAL '30 days'),
    
    -- Kỳ APPROVED (tháng 11/2024 - đã duyệt)
    (3, '2024-11-01', '2024-11-30', 2024, 11, 'APPROVED', 1, 8500000, 7400000, 2, CURRENT_TIMESTAMP - INTERVAL '60 days', CURRENT_TIMESTAMP - INTERVAL '60 days'),
    
    -- Các kỳ PAID (đã thanh toán)
    (4, '2024-10-01', '2024-10-31', 2024, 10, 'PAID', 1, 8500000, 7400000, 2, CURRENT_TIMESTAMP - INTERVAL '90 days', CURRENT_TIMESTAMP - INTERVAL '90 days'),
    (5, '2024-09-01', '2024-09-30', 2024, 9, 'PAID', 1, 8300000, 7250000, 2, CURRENT_TIMESTAMP - INTERVAL '120 days', CURRENT_TIMESTAMP - INTERVAL '120 days'),
    (6, '2024-08-01', '2024-08-31', 2024, 8, 'PAID', 1, 8400000, 7300000, 2, CURRENT_TIMESTAMP - INTERVAL '150 days', CURRENT_TIMESTAMP - INTERVAL '150 days'),
    (7, '2024-07-01', '2024-07-31', 2024, 7, 'PAID', 1, 8600000, 7500000, 2, CURRENT_TIMESTAMP - INTERVAL '180 days', CURRENT_TIMESTAMP - INTERVAL '180 days'),
    (8, '2024-06-01', '2024-06-30', 2024, 6, 'PAID', 1, 8200000, 7150000, 2, CURRENT_TIMESTAMP - INTERVAL '210 days', CURRENT_TIMESTAMP - INTERVAL '210 days');

-- Reset sequence cho payroll_periods
SELECT setval('payroll_periods_id_seq', (SELECT MAX(id) FROM payroll_periods));

-- 6. Tạo payroll items cho kỳ tháng 1/2025 (period_id = 1)
INSERT INTO payroll_items (
    payroll_period_id, employee_id, salary_type,
    base_salary, calculated_base_salary, working_days, working_hours, working_minutes,
    regular_overtime_minutes, night_overtime_minutes, holiday_overtime_minutes, weekend_overtime_minutes,
    total_overtime_pay, total_break_minutes, break_type, break_deduction_amount,
    allowance_details, total_allowances, deduction_details, total_deductions,
    gross_salary, net_salary, adjustment_amount, status,
    created_at, updated_at
)
VALUES 
    -- Employee 1 (Admin)
    (
        1, 1, 'MONTHLY',
        5000000, 5000000, 20, 160, 9600,
        60, 0, 0, 0, 50000,
        0, 'UNPAID', 0,
        '{"TRANSPORT": 500000, "MEAL": 300000}'::jsonb, 800000,
        '{"INSURANCE": 250000, "TAX": 500000}'::jsonb, 750000,
        5850000, 5100000, 0,
        'CALCULATED',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    ),
    
    -- Employee 2 (Bot1)
    (
        1, 2, 'MONTHLY',
        3500000, 3150000, 18, 144, 8640,
        0, 0, 0, 0, 0,
        0, 'UNPAID', 0,
        '{"TRANSPORT": 300000}'::jsonb, 300000,
        '{"INSURANCE": 175000}'::jsonb, 175000,
        3450000, 3275000, 0,
        'CALCULATED',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    );

-- 7. Tạo payroll items cho các kỳ cũ (để test pagination và history)
INSERT INTO payroll_items (
    payroll_period_id, employee_id, salary_type, base_salary, calculated_base_salary,
    working_days, working_hours, working_minutes,
    regular_overtime_minutes, night_overtime_minutes, holiday_overtime_minutes, weekend_overtime_minutes,
    total_overtime_pay, total_break_minutes, break_type, break_deduction_amount,
    allowance_details, total_allowances, deduction_details, total_deductions,
    gross_salary, net_salary, adjustment_amount, status, created_at, updated_at
)
VALUES 
    -- Tháng 12/2024 (REVIEWING)
    (2, 1, 'MONTHLY', 5000000, 5000000, 20, 160, 9600, 0, 0, 0, 0, 0, 0, 'UNPAID', 0,
     '{"TRANSPORT": 500000, "MEAL": 300000}'::jsonb, 800000,
     '{"INSURANCE": 250000, "TAX": 500000}'::jsonb, 750000,
     5800000, 5050000, 0, 'CALCULATED', CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP - INTERVAL '30 days'),
    (2, 2, 'MONTHLY', 3500000, 3500000, 20, 160, 9600, 0, 0, 0, 0, 0, 0, 'UNPAID', 0,
     '{"TRANSPORT": 300000}'::jsonb, 300000,
     '{"INSURANCE": 175000}'::jsonb, 175000,
     3800000, 3625000, 0, 'CALCULATED', CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP - INTERVAL '30 days'),
    
    -- Tháng 11/2024 (APPROVED)
    (3, 1, 'MONTHLY', 5000000, 5000000, 20, 160, 9600, 0, 0, 0, 0, 0, 0, 'UNPAID', 0,
     '{"TRANSPORT": 500000, "MEAL": 300000}'::jsonb, 800000,
     '{"INSURANCE": 250000, "TAX": 500000}'::jsonb, 750000,
     5800000, 5050000, 0, 'CALCULATED', CURRENT_TIMESTAMP - INTERVAL '60 days', CURRENT_TIMESTAMP - INTERVAL '60 days'),
    (3, 2, 'MONTHLY', 3500000, 3500000, 20, 160, 9600, 0, 0, 0, 0, 0, 0, 'UNPAID', 0,
     '{"TRANSPORT": 300000}'::jsonb, 300000,
     '{"INSURANCE": 175000}'::jsonb, 175000,
     3800000, 3625000, 0, 'CALCULATED', CURRENT_TIMESTAMP - INTERVAL '60 days', CURRENT_TIMESTAMP - INTERVAL '60 days'),
    
    -- Tháng 10/2024 (PAID)
    (4, 1, 'MONTHLY', 5000000, 5000000, 20, 160, 9600, 0, 0, 0, 0, 0, 0, 'UNPAID', 0,
     '{"TRANSPORT": 500000, "MEAL": 300000}'::jsonb, 800000,
     '{"INSURANCE": 250000, "TAX": 500000}'::jsonb, 750000,
     5800000, 5050000, 0, 'CALCULATED', CURRENT_TIMESTAMP - INTERVAL '90 days', CURRENT_TIMESTAMP - INTERVAL '90 days'),
    (4, 2, 'MONTHLY', 3500000, 3500000, 20, 160, 9600, 0, 0, 0, 0, 0, 0, 'UNPAID', 0,
     '{"TRANSPORT": 300000}'::jsonb, 300000,
     '{"INSURANCE": 175000}'::jsonb, 175000,
     3800000, 3625000, 0, 'CALCULATED', CURRENT_TIMESTAMP - INTERVAL '90 days', CURRENT_TIMESTAMP - INTERVAL '90 days'),
    
    -- Tháng 9/2024 (PAID)
    (5, 1, 'MONTHLY', 5000000, 5000000, 19, 152, 9120, 0, 0, 0, 0, 0, 0, 'UNPAID', 0,
     '{"TRANSPORT": 500000, "MEAL": 300000}'::jsonb, 800000,
     '{"INSURANCE": 250000, "TAX": 500000}'::jsonb, 750000,
     5800000, 5050000, 0, 'CALCULATED', CURRENT_TIMESTAMP - INTERVAL '120 days', CURRENT_TIMESTAMP - INTERVAL '120 days'),
    (5, 2, 'MONTHLY', 3500000, 3325000, 19, 152, 9120, 0, 0, 0, 0, 0, 0, 'UNPAID', 0,
     '{"TRANSPORT": 300000}'::jsonb, 300000,
     '{"INSURANCE": 175000}'::jsonb, 175000,
     3625000, 3450000, 0, 'CALCULATED', CURRENT_TIMESTAMP - INTERVAL '120 days', CURRENT_TIMESTAMP - INTERVAL '120 days'),
    
    -- Tháng 8/2024 (PAID)
    (6, 1, 'MONTHLY', 5000000, 5000000, 21, 168, 10080, 0, 0, 0, 0, 0, 0, 'UNPAID', 0,
     '{"TRANSPORT": 500000, "MEAL": 300000}'::jsonb, 800000,
     '{"INSURANCE": 250000, "TAX": 500000}'::jsonb, 750000,
     5800000, 5050000, 0, 'CALCULATED', CURRENT_TIMESTAMP - INTERVAL '150 days', CURRENT_TIMESTAMP - INTERVAL '150 days'),
    (6, 2, 'MONTHLY', 3500000, 3675000, 21, 168, 10080, 0, 0, 0, 0, 0, 0, 'UNPAID', 0,
     '{"TRANSPORT": 300000}'::jsonb, 300000,
     '{"INSURANCE": 175000}'::jsonb, 175000,
     3975000, 3800000, 0, 'CALCULATED', CURRENT_TIMESTAMP - INTERVAL '150 days', CURRENT_TIMESTAMP - INTERVAL '150 days'),
    
    -- Tháng 7/2024 (PAID - có overtime)
    (7, 1, 'MONTHLY', 5000000, 5000000, 20, 160, 9600, 120, 0, 0, 0, 100000, 0, 'UNPAID', 0,
     '{"TRANSPORT": 500000, "MEAL": 300000}'::jsonb, 800000,
     '{"INSURANCE": 250000, "TAX": 500000}'::jsonb, 750000,
     5900000, 5150000, 0, 'CALCULATED', CURRENT_TIMESTAMP - INTERVAL '180 days', CURRENT_TIMESTAMP - INTERVAL '180 days'),
    (7, 2, 'MONTHLY', 3500000, 3500000, 20, 160, 9600, 60, 0, 0, 0, 50000, 0, 'UNPAID', 0,
     '{"TRANSPORT": 300000}'::jsonb, 300000,
     '{"INSURANCE": 175000}'::jsonb, 175000,
     3850000, 3675000, 0, 'CALCULATED', CURRENT_TIMESTAMP - INTERVAL '180 days', CURRENT_TIMESTAMP - INTERVAL '180 days'),
    
    -- Tháng 6/2024 (PAID)
    (8, 1, 'MONTHLY', 5000000, 5000000, 18, 144, 8640, 0, 0, 0, 0, 0, 0, 'UNPAID', 0,
     '{"TRANSPORT": 500000, "MEAL": 300000}'::jsonb, 800000,
     '{"INSURANCE": 250000, "TAX": 500000}'::jsonb, 750000,
     5800000, 5050000, 0, 'CALCULATED', CURRENT_TIMESTAMP - INTERVAL '210 days', CURRENT_TIMESTAMP - INTERVAL '210 days'),
    (8, 2, 'MONTHLY', 3500000, 3150000, 18, 144, 8640, 0, 0, 0, 0, 0, 0, 'UNPAID', 0,
     '{"TRANSPORT": 300000}'::jsonb, 300000,
     '{"INSURANCE": 175000}'::jsonb, 175000,
     3450000, 3275000, 0, 'CALCULATED', CURRENT_TIMESTAMP - INTERVAL '210 days', CURRENT_TIMESTAMP - INTERVAL '210 days');

-- =====================================================
-- VERIFY DATA
-- =====================================================

-- Check payroll periods
SELECT 
    id, 
    year, 
    month, 
    status, 
    total_employees,
    total_gross_salary,
    total_net_salary,
    period_start,
    period_end
FROM payroll_periods 
ORDER BY year DESC, month DESC;

-- Check payroll items for January 2025
SELECT 
    pi.id,
    u.employee_code,
    up.name,
    pi.salary_type,
    pi.base_salary,
    pi.working_days,
    pi.total_allowances,
    pi.total_deductions,
    pi.gross_salary,
    pi.net_salary
FROM payroll_items pi
JOIN users u ON pi.employee_id = u.id
LEFT JOIN user_profiles up ON u.id = up.user_id
WHERE pi.payroll_period_id = 1
ORDER BY pi.employee_id;
