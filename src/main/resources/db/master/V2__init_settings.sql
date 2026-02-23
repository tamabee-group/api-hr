-- =====================================================
-- TAMABEE HR - MASTER DATABASE DEFAULT SETTINGS
-- Currency: JPY (Japanese Yen)
-- =====================================================

-- System settings
INSERT INTO tamabee_settings (setting_key, setting_value, description, value_type, deleted, created_at, updated_at) VALUES
    ('FREE_TRIAL_MONTHS', '2', 'Số tháng miễn phí cho company mới đăng ký', 'INTEGER', false, NOW(), NOW()),
    ('REFERRAL_BONUS_MONTHS', '1', 'Số tháng miễn phí thêm khi có mã giới thiệu', 'INTEGER', false, NOW(), NOW()),
    ('COMMISSION_AMOUNT', '5000', 'Số tiền hoa hồng giới thiệu cố định (JPY)', 'INTEGER', false, NOW(), NOW()),
    ('MIN_DEPOSIT_AMOUNT', '5000', 'Số tiền nạp tối thiểu (JPY)', 'INTEGER', false, NOW(), NOW()),
    ('CUSTOM_PRICE_PER_EMPLOYEE', '400', 'Giá mỗi nhân viên cho gói Custom (JPY)', 'INTEGER', false, NOW(), NOW()),
    ('BANK_NAME', 'MUFG', 'Tên ngân hàng nhận tiền', 'STRING', false, NOW(), NOW()),
    ('BANK_ACCOUNT', '1234567', 'Số tài khoản nhận tiền', 'STRING', false, NOW(), NOW()),
    ('BANK_ACCOUNT_NAME', 'タマビー株式会社', 'Tên chủ tài khoản', 'STRING', false, NOW(), NOW());

-- =====================================================
-- PLANS — Tất cả gói đều đầy đủ tính năng, chỉ khác số người và giá
-- =====================================================
INSERT INTO plans (id, name_vi, name_en, name_ja, description_vi, description_en, description_ja, monthly_price, max_employees, is_active, deleted, created_at, updated_at) VALUES
    (0, 'Gói Miễn phí', 'Free Plan', 'フリープラン',
     'Trải nghiệm đầy đủ tính năng trong 2 tháng, không cần thẻ tín dụng',
     'Experience all features for 2 months, no credit card required',
     '2ヶ月間全機能を無料体験、クレジットカード不要',
     0, 200, true, false, NOW(), NOW()),

    (1, 'Gói Khởi nghiệp', 'Starter Plan', 'スタータープラン',
     'Đầy đủ tính năng cho đội nhóm nhỏ và startup',
     'Full features for small teams and startups',
     'スタートアップや小規模チーム向け全機能プラン',
     3000, 15, true, false, NOW(), NOW()),

    (2, 'Gói Doanh nghiệp', 'Business Plan', 'ビジネスプラン',
     'Đầy đủ tính năng cho doanh nghiệp đang phát triển',
     'Full features for growing businesses',
     '成長中の企業向け全機能プラン',
     8000, 50, true, false, NOW(), NOW()),

    (3, 'Gói Chuyên nghiệp', 'Enterprise Plan', 'エンタープライズプラン',
     'Đầy đủ tính năng cho doanh nghiệp quy mô lớn',
     'Full features for large organizations',
     '大規模企業向け全機能プラン',
     18000, 150, true, false, NOW(), NOW()),

    (4, 'Gói Tùy chỉnh', 'Custom Plan', 'カスタムプラン',
     'Không giới hạn nhân viên, chỉ trả theo số người thực tế',
     'Unlimited employees, pay only for active headcount',
     '従業員数無制限、実際の人数分のみお支払い',
     0, 0, true, false, NOW(), NOW());

