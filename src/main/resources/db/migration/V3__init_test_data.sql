-- =====================================================
-- TAMABEE HR - TEST DATA
-- Email: tamachan.test1@gmail.com -> tamachan.test10@gmail.com
-- Password: hiep1234
-- Currency: JPY (Japanese Yen)
-- =====================================================

-- =====================================================
-- 1. USERS - Người dùng test
-- Password: hiep1234 (BCrypt: $2a$12$.dYHl1TIT3mE9x9MAtrANuJlMTKNThO1TWv./AWl.Y3XCOIGvfkMW)
-- Employee Code Format: prefix (25-99) + 2 số cuối companyId + ngày (2 số) + tháng (2 số)
-- Tamabee users: companyId = 0, nên 2 số cuối = 00
-- =====================================================
INSERT INTO users (id, employee_code, email, password, role, status, locale, language, company_id, profile_completeness, deleted, created_at, updated_at) VALUES
    -- Tamabee Staff (companyId = 0)
    (1, '25001501', 'tamachan.test1@gmail.com', '$2a$12$.dYHl1TIT3mE9x9MAtrANuJlMTKNThO1TWv./AWl.Y3XCOIGvfkMW', 'ADMIN_TAMABEE', 'ACTIVE', 'Asia/Tokyo', 'ja', 0, 50, false, NOW(), NOW()),
    (2, '25002005', 'tamachan.test2@gmail.com', '$2a$12$.dYHl1TIT3mE9x9MAtrANuJlMTKNThO1TWv./AWl.Y3XCOIGvfkMW', 'MANAGER_TAMABEE', 'ACTIVE', 'Asia/Tokyo', 'ja', 0, 30, false, NOW(), NOW()),
    (3, '25001008', 'tamachan.test3@gmail.com', '$2a$12$.dYHl1TIT3mE9x9MAtrANuJlMTKNThO1TWv./AWl.Y3XCOIGvfkMW', 'EMPLOYEE_TAMABEE', 'ACTIVE', 'Asia/Tokyo', 'ja', 0, 20, false, NOW(), NOW()),
    -- Company 1 Staff (companyId = 1, 2 số cuối = 01)
    (4, '25012503', 'tamachan.test4@gmail.com', '$2a$12$.dYHl1TIT3mE9x9MAtrANuJlMTKNThO1TWv./AWl.Y3XCOIGvfkMW', 'ADMIN_COMPANY', 'ACTIVE', 'Asia/Tokyo', 'ja', 1, 80, false, NOW(), NOW()),
    (5, '25011207', 'tamachan.test5@gmail.com', '$2a$12$.dYHl1TIT3mE9x9MAtrANuJlMTKNThO1TWv./AWl.Y3XCOIGvfkMW', 'MANAGER_COMPANY', 'ACTIVE', 'Asia/Tokyo', 'ja', 1, 60, false, NOW(), NOW()),
    (6, '25013011', 'tamachan.test6@gmail.com', '$2a$12$.dYHl1TIT3mE9x9MAtrANuJlMTKNThO1TWv./AWl.Y3XCOIGvfkMW', 'EMPLOYEE_COMPANY', 'ACTIVE', 'Asia/Tokyo', 'ja', 1, 40, false, NOW(), NOW()),
    -- Company 2 Staff (companyId = 2, 2 số cuối = 02)
    (7, '25021804', 'tamachan.test7@gmail.com', '$2a$12$.dYHl1TIT3mE9x9MAtrANuJlMTKNThO1TWv./AWl.Y3XCOIGvfkMW', 'ADMIN_COMPANY', 'ACTIVE', 'Asia/Tokyo', 'ja', 2, 70, false, NOW(), NOW()),
    (8, '25020509', 'tamachan.test8@gmail.com', '$2a$12$.dYHl1TIT3mE9x9MAtrANuJlMTKNThO1TWv./AWl.Y3XCOIGvfkMW', 'EMPLOYEE_COMPANY', 'ACTIVE', 'Asia/Tokyo', 'ja', 2, 25, false, NOW(), NOW()),
    -- Company 3 Staff (companyId = 3, 2 số cuối = 03)
    (9, '25030112', 'tamachan.test9@gmail.com', '$2a$12$.dYHl1TIT3mE9x9MAtrANuJlMTKNThO1TWv./AWl.Y3XCOIGvfkMW', 'ADMIN_COMPANY', 'ACTIVE', 'Asia/Tokyo', 'ja', 3, 90, false, NOW(), NOW()),
    (10, '25031506', 'tamachan.test10@gmail.com', '$2a$12$.dYHl1TIT3mE9x9MAtrANuJlMTKNThO1TWv./AWl.Y3XCOIGvfkMW', 'EMPLOYEE_COMPANY', 'ACTIVE', 'Asia/Tokyo', 'ja', 3, 35, false, NOW(), NOW()),
    -- Additional Tamabee Employees for referral testing
    (11, '25001203', 'employee.tamabee1@test.com', '$2a$12$.dYHl1TIT3mE9x9MAtrANuJlMTKNThO1TWv./AWl.Y3XCOIGvfkMW', 'EMPLOYEE_TAMABEE', 'ACTIVE', 'Asia/Tokyo', 'ja', 0, 45, false, NOW(), NOW()),
    (12, '25002507', 'employee.tamabee2@test.com', '$2a$12$.dYHl1TIT3mE9x9MAtrANuJlMTKNThO1TWv./AWl.Y3XCOIGvfkMW', 'EMPLOYEE_TAMABEE', 'ACTIVE', 'Asia/Tokyo', 'ja', 0, 55, false, NOW(), NOW()),
    -- Additional Company 1 Employees
    (13, '26010815', 'employee1.company1@test.com', '$2a$12$.dYHl1TIT3mE9x9MAtrANuJlMTKNThO1TWv./AWl.Y3XCOIGvfkMW', 'EMPLOYEE_COMPANY', 'ACTIVE', 'Asia/Tokyo', 'ja', 1, 30, false, NOW(), NOW()),
    (14, '26012209', 'employee2.company1@test.com', '$2a$12$.dYHl1TIT3mE9x9MAtrANuJlMTKNThO1TWv./AWl.Y3XCOIGvfkMW', 'EMPLOYEE_COMPANY', 'ACTIVE', 'Asia/Tokyo', 'ja', 1, 25, false, NOW(), NOW()),
    -- Additional Company 2 Employees
    (15, '26020310', 'employee1.company2@test.com', '$2a$12$.dYHl1TIT3mE9x9MAtrANuJlMTKNThO1TWv./AWl.Y3XCOIGvfkMW', 'EMPLOYEE_COMPANY', 'ACTIVE', 'Asia/Tokyo', 'ja', 2, 35, false, NOW(), NOW()),
    -- Additional Company 3 Employees
    (16, '26031102', 'employee1.company3@test.com', '$2a$12$.dYHl1TIT3mE9x9MAtrANuJlMTKNThO1TWv./AWl.Y3XCOIGvfkMW', 'EMPLOYEE_COMPANY', 'ACTIVE', 'Asia/Tokyo', 'ja', 3, 40, false, NOW(), NOW()),
    (17, '26032808', 'employee2.company3@test.com', '$2a$12$.dYHl1TIT3mE9x9MAtrANuJlMTKNThO1TWv./AWl.Y3XCOIGvfkMW', 'EMPLOYEE_COMPANY', 'ACTIVE', 'Asia/Tokyo', 'ja', 3, 28, false, NOW(), NOW()),
    -- Company 4 Staff (companyId = 4, 2 số cuối = 04)
    (18, '25040501', 'admin.company4@test.com', '$2a$12$.dYHl1TIT3mE9x9MAtrANuJlMTKNThO1TWv./AWl.Y3XCOIGvfkMW', 'ADMIN_COMPANY', 'ACTIVE', 'Asia/Tokyo', 'ja', 4, 75, false, NOW(), NOW()),
    (19, '25041507', 'employee1.company4@test.com', '$2a$12$.dYHl1TIT3mE9x9MAtrANuJlMTKNThO1TWv./AWl.Y3XCOIGvfkMW', 'EMPLOYEE_COMPANY', 'ACTIVE', 'Asia/Tokyo', 'ja', 4, 32, false, NOW(), NOW()),
    -- Company 5 Staff (companyId = 5, 2 số cuối = 05)
    (20, '25051003', 'admin.company5@test.com', '$2a$12$.dYHl1TIT3mE9x9MAtrANuJlMTKNThO1TWv./AWl.Y3XCOIGvfkMW', 'ADMIN_COMPANY', 'ACTIVE', 'Asia/Tokyo', 'ja', 5, 85, false, NOW(), NOW()),
    (21, '25052012', 'employee1.company5@test.com', '$2a$12$.dYHl1TIT3mE9x9MAtrANuJlMTKNThO1TWv./AWl.Y3XCOIGvfkMW', 'EMPLOYEE_COMPANY', 'ACTIVE', 'Asia/Tokyo', 'ja', 5, 38, false, NOW(), NOW()),
    -- Company 6 Staff (companyId = 6, 2 số cuối = 06)
    (22, '25060806', 'admin.company6@test.com', '$2a$12$.dYHl1TIT3mE9x9MAtrANuJlMTKNThO1TWv./AWl.Y3XCOIGvfkMW', 'ADMIN_COMPANY', 'ACTIVE', 'Asia/Tokyo', 'ja', 6, 65, false, NOW(), NOW()),
    (23, '26061510', 'employee1.company6@test.com', '$2a$12$.dYHl1TIT3mE9x9MAtrANuJlMTKNThO1TWv./AWl.Y3XCOIGvfkMW', 'EMPLOYEE_COMPANY', 'ACTIVE', 'Asia/Tokyo', 'ja', 6, 42, false, NOW(), NOW()),
    -- Company 7 Staff (companyId = 7, 2 số cuối = 07)
    (24, '25070203', 'admin.company7@test.com', '$2a$12$.dYHl1TIT3mE9x9MAtrANuJlMTKNThO1TWv./AWl.Y3XCOIGvfkMW', 'ADMIN_COMPANY', 'ACTIVE', 'Asia/Tokyo', 'ja', 7, 78, false, NOW(), NOW()),
    (25, '25071809', 'manager.company7@test.com', '$2a$12$.dYHl1TIT3mE9x9MAtrANuJlMTKNThO1TWv./AWl.Y3XCOIGvfkMW', 'MANAGER_COMPANY', 'ACTIVE', 'Asia/Tokyo', 'ja', 7, 55, false, NOW(), NOW()),
    (26, '26072504', 'employee1.company7@test.com', '$2a$12$.dYHl1TIT3mE9x9MAtrANuJlMTKNThO1TWv./AWl.Y3XCOIGvfkMW', 'EMPLOYEE_COMPANY', 'ACTIVE', 'Asia/Tokyo', 'ja', 7, 33, false, NOW(), NOW()),
    -- Company 8 Staff (companyId = 8, 2 số cuối = 08)
    (27, '25080711', 'admin.company8@test.com', '$2a$12$.dYHl1TIT3mE9x9MAtrANuJlMTKNThO1TWv./AWl.Y3XCOIGvfkMW', 'ADMIN_COMPANY', 'ACTIVE', 'Asia/Tokyo', 'ja', 8, 82, false, NOW(), NOW()),
    (28, '26081206', 'employee1.company8@test.com', '$2a$12$.dYHl1TIT3mE9x9MAtrANuJlMTKNThO1TWv./AWl.Y3XCOIGvfkMW', 'EMPLOYEE_COMPANY', 'ACTIVE', 'Asia/Tokyo', 'ja', 8, 29, false, NOW(), NOW()),
    -- Company 9 Staff (companyId = 9, 2 số cuối = 09)
    (29, '25091405', 'admin.company9@test.com', '$2a$12$.dYHl1TIT3mE9x9MAtrANuJlMTKNThO1TWv./AWl.Y3XCOIGvfkMW', 'ADMIN_COMPANY', 'ACTIVE', 'Asia/Tokyo', 'ja', 9, 88, false, NOW(), NOW()),
    (30, '26092108', 'employee1.company9@test.com', '$2a$12$.dYHl1TIT3mE9x9MAtrANuJlMTKNThO1TWv./AWl.Y3XCOIGvfkMW', 'EMPLOYEE_COMPANY', 'ACTIVE', 'Asia/Tokyo', 'ja', 9, 36, false, NOW(), NOW()),
    -- Company 10 Staff (companyId = 10, 2 số cuối = 10)
    (31, '25100912', 'admin.company10@test.com', '$2a$12$.dYHl1TIT3mE9x9MAtrANuJlMTKNThO1TWv./AWl.Y3XCOIGvfkMW', 'ADMIN_COMPANY', 'ACTIVE', 'Asia/Tokyo', 'ja', 10, 72, false, NOW(), NOW()),
    (32, '25101607', 'manager.company10@test.com', '$2a$12$.dYHl1TIT3mE9x9MAtrANuJlMTKNThO1TWv./AWl.Y3XCOIGvfkMW', 'MANAGER_COMPANY', 'ACTIVE', 'Asia/Tokyo', 'ja', 10, 48, false, NOW(), NOW()),
    (33, '26102301', 'employee1.company10@test.com', '$2a$12$.dYHl1TIT3mE9x9MAtrANuJlMTKNThO1TWv./AWl.Y3XCOIGvfkMW', 'EMPLOYEE_COMPANY', 'ACTIVE', 'Asia/Tokyo', 'ja', 10, 31, false, NOW(), NOW()),
    -- Additional Tamabee Employees for more referral testing
    (34, '25003006', 'employee.tamabee3@test.com', '$2a$12$.dYHl1TIT3mE9x9MAtrANuJlMTKNThO1TWv./AWl.Y3XCOIGvfkMW', 'EMPLOYEE_TAMABEE', 'ACTIVE', 'Asia/Tokyo', 'ja', 0, 52, false, NOW(), NOW()),
    (35, '25001511', 'employee.tamabee4@test.com', '$2a$12$.dYHl1TIT3mE9x9MAtrANuJlMTKNThO1TWv./AWl.Y3XCOIGvfkMW', 'EMPLOYEE_TAMABEE', 'ACTIVE', 'Asia/Tokyo', 'ja', 0, 48, false, NOW(), NOW());

SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));

-- =====================================================
-- 2. USER_PROFILES - Thông tin profile
-- Referral Code Format: 8 ký tự ngẫu nhiên (A-Z, 0-9)
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
    -- Company 2 Staff
    (7, 7, '佐藤美咲', '0377777777', '東京都新宿区6-6-6', '1600022', '1989-04-18', 'FEMALE', NULL, 'JP', 'Rakuten', '6677889', 'SATO MISAKI', NOW(), NOW()),
    (8, 8, '伊藤健太', '0388888888', NULL, NULL, '1994-09-05', 'MALE', NULL, 'VN', NULL, NULL, NULL, NOW(), NOW()),
    -- Company 3 Staff
    (9, 9, '田中一郎', '0399999999', '東京都港区7-7-7', '1050001', '1985-12-01', 'MALE', NULL, 'JP', 'MUFG', '9988776', 'TANAKA ICHIRO', NOW(), NOW()),
    (10, 10, '渡辺さくら', '0300000000', '東京都港区8-8-8', '1050002', '1996-06-15', 'FEMALE', NULL, 'VN', NULL, NULL, NULL, NOW(), NOW()),
    -- Additional Tamabee Employees
    (11, 11, '中村健一', '0311112222', '東京都品川区1-1-1', '1400001', '1993-03-12', 'MALE', 'TMEMP011', 'JP', 'SMBC', '1112223', 'NAKAMURA KENICHI', NOW(), NOW()),
    (12, 12, '小林美香', '0322223333', '東京都目黒区2-2-2', '1530001', '1991-07-25', 'FEMALE', 'TMEMP012', 'JP', 'MUFG', '3334445', 'KOBAYASHI MIKA', NOW(), NOW()),
    -- Additional Company 1 Employees
    (13, 13, '加藤大輔', '0333334444', '東京都渋谷区6-6-6', '1500003', '1990-08-15', 'MALE', NULL, 'JP', 'Mizuho', '4445556', 'KATO DAISUKE', NOW(), NOW()),
    (14, 14, '吉田真理', '0344445555', '東京都渋谷区7-7-7', '1500004', '1992-09-22', 'FEMALE', NULL, 'JP', 'SMBC', '5556667', 'YOSHIDA MARI', NOW(), NOW()),
    -- Additional Company 2 Employees
    (15, 15, '山本翔太', '0355556666', '東京都新宿区8-8-8', '1600023', '1994-10-03', 'MALE', NULL, 'VN', NULL, NULL, NULL, NOW(), NOW()),
    -- Additional Company 3 Employees
    (16, 16, '井上愛', '0366667777', '東京都港区9-9-9', '1050003', '1995-02-11', 'FEMALE', NULL, 'JP', 'Rakuten', '7778889', 'INOUE AI', NOW(), NOW()),
    (17, 17, '木村拓也', '0377778888', '東京都港区10-10-10', '1050004', '1993-08-28', 'MALE', NULL, 'VN', NULL, NULL, NULL, NOW(), NOW()),
    -- Company 4 Staff
    (18, 18, '松本浩二', '0388889999', '大阪府大阪市北区1-1-1', '5300001', '1987-01-05', 'MALE', NULL, 'JP', 'MUFG', '8889990', 'MATSUMOTO KOJI', NOW(), NOW()),
    (19, 19, '林美穂', '0399990000', '大阪府大阪市北区2-2-2', '5300002', '1995-07-15', 'FEMALE', NULL, 'JP', 'SMBC', '9990001', 'HAYASHI MIHO', NOW(), NOW()),
    -- Company 5 Staff
    (20, 20, '清水健太郎', '0300001111', '福岡県福岡市中央区1-1-1', '8100001', '1989-03-10', 'MALE', NULL, 'JP', 'Mizuho', '0001112', 'SHIMIZU KENTARO', NOW(), NOW()),
    (21, 21, '森田由美', '0311112222', '福岡県福岡市中央区2-2-2', '8100002', '1994-12-20', 'FEMALE', NULL, 'VN', NULL, NULL, NULL, NOW(), NOW()),
    -- Company 6 Staff
    (22, 22, '池田誠', '0322223333', '名古屋市中区1-1-1', '4600001', '1991-06-08', 'MALE', NULL, 'JP', 'MUFG', '2223334', 'IKEDA MAKOTO', NOW(), NOW()),
    (23, 23, '斎藤優子', '0333334444', '名古屋市中区2-2-2', '4600002', '1993-10-15', 'FEMALE', NULL, 'JP', 'SMBC', '3334445', 'SAITO YUKO', NOW(), NOW()),
    -- Company 7 Staff
    (24, 24, '藤田健', '0344445555', '札幌市中央区1-1-1', '0600001', '1988-02-03', 'MALE', NULL, 'JP', 'Mizuho', '4445556', 'FUJITA KEN', NOW(), NOW()),
    (25, 25, '岡田美紀', '0355556666', '札幌市中央区2-2-2', '0600002', '1990-09-18', 'FEMALE', NULL, 'JP', 'MUFG', '5556667', 'OKADA MIKI', NOW(), NOW()),
    (26, 26, '前田翔', '0366667777', '札幌市中央区3-3-3', '0600003', '1995-04-25', 'MALE', NULL, 'VN', NULL, NULL, NULL, NOW(), NOW()),
    -- Company 8 Staff
    (27, 27, '石井大輔', '0377778888', '仙台市青葉区1-1-1', '9800001', '1987-07-11', 'MALE', NULL, 'JP', 'Rakuten', '6667778', 'ISHII DAISUKE', NOW(), NOW()),
    (28, 28, '長谷川愛', '0388889999', '仙台市青葉区2-2-2', '9800002', '1994-06-12', 'FEMALE', NULL, 'JP', 'SMBC', '7778889', 'HASEGAWA AI', NOW(), NOW()),
    -- Company 9 Staff
    (29, 29, '近藤太一', '0399990000', '広島市中区1-1-1', '7300001', '1989-05-14', 'MALE', NULL, 'JP', 'MUFG', '8889990', 'KONDO TAICHI', NOW(), NOW()),
    (30, 30, '村上さやか', '0300001111', '広島市中区2-2-2', '7300002', '1992-08-21', 'FEMALE', NULL, 'VN', NULL, NULL, NULL, NOW(), NOW()),
    -- Company 10 Staff
    (31, 31, '遠藤正樹', '0311112222', '神戸市中央区1-1-1', '6500001', '1986-12-09', 'MALE', NULL, 'JP', 'Mizuho', '9990001', 'ENDO MASAKI', NOW(), NOW()),
    (32, 32, '青木恵', '0322223333', '神戸市中央区2-2-2', '6500002', '1991-07-16', 'FEMALE', NULL, 'JP', 'MUFG', '0001112', 'AOKI MEGUMI', NOW(), NOW()),
    (33, 33, '坂本龍', '0333334444', '神戸市中央区3-3-3', '6500003', '1994-01-23', 'MALE', NULL, 'VN', NULL, NULL, NULL, NOW(), NOW()),
    -- Additional Tamabee Employees
    (34, 34, '原田真一', '0344445555', '東京都世田谷区1-1-1', '1540001', '1990-06-30', 'MALE', 'TMEMP034', 'JP', 'SMBC', '1112223', 'HARADA SHINICHI', NOW(), NOW()),
    (35, 35, '福田麻衣', '0355556666', '東京都杉並区1-1-1', '1660001', '1993-11-15', 'FEMALE', 'TMEMP035', 'JP', 'MUFG', '2223334', 'FUKUDA MAI', NOW(), NOW());

