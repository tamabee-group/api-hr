-- =====================================================
-- TAMABEE HR - TEST DATA (MINIMAL VERSION)
-- Email: tamachan.test1@gmail.com -> tamachan.test6@gmail.com
-- Password: hiep1234
-- Currency: JPY (Japanese Yen)
-- =====================================================

-- =====================================================
-- 1. USERS - Người dùng test (12 users)
-- Password: hiep1234 (BCrypt: $2a$12$.dYHl1TIT3mE9x9MAtrANuJlMTKNThO1TWv./AWl.Y3XCOIGvfkMW)
-- =====================================================
INSERT INTO users (id, employee_code, email, password, role, status, locale, language, company_id, profile_completeness, deleted, created_at, updated_at) VALUES
    -- Tamabee Staff (companyId = 0)
    (1, '25001501', 'tamachan.test1@gmail.com', '$2a$12$.dYHl1TIT3mE9x9MAtrANuJlMTKNThO1TWv./AWl.Y3XCOIGvfkMW', 'ADMIN_TAMABEE', 'ACTIVE', 'Asia/Tokyo', 'ja', 0, 80, false, NOW(), NOW()),
    (2, '25002005', 'tamachan.test2@gmail.com', '$2a$12$.dYHl1TIT3mE9x9MAtrANuJlMTKNThO1TWv./AWl.Y3XCOIGvfkMW', 'MANAGER_TAMABEE', 'ACTIVE', 'Asia/Tokyo', 'ja', 0, 60, false, NOW(), NOW()),
    (3, '25001008', 'tamachan.test3@gmail.com', '$2a$12$.dYHl1TIT3mE9x9MAtrANuJlMTKNThO1TWv./AWl.Y3XCOIGvfkMW', 'EMPLOYEE_TAMABEE', 'ACTIVE', 'Asia/Tokyo', 'ja', 0, 40, false, NOW(), NOW()),
    -- Company 1 Staff (Standard Plan - full features)
    (4, '25012503', 'tamachan.test4@gmail.com', '$2a$12$.dYHl1TIT3mE9x9MAtrANuJlMTKNThO1TWv./AWl.Y3XCOIGvfkMW', 'ADMIN_COMPANY', 'ACTIVE', 'Asia/Tokyo', 'ja', 1, 90, false, NOW(), NOW()),
    (5, '25011207', 'tamachan.test5@gmail.com', '$2a$12$.dYHl1TIT3mE9x9MAtrANuJlMTKNThO1TWv./AWl.Y3XCOIGvfkMW', 'MANAGER_COMPANY', 'ACTIVE', 'Asia/Tokyo', 'ja', 1, 70, false, NOW(), NOW()),
    (6, '25013011', 'tamachan.test6@gmail.com', '$2a$12$.dYHl1TIT3mE9x9MAtrANuJlMTKNThO1TWv./AWl.Y3XCOIGvfkMW', 'EMPLOYEE_COMPANY', 'ACTIVE', 'Asia/Tokyo', 'ja', 1, 50, false, NOW(), NOW()),
    (7, '26010815', 'employee1.company1@test.com', '$2a$12$.dYHl1TIT3mE9x9MAtrANuJlMTKNThO1TWv./AWl.Y3XCOIGvfkMW', 'EMPLOYEE_COMPANY', 'ACTIVE', 'Asia/Tokyo', 'ja', 1, 30, false, NOW(), NOW()),
    -- Company 2 Staff (Basic Plan)
    (8, '25021804', 'admin.company2@test.com', '$2a$12$.dYHl1TIT3mE9x9MAtrANuJlMTKNThO1TWv./AWl.Y3XCOIGvfkMW', 'ADMIN_COMPANY', 'ACTIVE', 'Asia/Tokyo', 'ja', 2, 85, false, NOW(), NOW()),
    (9, '25020509', 'employee1.company2@test.com', '$2a$12$.dYHl1TIT3mE9x9MAtrANuJlMTKNThO1TWv./AWl.Y3XCOIGvfkMW', 'EMPLOYEE_COMPANY', 'ACTIVE', 'Asia/Tokyo', 'ja', 2, 45, false, NOW(), NOW()),
    -- Company 3 Staff (Enterprise Plan - all features)
    (10, '25030112', 'admin.company3@test.com', '$2a$12$.dYHl1TIT3mE9x9MAtrANuJlMTKNThO1TWv./AWl.Y3XCOIGvfkMW', 'ADMIN_COMPANY', 'ACTIVE', 'Asia/Tokyo', 'ja', 3, 95, false, NOW(), NOW()),
    (11, '25031506', 'manager.company3@test.com', '$2a$12$.dYHl1TIT3mE9x9MAtrANuJlMTKNThO1TWv./AWl.Y3XCOIGvfkMW', 'MANAGER_COMPANY', 'ACTIVE', 'Asia/Tokyo', 'ja', 3, 75, false, NOW(), NOW()),
    (12, '26031102', 'employee1.company3@test.com', '$2a$12$.dYHl1TIT3mE9x9MAtrANuJlMTKNThO1TWv./AWl.Y3XCOIGvfkMW', 'EMPLOYEE_COMPANY', 'ACTIVE', 'Asia/Tokyo', 'ja', 3, 55, false, NOW(), NOW());

SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));

-- =====================================================
-- 2. USER_PROFILES - Thông tin profile
-- =====================================================
INSERT INTO user_profiles (id, user_id, name, phone, address, zip_code, date_of_birth, gender, referral_code, bank_account_type, bank_name, bank_account, bank_account_name, created_at, updated_at) VALUES
    -- Tamabee Staff
    (1, 1, 'Admin Tamabee', '0311111111', '東京都千代田区1-1-1', '1000001', '1990-01-15', 'MALE', 'TMADM001', 'JP', 'MUFG', '1234567', 'ADMIN TAMABEE', NOW(), NOW()),
    (2, 2, 'Manager Tamabee', '0322222222', '東京都中央区2-2-2', '1030001', '1992-05-20', 'FEMALE', 'TMMGR002', 'JP', 'SMBC', '0987654', 'MANAGER TAMABEE', NOW(), NOW()),
    (3, 3, 'Employee Tamabee', '0333333333', NULL, NULL, '1995-08-10', 'MALE', 'TMEMP003', 'VN', NULL, NULL, NULL, NOW(), NOW()),
    -- Company 1 Staff
    (4, 4, '山田太郎', '0344444444', '東京都渋谷区3-3-3', '1500001', '1988-03-25', 'MALE', NULL, 'JP', 'Mizuho', '1122334', 'YAMADA TARO', NOW(), NOW()),
    (5, 5, '鈴木花子', '0355555555', '東京都渋谷区4-4-4', '1500002', '1991-07-12', 'FEMALE', NULL, 'JP', 'MUFG', '5544332', 'SUZUKI HANAKO', NOW(), NOW()),
    (6, 6, '高橋一郎', '0366666666', '東京都渋谷区5-5-5', NULL, '1993-11-30', 'MALE', NULL, 'VN', NULL, NULL, NULL, NOW(), NOW()),
    (7, 7, '加藤大輔', '0377777777', '東京都渋谷区6-6-6', '1500003', '1990-08-15', 'MALE', NULL, 'JP', 'Mizuho', '4445556', 'KATO DAISUKE', NOW(), NOW()),
    -- Company 2 Staff
    (8, 8, '佐藤美咲', '0388888888', '東京都新宿区6-6-6', '1600022', '1989-04-18', 'FEMALE', NULL, 'JP', 'Rakuten', '6677889', 'SATO MISAKI', NOW(), NOW()),
    (9, 9, '伊藤健太', '0399999999', NULL, NULL, '1994-09-05', 'MALE', NULL, 'VN', NULL, NULL, NULL, NOW(), NOW()),
    -- Company 3 Staff
    (10, 10, '田中一郎', '0300000001', '東京都港区7-7-7', '1050001', '1985-12-01', 'MALE', NULL, 'JP', 'MUFG', '9988776', 'TANAKA ICHIRO', NOW(), NOW()),
    (11, 11, '渡辺さくら', '0300000002', '東京都港区8-8-8', '1050002', '1990-06-15', 'FEMALE', NULL, 'JP', 'SMBC', '7766554', 'WATANABE SAKURA', NOW(), NOW()),
    (12, 12, '中村健一', '0300000003', '東京都港区9-9-9', '1050003', '1996-06-15', 'MALE', NULL, 'VN', NULL, NULL, NULL, NOW(), NOW());