-- =====================================================
-- PLAN FEATURES — Mỗi gói đều có cùng tính năng
-- =====================================================
INSERT INTO plan_features (plan_id, feature_vi, feature_en, feature_ja, sort_order, is_highlighted, deleted, created_at, updated_at) VALUES
    -- ===== Free Plan =====
    (0, 'Đầy đủ tính năng trong 2 tháng', 'All features for 2 months', '2ヶ月間全機能利用可能', 1, true, false, NOW(), NOW()),
    (0, 'Chấm công, ca làm, nghỉ phép', 'Attendance, shifts, leaves', '勤怠・シフト・休暇管理', 2, false, false, NOW(), NOW()),
    (0, 'Tính lương & phiếu lương tự động', 'Auto payroll & payslips', '自動給与計算・給与明細', 3, false, false, NOW(), NOW()),
    (0, 'Hợp đồng, báo cáo, xuất PDF/CSV', 'Contracts, reports, PDF/CSV export', '契約・レポート・PDF/CSV出力', 4, false, false, NOW(), NOW()),
    (0, 'Đa ngôn ngữ: Việt, Nhật, Anh', 'Multi-language: VI, JA, EN', '多言語対応：越・日・英', 5, false, false, NOW(), NOW()),

    -- ===== Starter Plan =====
    (1, 'Đầy đủ tính năng, không giới hạn', 'All features included', '全機能利用可能', 1, true, false, NOW(), NOW()),
    (1, 'Chấm công, ca làm, nghỉ phép', 'Attendance, shifts, leaves', '勤怠・シフト・休暇管理', 2, false, false, NOW(), NOW()),
    (1, 'Tính lương & phiếu lương tự động', 'Auto payroll & payslips', '自動給与計算・給与明細', 3, false, false, NOW(), NOW()),
    (1, 'Hợp đồng, báo cáo, xuất PDF/CSV', 'Contracts, reports, PDF/CSV export', '契約・レポート・PDF/CSV出力', 4, false, false, NOW(), NOW()),
    (1, 'Đa ngôn ngữ: Việt, Nhật, Anh', 'Multi-language: VI, JA, EN', '多言語対応：越・日・英', 5, false, false, NOW(), NOW()),

    -- ===== Business Plan =====
    (2, 'Đầy đủ tính năng, không giới hạn', 'All features included', '全機能利用可能', 1, true, false, NOW(), NOW()),
    (2, 'Chấm công, ca làm, nghỉ phép', 'Attendance, shifts, leaves', '勤怠・シフト・休暇管理', 2, false, false, NOW(), NOW()),
    (2, 'Tính lương & phiếu lương tự động', 'Auto payroll & payslips', '自動給与計算・給与明細', 3, false, false, NOW(), NOW()),
    (2, 'Hợp đồng, báo cáo, xuất PDF/CSV', 'Contracts, reports, PDF/CSV export', '契約・レポート・PDF/CSV出力', 4, false, false, NOW(), NOW()),
    (2, 'Đa ngôn ngữ: Việt, Nhật, Anh', 'Multi-language: VI, JA, EN', '多言語対応：越・日・英', 5, false, false, NOW(), NOW()),

    -- ===== Enterprise Plan =====
    (3, 'Đầy đủ tính năng, không giới hạn', 'All features included', '全機能利用可能', 1, true, false, NOW(), NOW()),
    (3, 'Chấm công, ca làm, nghỉ phép', 'Attendance, shifts, leaves', '勤怠・シフト・休暇管理', 2, false, false, NOW(), NOW()),
    (3, 'Tính lương & phiếu lương tự động', 'Auto payroll & payslips', '自動給与計算・給与明細', 3, false, false, NOW(), NOW()),
    (3, 'Hợp đồng, báo cáo, xuất PDF/CSV', 'Contracts, reports, PDF/CSV export', '契約・レポート・PDF/CSV出力', 4, false, false, NOW(), NOW()),
    (3, 'Đa ngôn ngữ: Việt, Nhật, Anh', 'Multi-language: VI, JA, EN', '多言語対応：越・日・英', 5, false, false, NOW(), NOW()),

    -- ===== Custom Plan =====
    (4, 'Không giới hạn nhân viên', 'Unlimited employees', '従業員数無制限', 1, true, false, NOW(), NOW()),
    (4, 'Chỉ ¥400/nhân viên/tháng', 'Only ¥400/employee/month', '¥400/人/月のみ', 2, true, false, NOW(), NOW()),
    (4, 'Đầy đủ tính năng, không giới hạn', 'All features included', '全機能利用可能', 3, false, false, NOW(), NOW()),
    (4, 'Tính lương, chấm công, nghỉ phép', 'Payroll, attendance, leaves', '給与計算・勤怠・休暇', 4, false, false, NOW(), NOW()),
    (4, 'Đa ngôn ngữ: Việt, Nhật, Anh', 'Multi-language: VI, JA, EN', '多言語対応：越・日・英', 5, false, false, NOW(), NOW());