SELECT setval('user_profiles_id_seq', (SELECT MAX(id) FROM user_profiles));



-- =====================================================
-- 3. COMPANIES - Công ty test
-- referred_by_employee_id: ID của user Tamabee đã giới thiệu
-- =====================================================
INSERT INTO companies (id, name, owner_name, email, phone, address, industry, zipcode, locale, language, plan_id, referred_by_employee_id, owner_id, status, deleted, created_at, updated_at) VALUES
    -- Companies referred by Admin Tamabee (user_id = 1)
    (1, 'ABC Tech株式会社', '山田太郎', 'company1@test.com', '0312345678', '東京都渋谷区1-2-3', 'technology', '1500001', 'Asia/Tokyo', 'ja', 2, 1, 4, 'ACTIVE', false, NOW() - INTERVAL '60 days', NOW()),
    -- Companies referred by Manager Tamabee (user_id = 2)
    (2, 'XYZ Solutions株式会社', '佐藤美咲', 'company2@test.com', '0323456789', '東京都新宿区4-5-6', 'technology', '1600022', 'Asia/Tokyo', 'ja', 1, 2, 7, 'ACTIVE', false, NOW() - INTERVAL '45 days', NOW()),
    -- Companies without referral
    (3, 'Tokyo Corp', '田中一郎', 'company3@test.com', '0334567890', '東京都港区7-8-9', 'finance', '1050001', 'Asia/Tokyo', 'ja', 3, NULL, 9, 'ACTIVE', false, NOW() - INTERVAL '90 days', NOW()),
    -- Companies referred by Employee Tamabee (user_id = 3)
    (4, 'Osaka Digital株式会社', '松本浩二', 'company4@test.com', '0645678901', '大阪府大阪市北区1-1-1', 'technology', '5300001', 'Asia/Tokyo', 'ja', 2, 3, 18, 'ACTIVE', false, NOW() - INTERVAL '30 days', NOW()),
    (5, 'Fukuoka Systems株式会社', '清水健太郎', 'company5@test.com', '0926789012', '福岡県福岡市中央区1-1-1', 'technology', '8100001', 'Asia/Tokyo', 'ja', 1, 3, 20, 'ACTIVE', false, NOW() - INTERVAL '20 days', NOW()),
    -- Companies referred by Additional Employee Tamabee (user_id = 11)
    (6, 'Nagoya Tech株式会社', '池田誠', 'company6@test.com', '0527890123', '名古屋市中区1-1-1', 'manufacturing', '4600001', 'Asia/Tokyo', 'ja', 2, 11, 22, 'ACTIVE', false, NOW() - INTERVAL '15 days', NOW()),
    -- Companies referred by Employee Tamabee 2 (user_id = 12)
    (7, 'Sapporo IT株式会社', '藤田健', 'company7@test.com', '0118901234', '札幌市中央区1-1-1', 'technology', '0600001', 'Asia/Tokyo', 'ja', 3, 12, 24, 'ACTIVE', false, NOW() - INTERVAL '40 days', NOW()),
    -- Companies referred by Additional Employee Tamabee 3 (user_id = 34)
    (8, 'Sendai Systems株式会社', '石井大輔', 'company8@test.com', '0229012345', '仙台市青葉区1-1-1', 'technology', '9800001', 'Asia/Tokyo', 'ja', 2, 34, 27, 'ACTIVE', false, NOW() - INTERVAL '25 days', NOW()),
    -- Companies referred by Additional Employee Tamabee 4 (user_id = 35)
    (9, 'Hiroshima Digital株式会社', '近藤太一', 'company9@test.com', '0820123456', '広島市中区1-1-1', 'finance', '7300001', 'Asia/Tokyo', 'ja', 1, 35, 29, 'ACTIVE', false, NOW() - INTERVAL '35 days', NOW()),
    -- Companies without referral
    (10, 'Kobe Corp株式会社', '遠藤正樹', 'company10@test.com', '0781234567', '神戸市中央区1-1-1', 'retail', '6500001', 'Asia/Tokyo', 'ja', 2, NULL, 31, 'ACTIVE', false, NOW() - INTERVAL '50 days', NOW());