SELECT setval('user_profiles_id_seq', (SELECT MAX(id) FROM user_profiles));

-- =====================================================
-- 3. COMPANIES - 3 công ty test với các plan khác nhau
-- =====================================================
INSERT INTO companies (id, name, owner_name, email, phone, address, industry, zipcode, locale, language, plan_id, referred_by_employee_id, owner_id, status, deleted, created_at, updated_at) VALUES
    (1, 'ABC Tech株式会社', '山田太郎', 'company1@test.com', '0312345678', '東京都渋谷区1-2-3', 'technology', '1500001', 'Asia/Tokyo', 'ja', 2, 1, 4, 'ACTIVE', false, NOW() - INTERVAL '60 days', NOW()),
    (2, 'XYZ Solutions株式会社', '佐藤美咲', 'company2@test.com', '0323456789', '東京都新宿区4-5-6', 'technology', '1600022', 'Asia/Tokyo', 'ja', 1, 2, 8, 'ACTIVE', false, NOW() - INTERVAL '45 days', NOW()),
    (3, 'Tokyo Corp', '田中一郎', 'company3@test.com', '0334567890', '東京都港区7-8-9', 'finance', '1050001', 'Asia/Tokyo', 'ja', 3, NULL, 10, 'ACTIVE', false, NOW() - INTERVAL '90 days', NOW());

SELECT setval('companies_id_seq', (SELECT MAX(id) FROM companies));

-- =====================================================
-- 4. WALLETS - Ví tiền công ty (JPY)
-- =====================================================
INSERT INTO wallets (id, company_id, balance, total_billing, last_billing_date, next_billing_date, free_trial_end_date, created_at, updated_at) VALUES
    (1, 1, 50000, 60000, NOW() - INTERVAL '30 days', NOW() + INTERVAL '30 days', NULL, NOW() - INTERVAL '60 days', NOW()),
    (2, 2, 15000, 20000, NOW() - INTERVAL '15 days', NOW() + INTERVAL '15 days', NOW() + INTERVAL '7 days', NOW() - INTERVAL '45 days', NOW()),
    (3, 3, 100000, 80000, NOW() - INTERVAL '20 days', NOW() + INTERVAL '10 days', NULL, NOW() - INTERVAL '90 days', NOW());

SELECT setval('wallets_id_seq', (SELECT MAX(id) FROM wallets));

-- =====================================================
-- 5. WALLET_TRANSACTIONS - Lịch sử giao dịch (JPY)
-- =====================================================
INSERT INTO wallet_transactions (id, wallet_id, transaction_type, amount, balance_before, balance_after, description, reference_id, deleted, created_at, updated_at) VALUES
    -- Company 1 Transactions
    (1, 1, 'DEPOSIT', 50000, 0, 50000, '初回入金', 1, false, NOW() - INTERVAL '55 days', NOW()),
    (2, 1, 'BILLING', -10000, 50000, 40000, 'スタンダードプラン11月分', NULL, false, NOW() - INTERVAL '30 days', NOW()),
    (3, 1, 'DEPOSIT', 30000, 40000, 70000, '追加入金', 2, false, NOW() - INTERVAL '20 days', NOW()),
    (4, 1, 'BILLING', -10000, 70000, 60000, 'スタンダードプラン12月分', NULL, false, NOW() - INTERVAL '10 days', NOW()),
    (5, 1, 'BILLING', -10000, 60000, 50000, 'スタンダードプラン1月分', NULL, false, NOW(), NOW()),
    -- Company 2 Transactions
    (6, 2, 'DEPOSIT', 20000, 0, 20000, '初回入金', 3, false, NOW() - INTERVAL '40 days', NOW()),
    (7, 2, 'BILLING', -5000, 20000, 15000, 'ベーシックプラン12月分', NULL, false, NOW() - INTERVAL '15 days', NOW()),
    -- Company 3 Transactions
    (8, 3, 'DEPOSIT', 100000, 0, 100000, '初回入金', 4, false, NOW() - INTERVAL '85 days', NOW()),
    (9, 3, 'BILLING', -20000, 100000, 80000, 'エンタープライズプラン11月分', NULL, false, NOW() - INTERVAL '60 days', NOW()),
    (10, 3, 'DEPOSIT', 40000, 80000, 120000, '追加入金', 5, false, NOW() - INTERVAL '30 days', NOW()),
    (11, 3, 'BILLING', -20000, 120000, 100000, 'エンタープライズプラン12月分', NULL, false, NOW() - INTERVAL '20 days', NOW());

SELECT setval('wallet_transactions_id_seq', (SELECT MAX(id) FROM wallet_transactions));


-- =====================================================
-- 6. DEPOSIT_REQUESTS - Yêu cầu nạp tiền (JPY)
-- =====================================================
INSERT INTO deposit_requests (id, company_id, amount, transfer_proof_url, status, requested_by, approved_by, rejection_reason, processed_at, deleted, created_at, updated_at) VALUES
    -- APPROVED
    (1, 1, 50000, '/uploads/deposits/proof1.jpg', 'APPROVED', '25012503', '25001501', NULL, NOW() - INTERVAL '54 days', false, NOW() - INTERVAL '55 days', NOW()),
    (2, 1, 30000, '/uploads/deposits/proof2.jpg', 'APPROVED', '25012503', '25002005', NULL, NOW() - INTERVAL '19 days', false, NOW() - INTERVAL '20 days', NOW()),
    (3, 2, 20000, '/uploads/deposits/proof3.jpg', 'APPROVED', '25021804', '25001501', NULL, NOW() - INTERVAL '39 days', false, NOW() - INTERVAL '40 days', NOW()),
    (4, 3, 100000, '/uploads/deposits/proof4.jpg', 'APPROVED', '25030112', '25001501', NULL, NOW() - INTERVAL '84 days', false, NOW() - INTERVAL '85 days', NOW()),
    (5, 3, 40000, '/uploads/deposits/proof5.jpg', 'APPROVED', '25030112', '25002005', NULL, NOW() - INTERVAL '29 days', false, NOW() - INTERVAL '30 days', NOW()),
    -- PENDING
    (6, 1, 25000, '/uploads/deposits/proof6.jpg', 'PENDING', '25012503', NULL, NULL, NULL, false, NOW() - INTERVAL '2 days', NOW()),
    (7, 2, 15000, '/uploads/deposits/proof7.jpg', 'PENDING', '25021804', NULL, NULL, NULL, false, NOW() - INTERVAL '1 day', NOW()),
    (8, 3, 50000, '/uploads/deposits/proof8.jpg', 'PENDING', '25030112', NULL, NULL, NULL, false, NOW() - INTERVAL '3 hours', NOW()),
    -- REJECTED
    (9, 1, 5000, '/uploads/deposits/proof9.jpg', 'REJECTED', '25012503', '25001501', '振込証明書が不鮮明です', NOW() - INTERVAL '5 days', false, NOW() - INTERVAL '7 days', NOW()),
    (10, 2, 10000, '/uploads/deposits/proof10.jpg', 'REJECTED', '25021804', '25002005', '金額が一致しません', NOW() - INTERVAL '3 days', false, NOW() - INTERVAL '4 days', NOW());