-- Reset sequences
SELECT setval('plans_id_seq', (SELECT MAX(id) FROM plans));

-- Tạo company Tamabee với id=0 (nếu chưa tồn tại)
INSERT INTO companies (id, name, owner_name, email, phone, address, industry, zipcode, region, language, tenant_domain, plan_id, status, deleted, created_at, updated_at)
SELECT 0, 'Tamabee株式会社', 'Tamabee Admin', 'contact@tamabee.vn', '03-1234-5678', '東京都渋谷区', 'IT Services', '150-0001', 'ja', 'ja', 'tamabee', 0, 'ACTIVE', false, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM companies WHERE tenant_domain = 'tamabee');

-- Tạo wallet cho Tamabee (nếu chưa tồn tại)
INSERT INTO wallets (company_id, balance, total_billing, last_billing_date, next_billing_date, deleted, created_at, updated_at)
SELECT 0, 0, 0, NOW(), NOW() + INTERVAL '1 month', false, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM wallets WHERE company_id = 0);

-- Fix Tamabee company (nếu đã tồn tại nhưng thiếu thông tin)
UPDATE companies 
SET plan_id = 0, 
    name = 'Tamabee株式会社', 
    email = 'contact@tamabee.vn',
    status = 'ACTIVE',
    region = 'ja',
    language = 'ja',
    owner_name = 'Tamabee Admin'
WHERE tenant_domain = 'tamabee' AND (plan_id IS NULL OR plan_id != 0);

-- =====================================================
-- UPDATE EXISTING DATA (cho DB đã chạy V2 trước đó)
-- =====================================================
UPDATE plans SET
    name_vi = 'Gói Miễn phí', name_en = 'Free Plan', name_ja = 'フリープラン',
    description_vi = 'Trải nghiệm đầy đủ tính năng trong 2 tháng, không cần thẻ tín dụng',
    description_en = 'Experience all features for 2 months, no credit card required',
    description_ja = '2ヶ月間全機能を無料体験、クレジットカード不要',
    max_employees = 200, monthly_price = 0
WHERE id = 0;

UPDATE plans SET
    name_vi = 'Gói Khởi nghiệp', name_en = 'Starter Plan', name_ja = 'スタータープラン',
    description_vi = 'Đầy đủ tính năng cho đội nhóm nhỏ và startup',
    description_en = 'Full features for small teams and startups',
    description_ja = 'スタートアップや小規模チーム向け全機能プラン',
    max_employees = 15, monthly_price = 3000
WHERE id = 1;

UPDATE plans SET
    name_vi = 'Gói Doanh nghiệp', name_en = 'Business Plan', name_ja = 'ビジネスプラン',
    description_vi = 'Đầy đủ tính năng cho doanh nghiệp đang phát triển',
    description_en = 'Full features for growing businesses',
    description_ja = '成長中の企業向け全機能プラン',
    max_employees = 50, monthly_price = 8000
WHERE id = 2;