SELECT setval('companies_id_seq', (SELECT MAX(id) FROM companies));

-- =====================================================
-- 4. WALLETS - Ví tiền công ty (JPY)
-- total_billing: Tổng số tiền billing đã tính cho company
-- =====================================================
INSERT INTO wallets (id, company_id, balance, total_billing, last_billing_date, next_billing_date, free_trial_end_date, created_at, updated_at) VALUES
    (1, 1, 50000, 80000, NOW() - INTERVAL '30 days', NOW() + INTERVAL '30 days', NULL, NOW() - INTERVAL '60 days', NOW()),
    (2, 2, 15000, 25000, NOW() - INTERVAL '15 days', NOW() + INTERVAL '15 days', NOW() + INTERVAL '7 days', NOW() - INTERVAL '45 days', NOW()),
    (3, 3, 100000, 120000, NOW() - INTERVAL '20 days', NOW() + INTERVAL '10 days', NULL, NOW() - INTERVAL '90 days', NOW()),
    (4, 4, 35000, 40000, NOW() - INTERVAL '25 days', NOW() + INTERVAL '5 days', NULL, NOW() - INTERVAL '30 days', NOW()),
    (5, 5, 8000, 12000, NOW() - INTERVAL '10 days', NOW() + INTERVAL '20 days', NOW() + INTERVAL '14 days', NOW() - INTERVAL '20 days', NOW()),
    (6, 6, 20000, 10000, NOW() - INTERVAL '5 days', NOW() + INTERVAL '25 days', NOW() + INTERVAL '21 days', NOW() - INTERVAL '15 days', NOW()),
    (7, 7, 75000, 95000, NOW() - INTERVAL '35 days', NOW() + INTERVAL '25 days', NULL, NOW() - INTERVAL '40 days', NOW()),
    (8, 8, 42000, 58000, NOW() - INTERVAL '20 days', NOW() + INTERVAL '10 days', NULL, NOW() - INTERVAL '25 days', NOW()),
    (9, 9, 5000, 15000, NOW() - INTERVAL '30 days', NOW() + INTERVAL '5 days', NOW() + INTERVAL '3 days', NOW() - INTERVAL '35 days', NOW()),
    (10, 10, 60000, 70000, NOW() - INTERVAL '45 days', NOW() + INTERVAL '15 days', NULL, NOW() - INTERVAL '50 days', NOW());