SELECT setval('deposit_requests_id_seq', (SELECT MAX(id) FROM deposit_requests));

-- =====================================================
-- 7. EMPLOYEE_COMMISSIONS - Hoa hồng nhân viên (JPY)
-- =====================================================
INSERT INTO employee_commissions (id, employee_code, company_id, amount, status, company_billing_at_creation, paid_at, paid_by, deleted, created_at, updated_at) VALUES
    -- Admin Tamabee (25001501) - Referred Company 1 (total_billing = 60,000 -> ELIGIBLE)
    (1, '25001501', 1, 5000, 'PAID', 0, NOW() - INTERVAL '50 days', '25001501', false, NOW() - INTERVAL '55 days', NOW()),
    (2, '25001501', 1, 5000, 'ELIGIBLE', 50000, NULL, NULL, false, NOW() - INTERVAL '10 days', NOW()),
    -- Manager Tamabee (25002005) - Referred Company 2 (total_billing = 20,000 -> PENDING)
    (3, '25002005', 2, 5000, 'PENDING', 0, NULL, NULL, false, NOW() - INTERVAL '40 days', NOW()),
    (4, '25002005', 2, 5000, 'PENDING', 15000, NULL, NULL, false, NOW() - INTERVAL '10 days', NOW());

SELECT setval('employee_commissions_id_seq', (SELECT MAX(id) FROM employee_commissions));

-- =====================================================
-- 8. COMPANY_SETTINGS - Cấu hình chấm công/lương
-- =====================================================
INSERT INTO company_settings (id, company_id, attendance_config, payroll_config, overtime_config, allowance_config, deduction_config, break_config, deleted, created_at, updated_at) VALUES
    (1, 1, 
        '{"defaultWorkStartTime": "09:00", "defaultWorkEndTime": "18:00", "defaultBreakMinutes": 60, "enableRounding": true, "checkInRounding": {"interval": "MINUTES_15", "direction": "DOWN"}, "checkOutRounding": {"interval": "MINUTES_15", "direction": "UP"}, "lateGraceMinutes": 5, "earlyLeaveGraceMinutes": 5, "requireDeviceRegistration": false, "requireGeoLocation": false, "geoFenceRadiusMeters": 100, "allowMobileCheckIn": true, "allowWebCheckIn": true}',
        '{"defaultSalaryType": "MONTHLY", "payDay": 25, "cutoffDay": 20, "salaryRounding": "NEAREST", "standardWorkingDaysPerMonth": 22, "standardWorkingHoursPerDay": 8}',
        '{"enableOvertime": true, "requireApproval": false, "regularOvertimeRate": 1.25, "nightOvertimeRate": 1.50, "holidayOvertimeRate": 1.35, "weekendOvertimeRate": 1.35, "nightStartTime": "22:00", "nightEndTime": "05:00", "maxOvertimeHoursPerDay": 4, "maxOvertimeHoursPerMonth": 45}',
        '{"allowances": [{"code": "TRANSPORT", "name": "交通費", "type": "FIXED", "amount": 10000, "taxable": false}, {"code": "MEAL", "name": "食事手当", "type": "FIXED", "amount": 5000, "taxable": true}]}',
        '{"deductions": [{"code": "HEALTH_INS", "name": "健康保険", "type": "PERCENTAGE", "percentage": 5.0, "order": 1}, {"code": "PENSION", "name": "厚生年金", "type": "PERCENTAGE", "percentage": 9.15, "order": 2}], "enableLatePenalty": true, "latePenaltyPerMinute": 100, "enableEarlyLeavePenalty": true, "earlyLeavePenaltyPerMinute": 100, "enableAbsenceDeduction": true}',
        '{"breakMode": "FLEXIBLE", "minBreakMinutes": 45, "maxBreakMinutes": 90, "maxBreaksPerDay": 3, "breakType": "UNPAID"}',
        false, NOW(), NOW()),
    (2, 2, 
        '{"defaultWorkStartTime": "09:30", "defaultWorkEndTime": "18:30", "defaultBreakMinutes": 60, "enableRounding": false, "lateGraceMinutes": 10, "earlyLeaveGraceMinutes": 10, "requireDeviceRegistration": false, "requireGeoLocation": false, "allowMobileCheckIn": true, "allowWebCheckIn": true}',
        '{"defaultSalaryType": "MONTHLY", "payDay": 25, "cutoffDay": 20, "salaryRounding": "NEAREST", "standardWorkingDaysPerMonth": 22, "standardWorkingHoursPerDay": 8}',
        '{"enableOvertime": true, "requireApproval": true, "regularOvertimeRate": 1.25, "nightOvertimeRate": 1.50, "holidayOvertimeRate": 1.35, "weekendOvertimeRate": 1.35, "nightStartTime": "22:00", "nightEndTime": "05:00", "maxOvertimeHoursPerDay": 3, "maxOvertimeHoursPerMonth": 40}',
        '{"allowances": [{"code": "TRANSPORT", "name": "交通費", "type": "FIXED", "amount": 15000, "taxable": false}]}',
        '{"deductions": [{"code": "HEALTH_INS", "name": "健康保険", "type": "PERCENTAGE", "percentage": 5.0, "order": 1}], "enableLatePenalty": false, "enableEarlyLeavePenalty": false, "enableAbsenceDeduction": true}',
        '{"breakMode": "FIXED", "fixedBreakMinutes": 60, "breakType": "UNPAID"}',
        false, NOW(), NOW()),
    (3, 3, 
        '{"defaultWorkStartTime": "08:30", "defaultWorkEndTime": "17:30", "defaultBreakMinutes": 45, "enableRounding": true, "checkInRounding": {"interval": "MINUTES_5", "direction": "NEAREST"}, "checkOutRounding": {"interval": "MINUTES_5", "direction": "NEAREST"}, "lateGraceMinutes": 0, "earlyLeaveGraceMinutes": 0, "requireDeviceRegistration": true, "requireGeoLocation": true, "geoFenceRadiusMeters": 50, "allowMobileCheckIn": true, "allowWebCheckIn": false}',
        '{"defaultSalaryType": "MONTHLY", "payDay": 28, "cutoffDay": 25, "salaryRounding": "DOWN", "standardWorkingDaysPerMonth": 20, "standardWorkingHoursPerDay": 8}',
        '{"enableOvertime": true, "requireApproval": true, "regularOvertimeRate": 1.30, "nightOvertimeRate": 1.60, "holidayOvertimeRate": 1.50, "weekendOvertimeRate": 1.40, "nightStartTime": "22:00", "nightEndTime": "05:00", "maxOvertimeHoursPerDay": 5, "maxOvertimeHoursPerMonth": 60}',
        '{"allowances": [{"code": "TRANSPORT", "name": "交通費", "type": "FIXED", "amount": 20000, "taxable": false}, {"code": "HOUSING", "name": "住宅手当", "type": "FIXED", "amount": 30000, "taxable": true}]}',
        '{"deductions": [{"code": "HEALTH_INS", "name": "健康保険", "type": "PERCENTAGE", "percentage": 5.0, "order": 1}, {"code": "PENSION", "name": "厚生年金", "type": "PERCENTAGE", "percentage": 9.15, "order": 2}, {"code": "INCOME_TAX", "name": "所得税", "type": "PERCENTAGE", "percentage": 10.0, "order": 3}], "enableLatePenalty": true, "latePenaltyPerMinute": 200, "enableEarlyLeavePenalty": true, "earlyLeavePenaltyPerMinute": 200, "enableAbsenceDeduction": true}',
        '{"breakMode": "FLEXIBLE", "minBreakMinutes": 45, "maxBreakMinutes": 60, "maxBreaksPerDay": 2, "breakType": "UNPAID"}',
        false, NOW(), NOW());