UPDATE plans SET
    name_vi = 'Gói Chuyên nghiệp', name_en = 'Enterprise Plan', name_ja = 'エンタープライズプラン',
    description_vi = 'Đầy đủ tính năng cho doanh nghiệp quy mô lớn',
    description_en = 'Full features for large organizations',
    description_ja = '大規模企業向け全機能プラン',
    max_employees = 150, monthly_price = 18000
WHERE id = 3;

UPDATE plans SET
    name_vi = 'Gói Tùy chỉnh', name_en = 'Custom Plan', name_ja = 'カスタムプラン',
    description_vi = 'Không giới hạn nhân viên, chỉ trả theo số người thực tế',
    description_en = 'Unlimited employees, pay only for active headcount',
    description_ja = '従業員数無制限、実際の人数分のみお支払い',
    max_employees = 0, monthly_price = 0
WHERE id = 4;

-- Xóa features cũ và thêm mới
DELETE FROM plan_features WHERE plan_id IN (0, 1, 2, 3, 4);

INSERT INTO plan_features (plan_id, feature_vi, feature_en, feature_ja, sort_order, is_highlighted, deleted, created_at, updated_at) VALUES
    -- Free Plan
    (0, 'Đầy đủ tính năng trong 2 tháng', 'All features for 2 months', '2ヶ月間全機能利用可能', 1, true, false, NOW(), NOW()),
    (0, 'Chấm công, ca làm, nghỉ phép', 'Attendance, shifts, leaves', '勤怠・シフト・休暇管理', 2, false, false, NOW(), NOW()),
    (0, 'Tính lương & phiếu lương tự động', 'Auto payroll & payslips', '自動給与計算・給与明細', 3, false, false, NOW(), NOW()),
    (0, 'Hợp đồng, báo cáo, xuất PDF/CSV', 'Contracts, reports, PDF/CSV export', '契約・レポート・PDF/CSV出力', 4, false, false, NOW(), NOW()),
    (0, 'Đa ngôn ngữ: Việt, Nhật, Anh', 'Multi-language: VI, JA, EN', '多言語対応：越・日・英', 5, false, false, NOW(), NOW()),
    -- Starter Plan
    (1, 'Đầy đủ tính năng, không giới hạn', 'All features included', '全機能利用可能', 1, true, false, NOW(), NOW()),
    (1, 'Chấm công, ca làm, nghỉ phép', 'Attendance, shifts, leaves', '勤怠・シフト・休暇管理', 2, false, false, NOW(), NOW()),
    (1, 'Tính lương & phiếu lương tự động', 'Auto payroll & payslips', '自動給与計算・給与明細', 3, false, false, NOW(), NOW()),
    (1, 'Hợp đồng, báo cáo, xuất PDF/CSV', 'Contracts, reports, PDF/CSV export', '契約・レポート・PDF/CSV出力', 4, false, false, NOW(), NOW()),
    (1, 'Đa ngôn ngữ: Việt, Nhật, Anh', 'Multi-language: VI, JA, EN', '多言語対応：越・日・英', 5, false, false, NOW(), NOW()),
    -- Business Plan
    (2, 'Đầy đủ tính năng, không giới hạn', 'All features included', '全機能利用可能', 1, true, false, NOW(), NOW()),
    (2, 'Chấm công, ca làm, nghỉ phép', 'Attendance, shifts, leaves', '勤怠・シフト・休暇管理', 2, false, false, NOW(), NOW()),
    (2, 'Tính lương & phiếu lương tự động', 'Auto payroll & payslips', '自動給与計算・給与明細', 3, false, false, NOW(), NOW()),
    (2, 'Hợp đồng, báo cáo, xuất PDF/CSV', 'Contracts, reports, PDF/CSV export', '契約・レポート・PDF/CSV出力', 4, false, false, NOW(), NOW()),
    (2, 'Đa ngôn ngữ: Việt, Nhật, Anh', 'Multi-language: VI, JA, EN', '多言語対応：越・日・英', 5, false, false, NOW(), NOW()),
    -- Enterprise Plan
    (3, 'Đầy đủ tính năng, không giới hạn', 'All features included', '全機能利用可能', 1, true, false, NOW(), NOW()),
    (3, 'Chấm công, ca làm, nghỉ phép', 'Attendance, shifts, leaves', '勤怠・シフト・休暇管理', 2, false, false, NOW(), NOW()),
    (3, 'Tính lương & phiếu lương tự động', 'Auto payroll & payslips', '自動給与計算・給与明細', 3, false, false, NOW(), NOW()),
    (3, 'Hợp đồng, báo cáo, xuất PDF/CSV', 'Contracts, reports, PDF/CSV export', '契約・レポート・PDF/CSV出力', 4, false, false, NOW(), NOW()),
    (3, 'Đa ngôn ngữ: Việt, Nhật, Anh', 'Multi-language: VI, JA, EN', '多言語対応：越・日・英', 5, false, false, NOW(), NOW()),
    -- Custom Plan
    (4, 'Không giới hạn nhân viên', 'Unlimited employees', '従業員数無制限', 1, true, false, NOW(), NOW()),
    (4, 'Chỉ ¥400/nhân viên/tháng', 'Only ¥400/employee/month', '¥400/人/月のみ', 2, true, false, NOW(), NOW()),
    (4, 'Đầy đủ tính năng, không giới hạn', 'All features included', '全機能利用可能', 3, false, false, NOW(), NOW()),
    (4, 'Tính lương, chấm công, nghỉ phép', 'Payroll, attendance, leaves', '給与計算・勤怠・休暇', 4, false, false, NOW(), NOW()),
    (4, 'Đa ngôn ngữ: Việt, Nhật, Anh', 'Multi-language: VI, JA, EN', '多言語対応：越・日・英', 5, false, false, NOW(), NOW());