SELECT setval('wallets_id_seq', (SELECT MAX(id) FROM wallets));

-- =====================================================
-- 5. WALLET_TRANSACTIONS - Lịch sử giao dịch (JPY)
-- =====================================================
INSERT INTO wallet_transactions (id, wallet_id, transaction_type, amount, balance_before, balance_after, description, reference_id, deleted, created_at, updated_at) VALUES
    -- Company 1 Transactions
    (1, 1, 'DEPOSIT', 50000, 0, 50000, '初回入金', 1, false, NOW() - INTERVAL '55 days', NOW()),
    (2, 1, 'BILLING', -10000, 50000, 40000, 'スタンダードプラン10月分', NULL, false, NOW() - INTERVAL '50 days', NOW()),
    (3, 1, 'DEPOSIT', 40000, 40000, 80000, '追加入金', 2, false, NOW() - INTERVAL '40 days', NOW()),
    (4, 1, 'BILLING', -10000, 80000, 70000, 'スタンダードプラン11月分', NULL, false, NOW() - INTERVAL '30 days', NOW()),
    (5, 1, 'BILLING', -10000, 70000, 60000, 'スタンダードプラン12月分', NULL, false, NOW() - INTERVAL '20 days', NOW()),
    (6, 1, 'DEPOSIT', 30000, 60000, 90000, '追加入金', 3, false, NOW() - INTERVAL '10 days', NOW()),
    (7, 1, 'BILLING', -10000, 90000, 80000, 'スタンダードプラン1月分', NULL, false, NOW() - INTERVAL '5 days', NOW()),
    (8, 1, 'REFUND', 5000, 80000, 85000, '返金処理', NULL, false, NOW() - INTERVAL '3 days', NOW()),
    (9, 1, 'BILLING', -10000, 85000, 75000, 'スタンダードプラン追加料金', NULL, false, NOW() - INTERVAL '2 days', NOW()),
    (10, 1, 'COMMISSION', -5000, 75000, 70000, '紹介手数料', NULL, false, NOW() - INTERVAL '1 day', NOW()),
    (11, 1, 'BILLING', -20000, 70000, 50000, 'スタンダードプラン2月分', NULL, false, NOW(), NOW()),
    -- Company 2 Transactions
    (12, 2, 'DEPOSIT', 30000, 0, 30000, '初回入金', 4, false, NOW() - INTERVAL '40 days', NOW()),
    (13, 2, 'BILLING', -5000, 30000, 25000, 'ベーシックプラン11月分', NULL, false, NOW() - INTERVAL '35 days', NOW()),
    (14, 2, 'BILLING', -5000, 25000, 20000, 'ベーシックプラン12月分', NULL, false, NOW() - INTERVAL '25 days', NOW()),
    (15, 2, 'BILLING', -5000, 20000, 15000, 'ベーシックプラン1月分', NULL, false, NOW() - INTERVAL '15 days', NOW()),
    (16, 2, 'BILLING', -5000, 15000, 10000, 'ベーシックプラン追加料金', NULL, false, NOW() - INTERVAL '10 days', NOW()),
    (17, 2, 'DEPOSIT', 10000, 10000, 20000, '追加入金', 5, false, NOW() - INTERVAL '5 days', NOW()),
    (18, 2, 'BILLING', -5000, 20000, 15000, 'ベーシックプラン2月分', NULL, false, NOW(), NOW()),
    -- Company 3 Transactions
    (19, 3, 'DEPOSIT', 100000, 0, 100000, '初回入金', 6, false, NOW() - INTERVAL '85 days', NOW()),
    (20, 3, 'BILLING', -20000, 100000, 80000, 'エンタープライズプラン10月分', NULL, false, NOW() - INTERVAL '80 days', NOW()),
    (21, 3, 'BILLING', -20000, 80000, 60000, 'エンタープライズプラン11月分', NULL, false, NOW() - INTERVAL '60 days', NOW()),
    (22, 3, 'DEPOSIT', 80000, 60000, 140000, '追加入金', 7, false, NOW() - INTERVAL '50 days', NOW()),
    (23, 3, 'BILLING', -20000, 140000, 120000, 'エンタープライズプラン12月分', NULL, false, NOW() - INTERVAL '40 days', NOW()),
    (24, 3, 'BILLING', -20000, 120000, 100000, 'エンタープライズプラン1月分', NULL, false, NOW() - INTERVAL '20 days', NOW()),
    (25, 3, 'REFUND', 20000, 100000, 120000, '返金処理', NULL, false, NOW() - INTERVAL '10 days', NOW()),
    (26, 3, 'BILLING', -20000, 120000, 100000, 'エンタープライズプラン2月分', NULL, false, NOW(), NOW()),
    -- Company 4 Transactions
    (27, 4, 'DEPOSIT', 50000, 0, 50000, '初回入金', 8, false, NOW() - INTERVAL '28 days', NOW()),
    (28, 4, 'BILLING', -10000, 50000, 40000, 'スタンダードプラン12月分', NULL, false, NOW() - INTERVAL '25 days', NOW()),
    (29, 4, 'BILLING', -10000, 40000, 30000, 'スタンダードプラン1月分', NULL, false, NOW() - INTERVAL '15 days', NOW()),
    (30, 4, 'DEPOSIT', 25000, 30000, 55000, '追加入金', 9, false, NOW() - INTERVAL '10 days', NOW()),
    (31, 4, 'BILLING', -10000, 55000, 45000, 'スタンダードプラン追加料金', NULL, false, NOW() - INTERVAL '5 days', NOW()),
    (32, 4, 'BILLING', -10000, 45000, 35000, 'スタンダードプラン2月分', NULL, false, NOW(), NOW()),
    -- Company 5 Transactions
    (33, 5, 'DEPOSIT', 20000, 0, 20000, '初回入金', 10, false, NOW() - INTERVAL '18 days', NOW()),
    (34, 5, 'BILLING', -5000, 20000, 15000, 'ベーシックプラン1月分', NULL, false, NOW() - INTERVAL '10 days', NOW()),
    (35, 5, 'BILLING', -2000, 15000, 13000, 'ベーシックプラン追加料金', NULL, false, NOW() - INTERVAL '5 days', NOW()),
    (36, 5, 'BILLING', -5000, 13000, 8000, 'ベーシックプラン2月分', NULL, false, NOW(), NOW()),
    -- Company 6 Transactions
    (37, 6, 'DEPOSIT', 30000, 0, 30000, '初回入金', 11, false, NOW() - INTERVAL '12 days', NOW()),
    (38, 6, 'BILLING', -10000, 30000, 20000, 'スタンダードプラン1月分', NULL, false, NOW() - INTERVAL '5 days', NOW()),
    -- Company 7 Transactions
    (39, 7, 'DEPOSIT', 80000, 0, 80000, '初回入金', 12, false, NOW() - INTERVAL '38 days', NOW()),
    (40, 7, 'BILLING', -20000, 80000, 60000, 'エンタープライズプラン11月分', NULL, false, NOW() - INTERVAL '35 days', NOW()),
    (41, 7, 'DEPOSIT', 50000, 60000, 110000, '追加入金', 13, false, NOW() - INTERVAL '25 days', NOW()),
    (42, 7, 'BILLING', -20000, 110000, 90000, 'エンタープライズプラン12月分', NULL, false, NOW() - INTERVAL '20 days', NOW()),
    (43, 7, 'BILLING', -20000, 90000, 70000, 'エンタープライズプラン1月分', NULL, false, NOW() - INTERVAL '10 days', NOW()),
    (44, 7, 'REFUND', 25000, 70000, 95000, '返金処理', NULL, false, NOW() - INTERVAL '5 days', NOW()),
    (45, 7, 'BILLING', -20000, 95000, 75000, 'エンタープライズプラン2月分', NULL, false, NOW(), NOW()),
    -- Company 8 Transactions
    (46, 8, 'DEPOSIT', 60000, 0, 60000, '初回入金', 14, false, NOW() - INTERVAL '23 days', NOW()),
    (47, 8, 'BILLING', -10000, 60000, 50000, 'スタンダードプラン12月分', NULL, false, NOW() - INTERVAL '20 days', NOW()),
    (48, 8, 'BILLING', -10000, 50000, 40000, 'スタンダードプラン1月分', NULL, false, NOW() - INTERVAL '15 days', NOW()),
    (49, 8, 'DEPOSIT', 30000, 40000, 70000, '追加入金', 15, false, NOW() - INTERVAL '10 days', NOW()),
    (50, 8, 'BILLING', -10000, 70000, 60000, 'スタンダードプラン追加料金', NULL, false, NOW() - INTERVAL '5 days', NOW()),
    (51, 8, 'COMMISSION', -8000, 60000, 52000, '紹介手数料', NULL, false, NOW() - INTERVAL '3 days', NOW()),
    (52, 8, 'BILLING', -10000, 52000, 42000, 'スタンダードプラン2月分', NULL, false, NOW(), NOW()),
    -- Company 9 Transactions
    (53, 9, 'DEPOSIT', 20000, 0, 20000, '初回入金', 16, false, NOW() - INTERVAL '33 days', NOW()),
    (54, 9, 'BILLING', -5000, 20000, 15000, 'ベーシックプラン12月分', NULL, false, NOW() - INTERVAL '30 days', NOW()),
    (55, 9, 'BILLING', -5000, 15000, 10000, 'ベーシックプラン1月分', NULL, false, NOW() - INTERVAL '20 days', NOW()),
    (56, 9, 'BILLING', -5000, 10000, 5000, 'ベーシックプラン2月分', NULL, false, NOW(), NOW()),
    -- Company 10 Transactions
    (57, 10, 'DEPOSIT', 70000, 0, 70000, '初回入金', 17, false, NOW() - INTERVAL '48 days', NOW()),
    (58, 10, 'BILLING', -10000, 70000, 60000, 'スタンダードプラン11月分', NULL, false, NOW() - INTERVAL '45 days', NOW()),
    (59, 10, 'BILLING', -10000, 60000, 50000, 'スタンダードプラン12月分', NULL, false, NOW() - INTERVAL '35 days', NOW()),
    (60, 10, 'DEPOSIT', 40000, 50000, 90000, '追加入金', 18, false, NOW() - INTERVAL '25 days', NOW()),
    (61, 10, 'BILLING', -10000, 90000, 80000, 'スタンダードプラン1月分', NULL, false, NOW() - INTERVAL '20 days', NOW()),
    (62, 10, 'BILLING', -10000, 80000, 70000, 'スタンダードプラン追加料金', NULL, false, NOW() - INTERVAL '10 days', NOW()),
    (63, 10, 'BILLING', -10000, 70000, 60000, 'スタンダードプラン2月分', NULL, false, NOW(), NOW());