SELECT setval('company_settings_id_seq', (SELECT MAX(id) FROM company_settings));

-- =====================================================
-- 9. WORK_SCHEDULES - Lịch làm việc
-- =====================================================
INSERT INTO work_schedules (id, company_id, name, type, is_default, schedule_data, description, deleted, created_at, updated_at) VALUES
    (1, 1, '標準勤務', 'FIXED', true, '{"workStartTime": "09:00", "workEndTime": "18:00", "breakMinutes": 60}', '標準的な勤務時間', false, NOW(), NOW()),
    (2, 1, 'フレックス勤務', 'FLEXIBLE', false, '{"monday": {"workStartTime": "10:00", "workEndTime": "19:00"}, "tuesday": {"workStartTime": "09:00", "workEndTime": "18:00"}, "wednesday": {"workStartTime": "09:00", "workEndTime": "18:00"}, "thursday": {"workStartTime": "09:00", "workEndTime": "18:00"}, "friday": {"workStartTime": "09:00", "workEndTime": "17:00"}, "breakMinutes": 60}', 'フレックスタイム制', false, NOW(), NOW()),
    (3, 2, '標準勤務', 'FIXED', true, '{"workStartTime": "09:30", "workEndTime": "18:30", "breakMinutes": 60}', '標準的な勤務時間', false, NOW(), NOW()),
    (4, 3, '早番', 'FIXED', true, '{"workStartTime": "08:30", "workEndTime": "17:30", "breakMinutes": 45}', '早番シフト', false, NOW(), NOW()),
    (5, 3, '遅番', 'FIXED', false, '{"workStartTime": "10:00", "workEndTime": "19:00", "breakMinutes": 45}', '遅番シフト', false, NOW(), NOW());

SELECT setval('work_schedules_id_seq', (SELECT MAX(id) FROM work_schedules));

-- =====================================================
-- 10. WORK_SCHEDULE_ASSIGNMENTS - Gán lịch làm việc
-- =====================================================
INSERT INTO work_schedule_assignments (id, employee_id, schedule_id, effective_from, effective_to, deleted, created_at, updated_at) VALUES
    (1, 4, 1, '2025-01-01', NULL, false, NOW(), NOW()),
    (2, 5, 1, '2025-01-01', NULL, false, NOW(), NOW()),
    (3, 6, 2, '2025-01-01', NULL, false, NOW(), NOW()),
    (4, 7, 1, '2025-01-01', NULL, false, NOW(), NOW()),
    (5, 8, 3, '2025-01-01', NULL, false, NOW(), NOW()),
    (6, 9, 3, '2025-01-01', NULL, false, NOW(), NOW()),
    (7, 10, 4, '2025-01-01', NULL, false, NOW(), NOW()),
    (8, 11, 4, '2025-01-01', NULL, false, NOW(), NOW()),
    (9, 12, 5, '2025-01-01', NULL, false, NOW(), NOW());

SELECT setval('work_schedule_assignments_id_seq', (SELECT MAX(id) FROM work_schedule_assignments));


-- =====================================================
-- 11. HOLIDAYS - Ngày lễ 2025 (Japan)
-- =====================================================
INSERT INTO holidays (id, company_id, date, name, type, is_paid, deleted, created_at, updated_at) VALUES
    (1, NULL, '2025-01-01', '元日', 'NATIONAL', true, false, NOW(), NOW()),
    (2, NULL, '2025-01-13', '成人の日', 'NATIONAL', true, false, NOW(), NOW()),
    (3, NULL, '2025-02-11', '建国記念の日', 'NATIONAL', true, false, NOW(), NOW()),
    (4, NULL, '2025-02-23', '天皇誕生日', 'NATIONAL', true, false, NOW(), NOW()),
    (5, NULL, '2025-03-20', '春分の日', 'NATIONAL', true, false, NOW(), NOW()),
    (6, NULL, '2025-04-29', '昭和の日', 'NATIONAL', true, false, NOW(), NOW()),
    (7, NULL, '2025-05-03', '憲法記念日', 'NATIONAL', true, false, NOW(), NOW()),
    (8, NULL, '2025-05-04', 'みどりの日', 'NATIONAL', true, false, NOW(), NOW()),
    (9, NULL, '2025-05-05', 'こどもの日', 'NATIONAL', true, false, NOW(), NOW()),
    (10, NULL, '2025-07-21', '海の日', 'NATIONAL', true, false, NOW(), NOW()),
    (11, NULL, '2025-08-11', '山の日', 'NATIONAL', true, false, NOW(), NOW()),
    (12, NULL, '2025-09-15', '敬老の日', 'NATIONAL', true, false, NOW(), NOW()),
    (13, NULL, '2025-09-23', '秋分の日', 'NATIONAL', true, false, NOW(), NOW()),
    (14, NULL, '2025-10-13', 'スポーツの日', 'NATIONAL', true, false, NOW(), NOW()),
    (15, NULL, '2025-11-03', '文化の日', 'NATIONAL', true, false, NOW(), NOW()),
    (16, NULL, '2025-11-23', '勤労感謝の日', 'NATIONAL', true, false, NOW(), NOW()),
    -- Company-specific holidays
    (17, 1, '2025-12-29', '年末休暇', 'COMPANY', true, false, NOW(), NOW()),
    (18, 1, '2025-12-30', '年末休暇', 'COMPANY', true, false, NOW(), NOW()),
    (19, 1, '2025-12-31', '年末休暇', 'COMPANY', true, false, NOW(), NOW()),
    (20, 3, '2025-06-15', '創立記念日', 'COMPANY', true, false, NOW(), NOW());

SELECT setval('holidays_id_seq', (SELECT MAX(id) FROM holidays));