-- Reset sequences
SELECT setval('plans_id_seq', (SELECT MAX(id) FROM plans));

-- Add CUSTOM_PRICE_PER_EMPLOYEE setting if not exists
INSERT INTO tamabee_settings (setting_key, setting_value, description, value_type, deleted, created_at, updated_at)
SELECT 'CUSTOM_PRICE_PER_EMPLOYEE', '400', 'Giá mỗi nhân viên cho gói Custom (JPY)', 'INTEGER', false, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM tamabee_settings WHERE setting_key = 'CUSTOM_PRICE_PER_EMPLOYEE');

-- Tạo company Tamabee với id=0 (nếu chưa tồn tại)
INSERT INTO companies (id, name, owner_name, email, phone, address, industry, zipcode, region, language, tenant_domain, plan_id, status, deleted, created_at, updated_at)
SELECT 0, 'Tamabee株式会社', 'Tamabee Admin', 'contact@tamabee.vn', '03-1234-5678', '東京都渋谷区', 'IT Services', '150-0001', 'ja', 'ja', 'tamabee', 0, 'ACTIVE', false, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM companies WHERE tenant_domain = 'tamabee');

-- Tạo wallet cho Tamabee (nếu chưa tồn tại)
INSERT INTO wallets (company_id, balance, total_billing, last_billing_date, next_billing_date, deleted, created_at, updated_at)
SELECT 0, 0, 0, NOW(), NOW() + INTERVAL '1 month', false, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM wallets WHERE company_id = 0);

-- Fix Tamabee company (nếu đã tồn tại nhưng thiếu thông tin)
UPDATE companies 
SET plan_id = 0, 
    name = 'Tamabee株式会社', 
    email = 'contact@tamabee.vn',
    status = 'ACTIVE',
    region = 'ja',
    language = 'ja',
    owner_name = 'Tamabee Admin'
WHERE tenant_domain = 'tamabee' AND (plan_id IS NULL OR plan_id != 0);