SELECT setval('wallet_transactions_id_seq', (SELECT MAX(id) FROM wallet_transactions));



-- =====================================================
-- 6. DEPOSIT_REQUESTS - Yêu cầu nạp tiền (JPY)
-- requested_by: employee_code của người yêu cầu
-- =====================================================
INSERT INTO deposit_requests (id, company_id, amount, transfer_proof_url, status, requested_by, approved_by, rejection_reason, processed_at, deleted, created_at, updated_at) VALUES
    -- Company 1 Deposits (requested by 25012503 - Admin Company 1)
    (1, 1, 50000, '/uploads/deposits/proof1.jpg', 'APPROVED', '25012503', '25001501', NULL, NOW() - INTERVAL '54 days', false, NOW() - INTERVAL '55 days', NOW()),
    (2, 1, 40000, '/uploads/deposits/proof2.jpg', 'APPROVED', '25012503', '25002005', NULL, NOW() - INTERVAL '39 days', false, NOW() - INTERVAL '40 days', NOW()),
    (3, 1, 30000, '/uploads/deposits/proof3.jpg', 'APPROVED', '25012503', '25001501', NULL, NOW() - INTERVAL '9 days', false, NOW() - INTERVAL '10 days', NOW()),
    -- Company 2 Deposits (requested by 25021804 - Admin Company 2)
    (4, 2, 30000, '/uploads/deposits/proof4.jpg', 'APPROVED', '25021804', '25001501', NULL, NOW() - INTERVAL '39 days', false, NOW() - INTERVAL '40 days', NOW()),
    (5, 2, 10000, '/uploads/deposits/proof5.jpg', 'APPROVED', '25021804', '25002005', NULL, NOW() - INTERVAL '4 days', false, NOW() - INTERVAL '5 days', NOW()),
    -- Company 3 Deposits (requested by 25030112 - Admin Company 3)
    (6, 3, 100000, '/uploads/deposits/proof6.jpg', 'APPROVED', '25030112', '25001501', NULL, NOW() - INTERVAL '84 days', false, NOW() - INTERVAL '85 days', NOW()),
    (7, 3, 80000, '/uploads/deposits/proof7.jpg', 'APPROVED', '25030112', '25002005', NULL, NOW() - INTERVAL '49 days', false, NOW() - INTERVAL '50 days', NOW()),
    -- Company 4 Deposits (requested by 25040501 - Admin Company 4)
    (8, 4, 50000, '/uploads/deposits/proof8.jpg', 'APPROVED', '25040501', '25001501', NULL, NOW() - INTERVAL '27 days', false, NOW() - INTERVAL '28 days', NOW()),
    (9, 4, 25000, '/uploads/deposits/proof9.jpg', 'APPROVED', '25040501', '25002005', NULL, NOW() - INTERVAL '9 days', false, NOW() - INTERVAL '10 days', NOW()),
    -- Company 5 Deposits (requested by 25051003 - Admin Company 5)
    (10, 5, 20000, '/uploads/deposits/proof10.jpg', 'APPROVED', '25051003', '25001501', NULL, NOW() - INTERVAL '17 days', false, NOW() - INTERVAL '18 days', NOW()),
    -- Company 6 Deposits (requested by 25060806 - Admin Company 6)
    (11, 6, 30000, '/uploads/deposits/proof11.jpg', 'APPROVED', '25060806', '25002005', NULL, NOW() - INTERVAL '11 days', false, NOW() - INTERVAL '12 days', NOW()),
    -- PENDING Deposits
    (12, 1, 25000, '/uploads/deposits/proof12.jpg', 'PENDING', '25012503', NULL, NULL, NULL, false, NOW() - INTERVAL '2 days', NOW()),
    (13, 2, 15000, '/uploads/deposits/proof13.jpg', 'PENDING', '25021804', NULL, NULL, NULL, false, NOW() - INTERVAL '1 day', NOW()),
    (14, 3, 50000, '/uploads/deposits/proof14.jpg', 'PENDING', '25030112', NULL, NULL, NULL, false, NOW() - INTERVAL '3 hours', NOW()),
    (15, 4, 20000, '/uploads/deposits/proof15.jpg', 'PENDING', '25040501', NULL, NULL, NULL, false, NOW() - INTERVAL '6 hours', NOW()),
    (16, 5, 30000, '/uploads/deposits/proof16.jpg', 'PENDING', '25051003', NULL, NULL, NULL, false, NOW() - INTERVAL '12 hours', NOW()),
    (17, 6, 10000, '/uploads/deposits/proof17.jpg', 'PENDING', '25060806', NULL, NULL, NULL, false, NOW() - INTERVAL '4 hours', NOW()),
    -- REJECTED Deposits
    (18, 2, 5000, '/uploads/deposits/proof18.jpg', 'REJECTED', '25021804', '25001501', '振込証明書が不鮮明です', NOW() - INTERVAL '5 days', false, NOW() - INTERVAL '7 days', NOW()),
    (19, 3, 10000, '/uploads/deposits/proof19.jpg', 'REJECTED', '25030112', '25002005', '金額が一致しません', NOW() - INTERVAL '3 days', false, NOW() - INTERVAL '4 days', NOW()),
    (20, 4, 8000, '/uploads/deposits/proof20.jpg', 'REJECTED', '25040501', '25001501', '振込先口座が異なります', NOW() - INTERVAL '2 days', false, NOW() - INTERVAL '3 days', NOW()),
    -- Company 7 Deposits (requested by 25070203 - Admin Company 7)
    (21, 7, 80000, '/uploads/deposits/proof21.jpg', 'APPROVED', '25070203', '25001501', NULL, NOW() - INTERVAL '37 days', false, NOW() - INTERVAL '38 days', NOW()),
    (22, 7, 50000, '/uploads/deposits/proof22.jpg', 'APPROVED', '25070203', '25002005', NULL, NOW() - INTERVAL '24 days', false, NOW() - INTERVAL '25 days', NOW()),
    -- Company 8 Deposits (requested by 25080711 - Admin Company 8)
    (23, 8, 60000, '/uploads/deposits/proof23.jpg', 'APPROVED', '25080711', '25001501', NULL, NOW() - INTERVAL '22 days', false, NOW() - INTERVAL '23 days', NOW()),
    (24, 8, 30000, '/uploads/deposits/proof24.jpg', 'APPROVED', '25080711', '25002005', NULL, NOW() - INTERVAL '9 days', false, NOW() - INTERVAL '10 days', NOW()),
    -- Company 9 Deposits (requested by 25091405 - Admin Company 9)
    (25, 9, 20000, '/uploads/deposits/proof25.jpg', 'APPROVED', '25091405', '25001501', NULL, NOW() - INTERVAL '32 days', false, NOW() - INTERVAL '33 days', NOW()),
    -- Company 10 Deposits (requested by 25100912 - Admin Company 10)
    (26, 10, 70000, '/uploads/deposits/proof26.jpg', 'APPROVED', '25100912', '25001501', NULL, NOW() - INTERVAL '47 days', false, NOW() - INTERVAL '48 days', NOW()),
    (27, 10, 40000, '/uploads/deposits/proof27.jpg', 'APPROVED', '25100912', '25002005', NULL, NOW() - INTERVAL '24 days', false, NOW() - INTERVAL '25 days', NOW()),
    -- More PENDING Deposits
    (28, 7, 35000, '/uploads/deposits/proof28.jpg', 'PENDING', '25070203', NULL, NULL, NULL, false, NOW() - INTERVAL '8 hours', NOW()),
    (29, 8, 25000, '/uploads/deposits/proof29.jpg', 'PENDING', '25080711', NULL, NULL, NULL, false, NOW() - INTERVAL '5 hours', NOW()),
    (30, 9, 40000, '/uploads/deposits/proof30.jpg', 'PENDING', '25091405', NULL, NULL, NULL, false, NOW() - INTERVAL '10 hours', NOW()),
    (31, 10, 15000, '/uploads/deposits/proof31.jpg', 'PENDING', '25100912', NULL, NULL, NULL, false, NOW() - INTERVAL '2 hours', NOW()),
    -- More REJECTED Deposits
    (32, 7, 12000, '/uploads/deposits/proof32.jpg', 'REJECTED', '25070203', '25001501', '振込日が確認できません', NOW() - INTERVAL '6 days', false, NOW() - INTERVAL '8 days', NOW()),
    (33, 9, 7000, '/uploads/deposits/proof33.jpg', 'REJECTED', '25091405', '25002005', '振込名義が異なります', NOW() - INTERVAL '4 days', false, NOW() - INTERVAL '5 days', NOW());