-- =====================================================
-- 12. LEAVE_BALANCES - Số ngày nghỉ phép 2025
-- =====================================================
INSERT INTO leave_balances (id, employee_id, year, leave_type, total_days, used_days, remaining_days, deleted, created_at, updated_at) VALUES
    -- Company 1 Employees
    (1, 4, 2025, 'ANNUAL', 20, 2, 18, false, NOW(), NOW()),
    (2, 4, 2025, 'SICK', 10, 0, 10, false, NOW(), NOW()),
    (3, 5, 2025, 'ANNUAL', 15, 1, 14, false, NOW(), NOW()),
    (4, 5, 2025, 'SICK', 10, 0, 10, false, NOW(), NOW()),
    (5, 6, 2025, 'ANNUAL', 12, 0, 12, false, NOW(), NOW()),
    (6, 6, 2025, 'SICK', 10, 1, 9, false, NOW(), NOW()),
    (7, 7, 2025, 'ANNUAL', 10, 0, 10, false, NOW(), NOW()),
    (8, 7, 2025, 'SICK', 10, 0, 10, false, NOW(), NOW()),
    -- Company 2 Employees
    (9, 8, 2025, 'ANNUAL', 20, 3, 17, false, NOW(), NOW()),
    (10, 8, 2025, 'SICK', 10, 0, 10, false, NOW(), NOW()),
    (11, 9, 2025, 'ANNUAL', 10, 0, 10, false, NOW(), NOW()),
    (12, 9, 2025, 'SICK', 10, 2, 8, false, NOW(), NOW()),
    -- Company 3 Employees
    (13, 10, 2025, 'ANNUAL', 25, 5, 20, false, NOW(), NOW()),
    (14, 10, 2025, 'SICK', 15, 0, 15, false, NOW(), NOW()),
    (15, 11, 2025, 'ANNUAL', 20, 1, 19, false, NOW(), NOW()),
    (16, 11, 2025, 'SICK', 10, 0, 10, false, NOW(), NOW()),
    (17, 12, 2025, 'ANNUAL', 12, 0, 12, false, NOW(), NOW()),
    (18, 12, 2025, 'SICK', 10, 0, 10, false, NOW(), NOW());

SELECT setval('leave_balances_id_seq', (SELECT MAX(id) FROM leave_balances));

-- =====================================================
-- 13. EMPLOYEE_SALARIES - Thông tin lương nhân viên
-- =====================================================
INSERT INTO employee_salaries (id, employee_id, company_id, salary_type, monthly_salary, daily_rate, hourly_rate, effective_from, effective_to, note, deleted, created_at, updated_at) VALUES
    -- Company 1 Employees
    (1, 4, 1, 'MONTHLY', 450000, NULL, NULL, '2025-01-01', NULL, '管理職給与', false, NOW(), NOW()),
    (2, 5, 1, 'MONTHLY', 380000, NULL, NULL, '2025-01-01', NULL, 'マネージャー給与', false, NOW(), NOW()),
    (3, 6, 1, 'MONTHLY', 280000, NULL, NULL, '2025-01-01', NULL, '一般社員給与', false, NOW(), NOW()),
    (4, 7, 1, 'HOURLY', NULL, NULL, 1500, '2025-01-01', NULL, 'パートタイム', false, NOW(), NOW()),
    -- Company 2 Employees
    (5, 8, 2, 'MONTHLY', 420000, NULL, NULL, '2025-01-01', NULL, '管理職給与', false, NOW(), NOW()),
    (6, 9, 2, 'MONTHLY', 260000, NULL, NULL, '2025-01-01', NULL, '一般社員給与', false, NOW(), NOW()),
    -- Company 3 Employees
    (7, 10, 3, 'MONTHLY', 500000, NULL, NULL, '2025-01-01', NULL, '管理職給与', false, NOW(), NOW()),
    (8, 11, 3, 'MONTHLY', 400000, NULL, NULL, '2025-01-01', NULL, 'マネージャー給与', false, NOW(), NOW()),
    (9, 12, 3, 'DAILY', NULL, 15000, NULL, '2025-01-01', NULL, '日給制', false, NOW(), NOW());

SELECT setval('employee_salaries_id_seq', (SELECT MAX(id) FROM employee_salaries));

-- =====================================================
-- 14. ATTENDANCE_RECORDS - Bản ghi chấm công (January 2025)
-- =====================================================
INSERT INTO attendance_records (id, employee_id, company_id, work_date, original_check_in, original_check_out, rounded_check_in, rounded_check_out, working_minutes, overtime_minutes, late_minutes, early_leave_minutes, total_break_minutes, effective_break_minutes, break_type, break_compliant, status, deleted, created_at, updated_at) VALUES
    -- Company 1 - Employee 4 (Admin) - Week 1
    (1, 4, 1, '2025-01-06', '2025-01-06 08:55:00', '2025-01-06 18:05:00', '2025-01-06 09:00:00', '2025-01-06 18:00:00', 480, 0, 0, 0, 60, 60, 'UNPAID', true, 'PRESENT', false, NOW(), NOW()),
    (2, 4, 1, '2025-01-07', '2025-01-07 09:00:00', '2025-01-07 19:00:00', '2025-01-07 09:00:00', '2025-01-07 19:00:00', 540, 60, 0, 0, 60, 60, 'UNPAID', true, 'PRESENT', false, NOW(), NOW()),
    (3, 4, 1, '2025-01-08', '2025-01-08 09:00:00', '2025-01-08 18:00:00', '2025-01-08 09:00:00', '2025-01-08 18:00:00', 480, 0, 0, 0, 60, 60, 'UNPAID', true, 'PRESENT', false, NOW(), NOW()),
    (4, 4, 1, '2025-01-09', '2025-01-09 09:10:00', '2025-01-09 18:00:00', '2025-01-09 09:15:00', '2025-01-09 18:00:00', 465, 0, 10, 0, 60, 60, 'UNPAID', true, 'LATE', false, NOW(), NOW()),
    (5, 4, 1, '2025-01-10', '2025-01-10 09:00:00', '2025-01-10 18:00:00', '2025-01-10 09:00:00', '2025-01-10 18:00:00', 480, 0, 0, 0, 60, 60, 'UNPAID', true, 'PRESENT', false, NOW(), NOW()),
    -- Company 1 - Employee 5 (Manager) - Week 1
    (6, 5, 1, '2025-01-06', '2025-01-06 09:00:00', '2025-01-06 18:00:00', '2025-01-06 09:00:00', '2025-01-06 18:00:00', 480, 0, 0, 0, 60, 60, 'UNPAID', true, 'PRESENT', false, NOW(), NOW()),
    (7, 5, 1, '2025-01-07', '2025-01-07 09:00:00', '2025-01-07 18:00:00', '2025-01-07 09:00:00', '2025-01-07 18:00:00', 480, 0, 0, 0, 60, 60, 'UNPAID', true, 'PRESENT', false, NOW(), NOW()),
    (8, 5, 1, '2025-01-08', NULL, NULL, NULL, NULL, 0, 0, 0, 0, 0, 0, NULL, NULL, 'SICK_LEAVE', false, NOW(), NOW()),
    (9, 5, 1, '2025-01-09', '2025-01-09 09:00:00', '2025-01-09 18:00:00', '2025-01-09 09:00:00', '2025-01-09 18:00:00', 480, 0, 0, 0, 60, 60, 'UNPAID', true, 'PRESENT', false, NOW(), NOW()),
    (10, 5, 1, '2025-01-10', '2025-01-10 09:00:00', '2025-01-10 18:00:00', '2025-01-10 09:00:00', '2025-01-10 18:00:00', 480, 0, 0, 0, 60, 60, 'UNPAID', true, 'PRESENT', false, NOW(), NOW()),
    -- Company 1 - Employee 6 - Week 1 (various statuses)
    (11, 6, 1, '2025-01-06', '2025-01-06 10:00:00', '2025-01-06 19:00:00', '2025-01-06 10:00:00', '2025-01-06 19:00:00', 480, 0, 0, 0, 60, 60, 'UNPAID', true, 'PRESENT', false, NOW(), NOW()),
    (12, 6, 1, '2025-01-07', '2025-01-07 10:15:00', '2025-01-07 19:00:00', '2025-01-07 10:15:00', '2025-01-07 19:00:00', 465, 0, 15, 0, 60, 60, 'UNPAID', true, 'LATE', false, NOW(), NOW()),
    (13, 6, 1, '2025-01-08', '2025-01-08 10:00:00', '2025-01-08 18:30:00', '2025-01-08 10:00:00', '2025-01-08 18:30:00', 450, 0, 0, 30, 60, 60, 'UNPAID', true, 'EARLY_LEAVE', false, NOW(), NOW()),
    (14, 6, 1, '2025-01-09', NULL, NULL, NULL, NULL, 0, 0, 0, 0, 0, 0, NULL, NULL, 'ABSENT', false, NOW(), NOW()),
    (15, 6, 1, '2025-01-10', '2025-01-10 10:00:00', '2025-01-10 19:00:00', '2025-01-10 10:00:00', '2025-01-10 19:00:00', 480, 0, 0, 0, 60, 60, 'UNPAID', true, 'PRESENT', false, NOW(), NOW()),
    -- Company 2 - Employee 8 (Admin) - Week 1
    (16, 8, 2, '2025-01-06', '2025-01-06 09:30:00', '2025-01-06 18:30:00', '2025-01-06 09:30:00', '2025-01-06 18:30:00', 480, 0, 0, 0, 60, 60, 'UNPAID', true, 'PRESENT', false, NOW(), NOW()),
    (17, 8, 2, '2025-01-07', '2025-01-07 09:30:00', '2025-01-07 18:30:00', '2025-01-07 09:30:00', '2025-01-07 18:30:00', 480, 0, 0, 0, 60, 60, 'UNPAID', true, 'PRESENT', false, NOW(), NOW()),
    (18, 8, 2, '2025-01-08', '2025-01-08 09:30:00', '2025-01-08 18:30:00', '2025-01-08 09:30:00', '2025-01-08 18:30:00', 480, 0, 0, 0, 60, 60, 'UNPAID', true, 'PRESENT', false, NOW(), NOW()),
    (19, 8, 2, '2025-01-09', '2025-01-09 09:30:00', '2025-01-09 18:30:00', '2025-01-09 09:30:00', '2025-01-09 18:30:00', 480, 0, 0, 0, 60, 60, 'UNPAID', true, 'PRESENT', false, NOW(), NOW()),
    (20, 8, 2, '2025-01-10', '2025-01-10 09:30:00', '2025-01-10 18:30:00', '2025-01-10 09:30:00', '2025-01-10 18:30:00', 480, 0, 0, 0, 60, 60, 'UNPAID', true, 'PRESENT', false, NOW(), NOW()),
    -- Company 3 - Employee 10 (Admin) - Week 1
    (21, 10, 3, '2025-01-06', '2025-01-06 08:30:00', '2025-01-06 17:30:00', '2025-01-06 08:30:00', '2025-01-06 17:30:00', 495, 0, 0, 0, 45, 45, 'UNPAID', true, 'PRESENT', false, NOW(), NOW()),
    (22, 10, 3, '2025-01-07', '2025-01-07 08:30:00', '2025-01-07 19:00:00', '2025-01-07 08:30:00', '2025-01-07 19:00:00', 585, 90, 0, 0, 45, 45, 'UNPAID', true, 'PRESENT', false, NOW(), NOW()),
    (23, 10, 3, '2025-01-08', '2025-01-08 08:30:00', '2025-01-08 17:30:00', '2025-01-08 08:30:00', '2025-01-08 17:30:00', 495, 0, 0, 0, 45, 45, 'UNPAID', true, 'PRESENT', false, NOW(), NOW()),
    (24, 10, 3, '2025-01-09', '2025-01-09 08:30:00', '2025-01-09 17:30:00', '2025-01-09 08:30:00', '2025-01-09 17:30:00', 495, 0, 0, 0, 45, 45, 'UNPAID', true, 'PRESENT', false, NOW(), NOW()),
    (25, 10, 3, '2025-01-10', '2025-01-10 08:30:00', '2025-01-10 17:30:00', '2025-01-10 08:30:00', '2025-01-10 17:30:00', 495, 0, 0, 0, 45, 45, 'UNPAID', true, 'PRESENT', false, NOW(), NOW());