SELECT setval('deposit_requests_id_seq', (SELECT MAX(id) FROM deposit_requests));

-- =====================================================
-- 7. EMPLOYEE_COMMISSIONS - Hoa hồng nhân viên (JPY)
-- employee_code: Mã nhân viên Tamabee nhận hoa hồng
-- status: PENDING (chưa đủ điều kiện), ELIGIBLE (đủ điều kiện), PAID (đã thanh toán)
-- company_billing_at_creation: Tổng billing của company tại thời điểm tạo commission
-- Điều kiện đủ: company đã billing >= 50,000 JPY
-- =====================================================
INSERT INTO employee_commissions (id, employee_code, company_id, amount, status, company_billing_at_creation, paid_at, paid_by, deleted, created_at, updated_at) VALUES
    -- Admin Tamabee (25001501) - Referred Company 1 (total_billing = 80,000 -> ELIGIBLE/PAID)
    (1, '25001501', 1, 5000, 'PAID', 0, NOW() - INTERVAL '50 days', '25001501', false, NOW() - INTERVAL '55 days', NOW()),
    (2, '25001501', 1, 5000, 'PAID', 20000, NOW() - INTERVAL '30 days', '25001501', false, NOW() - INTERVAL '35 days', NOW()),
    (3, '25001501', 1, 5000, 'PAID', 40000, NOW() - INTERVAL '15 days', '25002005', false, NOW() - INTERVAL '20 days', NOW()),
    (4, '25001501', 1, 5000, 'ELIGIBLE', 60000, NULL, NULL, false, NOW() - INTERVAL '5 days', NOW()),
    (5, '25001501', 1, 5000, 'ELIGIBLE', 80000, NULL, NULL, false, NOW() - INTERVAL '1 day', NOW()),
    
    -- Manager Tamabee (25002005) - Referred Company 2 (total_billing = 25,000 -> PENDING)
    (6, '25002005', 2, 5000, 'PAID', 0, NOW() - INTERVAL '35 days', '25001501', false, NOW() - INTERVAL '40 days', NOW()),
    (7, '25002005', 2, 5000, 'PENDING', 10000, NULL, NULL, false, NOW() - INTERVAL '25 days', NOW()),
    (8, '25002005', 2, 5000, 'PENDING', 20000, NULL, NULL, false, NOW() - INTERVAL '10 days', NOW()),
    (9, '25002005', 2, 5000, 'PENDING', 25000, NULL, NULL, false, NOW() - INTERVAL '2 days', NOW()),
    
    -- Employee Tamabee (25001008) - Referred Company 4 (total_billing = 40,000 -> PENDING)
    (10, '25001008', 4, 5000, 'PAID', 0, NOW() - INTERVAL '25 days', '25001501', false, NOW() - INTERVAL '28 days', NOW()),
    (11, '25001008', 4, 5000, 'PENDING', 10000, NULL, NULL, false, NOW() - INTERVAL '15 days', NOW()),
    (12, '25001008', 4, 5000, 'PENDING', 30000, NULL, NULL, false, NOW() - INTERVAL '5 days', NOW()),
    (13, '25001008', 4, 5000, 'PENDING', 40000, NULL, NULL, false, NOW() - INTERVAL '1 day', NOW()),
    
    -- Employee Tamabee (25001008) - Referred Company 5 (total_billing = 12,000 -> PENDING)
    (14, '25001008', 5, 5000, 'PENDING', 0, NULL, NULL, false, NOW() - INTERVAL '18 days', NOW()),
    (15, '25001008', 5, 5000, 'PENDING', 7000, NULL, NULL, false, NOW() - INTERVAL '5 days', NOW()),
    
    -- Additional Employee Tamabee (25001203) - Referred Company 6 (total_billing = 10,000 -> PENDING)
    (16, '25001203', 6, 5000, 'PENDING', 0, NULL, NULL, false, NOW() - INTERVAL '12 days', NOW()),
    (17, '25001203', 6, 5000, 'PENDING', 10000, NULL, NULL, false, NOW() - INTERVAL '2 days', NOW()),
    
    -- Employee Tamabee 2 (25002507) - Referred Company 7 (total_billing = 95,000 -> ELIGIBLE)
    (18, '25002507', 7, 5000, 'PAID', 0, NOW() - INTERVAL '35 days', '25001501', false, NOW() - INTERVAL '38 days', NOW()),
    (19, '25002507', 7, 5000, 'PAID', 20000, NOW() - INTERVAL '20 days', '25001501', false, NOW() - INTERVAL '25 days', NOW()),
    (20, '25002507', 7, 5000, 'PAID', 40000, NOW() - INTERVAL '10 days', '25002005', false, NOW() - INTERVAL '15 days', NOW()),
    (21, '25002507', 7, 5000, 'ELIGIBLE', 75000, NULL, NULL, false, NOW() - INTERVAL '5 days', NOW()),
    (22, '25002507', 7, 5000, 'ELIGIBLE', 95000, NULL, NULL, false, NOW() - INTERVAL '1 day', NOW()),
    
    -- Employee Tamabee 3 (25003006) - Referred Company 8 (total_billing = 58,000 -> ELIGIBLE)
    (23, '25003006', 8, 5000, 'PAID', 0, NOW() - INTERVAL '20 days', '25001501', false, NOW() - INTERVAL '23 days', NOW()),
    (24, '25003006', 8, 5000, 'PAID', 20000, NOW() - INTERVAL '10 days', '25002005', false, NOW() - INTERVAL '15 days', NOW()),
    (25, '25003006', 8, 5000, 'ELIGIBLE', 48000, NULL, NULL, false, NOW() - INTERVAL '5 days', NOW()),
    (26, '25003006', 8, 5000, 'ELIGIBLE', 58000, NULL, NULL, false, NOW() - INTERVAL '1 day', NOW()),
    
    -- Employee Tamabee 4 (25001511) - Referred Company 9 (total_billing = 15,000 -> PENDING)
    (27, '25001511', 9, 5000, 'PENDING', 0, NULL, NULL, false, NOW() - INTERVAL '33 days', NOW()),
    (28, '25001511', 9, 5000, 'PENDING', 5000, NULL, NULL, false, NOW() - INTERVAL '20 days', NOW()),
    (29, '25001511', 9, 5000, 'PENDING', 15000, NULL, NULL, false, NOW() - INTERVAL '5 days', NOW());

SELECT setval('employee_commissions_id_seq', (SELECT MAX(id) FROM employee_commissions));