SELECT setval('attendance_records_id_seq', (SELECT MAX(id) FROM attendance_records));


-- =====================================================
-- 15. BREAK_RECORDS - Bản ghi giờ giải lao
-- =====================================================
INSERT INTO break_records (id, attendance_record_id, employee_id, company_id, work_date, break_number, break_start, break_end, actual_break_minutes, effective_break_minutes, notes, deleted, created_at, updated_at) VALUES
    -- Company 1 - Employee 4 (single breaks)
    (1, 1, 4, 1, '2025-01-06', 1, '2025-01-06 12:00:00', '2025-01-06 13:00:00', 60, 60, '昼休憩', false, NOW(), NOW()),
    (2, 2, 4, 1, '2025-01-07', 1, '2025-01-07 12:00:00', '2025-01-07 13:00:00', 60, 60, '昼休憩', false, NOW(), NOW()),
    (3, 3, 4, 1, '2025-01-08', 1, '2025-01-08 12:00:00', '2025-01-08 13:00:00', 60, 60, '昼休憩', false, NOW(), NOW()),
    (4, 4, 4, 1, '2025-01-09', 1, '2025-01-09 12:00:00', '2025-01-09 13:00:00', 60, 60, '昼休憩', false, NOW(), NOW()),
    (5, 5, 4, 1, '2025-01-10', 1, '2025-01-10 12:00:00', '2025-01-10 13:00:00', 60, 60, '昼休憩', false, NOW(), NOW()),
    -- Company 1 - Employee 5 (single breaks)
    (6, 6, 5, 1, '2025-01-06', 1, '2025-01-06 12:00:00', '2025-01-06 13:00:00', 60, 60, '昼休憩', false, NOW(), NOW()),
    (7, 7, 5, 1, '2025-01-07', 1, '2025-01-07 12:00:00', '2025-01-07 13:00:00', 60, 60, '昼休憩', false, NOW(), NOW()),
    (8, 9, 5, 1, '2025-01-09', 1, '2025-01-09 12:00:00', '2025-01-09 13:00:00', 60, 60, '昼休憩', false, NOW(), NOW()),
    (9, 10, 5, 1, '2025-01-10', 1, '2025-01-10 12:00:00', '2025-01-10 13:00:00', 60, 60, '昼休憩', false, NOW(), NOW()),
    -- Company 1 - Employee 6 (multiple breaks - flexible schedule)
    (10, 11, 6, 1, '2025-01-06', 1, '2025-01-06 10:30:00', '2025-01-06 10:45:00', 15, 15, '午前休憩', false, NOW(), NOW()),
    (11, 11, 6, 1, '2025-01-06', 2, '2025-01-06 13:00:00', '2025-01-06 13:30:00', 30, 30, '昼休憩', false, NOW(), NOW()),
    (12, 11, 6, 1, '2025-01-06', 3, '2025-01-06 16:00:00', '2025-01-06 16:15:00', 15, 15, '午後休憩', false, NOW(), NOW()),
    (13, 12, 6, 1, '2025-01-07', 1, '2025-01-07 13:00:00', '2025-01-07 14:00:00', 60, 60, '昼休憩', false, NOW(), NOW()),
    (14, 13, 6, 1, '2025-01-08', 1, '2025-01-08 13:00:00', '2025-01-08 14:00:00', 60, 60, '昼休憩', false, NOW(), NOW()),
    (15, 15, 6, 1, '2025-01-10', 1, '2025-01-10 13:00:00', '2025-01-10 14:00:00', 60, 60, '昼休憩', false, NOW(), NOW()),
    -- Company 2 - Employee 8 (fixed breaks)
    (16, 16, 8, 2, '2025-01-06', 1, '2025-01-06 12:30:00', '2025-01-06 13:30:00', 60, 60, '昼休憩', false, NOW(), NOW()),
    (17, 17, 8, 2, '2025-01-07', 1, '2025-01-07 12:30:00', '2025-01-07 13:30:00', 60, 60, '昼休憩', false, NOW(), NOW()),
    (18, 18, 8, 2, '2025-01-08', 1, '2025-01-08 12:30:00', '2025-01-08 13:30:00', 60, 60, '昼休憩', false, NOW(), NOW()),
    (19, 19, 8, 2, '2025-01-09', 1, '2025-01-09 12:30:00', '2025-01-09 13:30:00', 60, 60, '昼休憩', false, NOW(), NOW()),
    (20, 20, 8, 2, '2025-01-10', 1, '2025-01-10 12:30:00', '2025-01-10 13:30:00', 60, 60, '昼休憩', false, NOW(), NOW()),
    -- Company 3 - Employee 10 (45 min breaks)
    (21, 21, 10, 3, '2025-01-06', 1, '2025-01-06 12:00:00', '2025-01-06 12:45:00', 45, 45, '昼休憩', false, NOW(), NOW()),
    (22, 22, 10, 3, '2025-01-07', 1, '2025-01-07 12:00:00', '2025-01-07 12:45:00', 45, 45, '昼休憩', false, NOW(), NOW()),
    (23, 23, 10, 3, '2025-01-08', 1, '2025-01-08 12:00:00', '2025-01-08 12:45:00', 45, 45, '昼休憩', false, NOW(), NOW()),
    (24, 24, 10, 3, '2025-01-09', 1, '2025-01-09 12:00:00', '2025-01-09 12:45:00', 45, 45, '昼休憩', false, NOW(), NOW()),
    (25, 25, 10, 3, '2025-01-10', 1, '2025-01-10 12:00:00', '2025-01-10 12:45:00', 45, 45, '昼休憩', false, NOW(), NOW());

SELECT setval('break_records_id_seq', (SELECT MAX(id) FROM break_records));

-- =====================================================
-- 16. ATTENDANCE_ADJUSTMENT_REQUESTS - Yêu cầu điều chỉnh chấm công
-- =====================================================
INSERT INTO attendance_adjustment_requests (id, employee_id, company_id, attendance_record_id, work_date, break_record_id, original_check_in, original_check_out, original_break_start, original_break_end, requested_check_in, requested_check_out, requested_break_start, requested_break_end, reason, status, approved_by, approved_at, approver_comment, rejection_reason, deleted, created_at, updated_at) VALUES
    -- PENDING requests
    (1, 6, 1, 12, '2025-01-07', NULL, '2025-01-07 10:15:00', '2025-01-07 19:00:00', NULL, NULL, '2025-01-07 10:00:00', '2025-01-07 19:00:00', NULL, NULL, '実際は10:00に出勤しました', 'PENDING', NULL, NULL, NULL, NULL, false, NOW() - INTERVAL '2 days', NOW()),
    (2, 6, 1, 14, '2025-01-09', NULL, NULL, NULL, NULL, NULL, '2025-01-09 10:00:00', '2025-01-09 19:00:00', NULL, NULL, '出勤記録が漏れました', 'PENDING', NULL, NULL, NULL, NULL, false, NOW() - INTERVAL '1 day', NOW()),
    -- APPROVED request
    (3, 7, 1, 1, '2025-01-06', NULL, '2025-01-06 09:00:00', '2025-01-06 18:00:00', NULL, NULL, '2025-01-06 08:30:00', '2025-01-06 18:00:00', NULL, NULL, '早出勤務でした', 'APPROVED', 4, NOW() - INTERVAL '1 day', '承認しました', NULL, false, NOW() - INTERVAL '3 days', NOW()),
    -- REJECTED request
    (4, 9, 2, 16, '2025-01-06', NULL, '2025-01-06 09:30:00', '2025-01-06 18:30:00', NULL, NULL, '2025-01-06 09:00:00', '2025-01-06 18:30:00', NULL, NULL, '出勤時間を修正したい', 'REJECTED', 8, NOW() - INTERVAL '2 days', NULL, '証拠が不十分です', false, NOW() - INTERVAL '4 days', NOW()),
    -- Break adjustment request
    (5, 6, 1, 11, '2025-01-06', 11, '2025-01-06 10:00:00', '2025-01-06 19:00:00', '2025-01-06 13:00:00', '2025-01-06 13:30:00', '2025-01-06 10:00:00', '2025-01-06 19:00:00', '2025-01-06 13:00:00', '2025-01-06 14:00:00', '休憩時間を1時間に修正', 'PENDING', NULL, NULL, NULL, NULL, false, NOW() - INTERVAL '12 hours', NOW());

SELECT setval('attendance_adjustment_requests_id_seq', (SELECT MAX(id) FROM attendance_adjustment_requests));

-- =====================================================
-- 17. LEAVE_REQUESTS - Yêu cầu nghỉ phép
-- =====================================================
INSERT INTO leave_requests (id, employee_id, company_id, leave_type, start_date, end_date, total_days, reason, status, approved_by, approved_at, rejection_reason, deleted, created_at, updated_at) VALUES
    -- APPROVED
    (1, 4, 1, 'ANNUAL', '2025-01-20', '2025-01-21', 2, '私用のため', 'APPROVED', 4, NOW() - INTERVAL '10 days', NULL, false, NOW() - INTERVAL '15 days', NOW()),
    (2, 5, 1, 'SICK', '2025-01-08', '2025-01-08', 1, '体調不良のため', 'APPROVED', 4, NOW() - INTERVAL '5 days', NULL, false, NOW() - INTERVAL '7 days', NOW()),
    (3, 10, 3, 'ANNUAL', '2025-01-27', '2025-01-31', 5, '年末年始休暇延長', 'APPROVED', 10, NOW() - INTERVAL '20 days', NULL, false, NOW() - INTERVAL '25 days', NOW()),
    -- PENDING
    (4, 6, 1, 'ANNUAL', '2025-02-10', '2025-02-14', 5, '旅行のため', 'PENDING', NULL, NULL, NULL, false, NOW() - INTERVAL '2 days', NOW()),
    (5, 9, 2, 'SICK', '2025-01-22', '2025-01-22', 1, '通院のため', 'PENDING', NULL, NULL, NULL, false, NOW() - INTERVAL '3 days', NOW()),
    (6, 12, 3, 'ANNUAL', '2025-02-20', '2025-02-21', 2, '私用', 'PENDING', NULL, NULL, NULL, false, NOW() - INTERVAL '1 day', NOW()),
    -- REJECTED
    (7, 7, 1, 'ANNUAL', '2025-01-25', '2025-01-31', 5, '帰省のため', 'REJECTED', 4, NOW() - INTERVAL '3 days', '繁忙期のため承認できません', false, NOW() - INTERVAL '8 days', NOW());

SELECT setval('leave_requests_id_seq', (SELECT MAX(id) FROM leave_requests));

-- =====================================================
-- 18. PAYROLL_RECORDS - Bản ghi lương (December 2024)
-- =====================================================
INSERT INTO payroll_records (id, employee_id, company_id, year, month, salary_type, base_salary, working_days, working_hours, regular_overtime_pay, night_overtime_pay, holiday_overtime_pay, weekend_overtime_pay, total_overtime_pay, regular_overtime_hours, night_overtime_hours, holiday_overtime_hours, weekend_overtime_hours, allowance_details, total_allowances, deduction_details, total_deductions, total_break_minutes, break_type, break_deduction_amount, gross_salary, net_salary, status, payment_status, paid_at, payment_reference, notification_sent, notification_sent_at, finalized_at, finalized_by, deleted, created_at, updated_at) VALUES
    -- Company 1 - December 2024 (FINALIZED & PAID)
    (1, 4, 1, 2024, 12, 'MONTHLY', 450000, 22, 176, 15000, 0, 0, 0, 15000, 8, 0, 0, 0, '[{"code": "TRANSPORT", "amount": 10000}, {"code": "MEAL", "amount": 5000}]', 15000, '[{"code": "HEALTH_INS", "amount": 22500}, {"code": "PENSION", "amount": 41175}]', 63675, 1320, 'UNPAID', 0, 480000, 416325, 'FINALIZED', 'PAID', '2024-12-25 10:00:00', 'PAY-2024-12-001', true, '2024-12-25 10:00:00', '2024-12-24 18:00:00', 4, false, NOW(), NOW()),
    (2, 5, 1, 2024, 12, 'MONTHLY', 380000, 22, 176, 10000, 0, 0, 0, 10000, 6, 0, 0, 0, '[{"code": "TRANSPORT", "amount": 10000}, {"code": "MEAL", "amount": 5000}]', 15000, '[{"code": "HEALTH_INS", "amount": 19000}, {"code": "PENSION", "amount": 34770}]', 53770, 1320, 'UNPAID', 0, 405000, 351230, 'FINALIZED', 'PAID', '2024-12-25 10:00:00', 'PAY-2024-12-002', true, '2024-12-25 10:00:00', '2024-12-24 18:00:00', 4, false, NOW(), NOW()),
    (3, 6, 1, 2024, 12, 'MONTHLY', 280000, 20, 160, 0, 0, 0, 0, 0, 0, 0, 0, 0, '[{"code": "TRANSPORT", "amount": 10000}, {"code": "MEAL", "amount": 5000}]', 15000, '[{"code": "HEALTH_INS", "amount": 14000}, {"code": "PENSION", "amount": 25620}]', 39620, 1200, 'UNPAID', 0, 295000, 255380, 'FINALIZED', 'PAID', '2024-12-25 10:00:00', 'PAY-2024-12-003', true, '2024-12-25 10:00:00', '2024-12-24 18:00:00', 4, false, NOW(), NOW()),
    -- Company 1 - January 2025 (CALCULATED, not finalized)
    (4, 4, 1, 2025, 1, 'MONTHLY', 450000, 20, 160, 20000, 0, 0, 0, 20000, 10, 0, 0, 0, '[{"code": "TRANSPORT", "amount": 10000}, {"code": "MEAL", "amount": 5000}]', 15000, '[{"code": "HEALTH_INS", "amount": 22500}, {"code": "PENSION", "amount": 41175}]', 63675, 1200, 'UNPAID', 0, 485000, 421325, 'CALCULATED', 'PENDING', NULL, NULL, false, NULL, NULL, NULL, false, NOW(), NOW()),
    (5, 5, 1, 2025, 1, 'MONTHLY', 380000, 19, 152, 0, 0, 0, 0, 0, 0, 0, 0, 0, '[{"code": "TRANSPORT", "amount": 10000}, {"code": "MEAL", "amount": 5000}]', 15000, '[{"code": "HEALTH_INS", "amount": 19000}, {"code": "PENSION", "amount": 34770}]', 53770, 1140, 'UNPAID', 0, 395000, 341230, 'CALCULATED', 'PENDING', NULL, NULL, false, NULL, NULL, NULL, false, NOW(), NOW()),
    -- Company 2 - December 2024 (FINALIZED & PAID)
    (6, 8, 2, 2024, 12, 'MONTHLY', 420000, 22, 176, 0, 0, 0, 0, 0, 0, 0, 0, 0, '[{"code": "TRANSPORT", "amount": 15000}]', 15000, '[{"code": "HEALTH_INS", "amount": 21000}]', 21000, 1320, 'UNPAID', 0, 435000, 414000, 'FINALIZED', 'PAID', '2024-12-25 10:00:00', 'PAY-2024-12-004', true, '2024-12-25 10:00:00', '2024-12-24 18:00:00', 8, false, NOW(), NOW()),
    -- Company 2 - January 2025 (DRAFT)
    (7, 8, 2, 2025, 1, 'MONTHLY', 420000, 18, 144, 0, 0, 0, 0, 0, 0, 0, 0, 0, '[{"code": "TRANSPORT", "amount": 15000}]', 15000, '[]', 0, 1080, 'UNPAID', 0, 435000, 435000, 'DRAFT', 'PENDING', NULL, NULL, false, NULL, NULL, NULL, false, NOW(), NOW()),
    -- Company 3 - December 2024 (FINALIZED & PAID)
    (8, 10, 3, 2024, 12, 'MONTHLY', 500000, 20, 160, 30000, 0, 0, 0, 30000, 12, 0, 0, 0, '[{"code": "TRANSPORT", "amount": 20000}, {"code": "HOUSING", "amount": 30000}]', 50000, '[{"code": "HEALTH_INS", "amount": 25000}, {"code": "PENSION", "amount": 45750}, {"code": "INCOME_TAX", "amount": 50000}]', 120750, 900, 'UNPAID', 0, 580000, 459250, 'FINALIZED', 'PAID', '2024-12-28 10:00:00', 'PAY-2024-12-005', true, '2024-12-28 10:00:00', '2024-12-27 18:00:00', 10, false, NOW(), NOW()),
    -- Company 3 - January 2025 (CALCULATED)
    (9, 10, 3, 2025, 1, 'MONTHLY', 500000, 18, 144, 25000, 0, 0, 0, 25000, 10, 0, 0, 0, '[{"code": "TRANSPORT", "amount": 20000}, {"code": "HOUSING", "amount": 30000}]', 50000, '[{"code": "HEALTH_INS", "amount": 25000}, {"code": "PENSION", "amount": 45750}, {"code": "INCOME_TAX", "amount": 50000}]', 120750, 810, 'UNPAID', 0, 575000, 454250, 'CALCULATED', 'PENDING', NULL, NULL, false, NULL, NULL, NULL, false, NOW(), NOW());

SELECT setval('payroll_records_id_seq', (SELECT MAX(id) FROM payroll_records));

-- =====================================================
-- 19. SCHEDULE_SELECTIONS - Lựa chọn lịch làm việc
-- =====================================================
INSERT INTO schedule_selections (id, employee_id, company_id, schedule_id, effective_from, effective_to, status, approved_by, approved_at, rejection_reason, deleted, created_at, updated_at) VALUES
    (1, 6, 1, 2, '2025-02-01', NULL, 'APPROVED', 4, NOW() - INTERVAL '5 days', NULL, false, NOW() - INTERVAL '10 days', NOW()),
    (2, 7, 1, 2, '2025-02-15', NULL, 'PENDING', NULL, NULL, NULL, false, NOW() - INTERVAL '2 days', NOW()),
    (3, 12, 3, 4, '2025-01-20', NULL, 'REJECTED', 10, NOW() - INTERVAL '3 days', '現在のシフトを維持してください', false, NOW() - INTERVAL '7 days', NOW());

SELECT setval('schedule_selections_id_seq', (SELECT MAX(id) FROM schedule_selections));
