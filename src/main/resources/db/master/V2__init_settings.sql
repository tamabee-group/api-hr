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

-- Default plans (JPY)
-- Plan 0: Free - 200 người, ¥0 (trial)
-- Plan 1: Basic - 10 người, ¥5,000
-- Plan 2: Standard - 20 người, ¥15,000 (gấp 3 Basic)
-- Plan 3: Enterprise - 40 người, ¥20,000 (gấp 4 Basic)
-- Plan 4: Custom - Tính theo người, ¥400/người (từ setting)
INSERT INTO plans (id, name_vi, name_en, name_ja, description_vi, description_en, description_ja, monthly_price, max_employees, is_active, deleted, created_at, updated_at) VALUES
    (0, 'Gói Miễn phí', 'Free Plan', 'フリープラン', 'Dùng thử miễn phí trong 2 tháng', 'Free trial for 2 months', '2ヶ月間無料トライアル', 0, 200, true, false, NOW(), NOW()),
    (1, 'Gói Cơ bản', 'Basic Plan', 'ベーシックプラン', 'Phù hợp cho doanh nghiệp nhỏ', 'Suitable for small businesses', '中小企業向け', 5000, 10, true, false, NOW(), NOW()),
    (2, 'Gói Tiêu chuẩn', 'Standard Plan', 'スタンダードプラン', 'Phù hợp cho doanh nghiệp vừa', 'Suitable for medium businesses', '中堅企業向け', 15000, 20, true, false, NOW(), NOW()),
    (3, 'Gói Doanh nghiệp', 'Enterprise Plan', 'エンタープライズプラン', 'Phù hợp cho doanh nghiệp lớn', 'Suitable for large businesses', '大企業向け', 20000, 40, true, false, NOW(), NOW()),
    (4, 'Gói Tùy chỉnh', 'Custom Plan', 'カスタムプラン', 'Tính theo số nhân viên, ¥400/người', 'Pay per employee, ¥400/person', '従業員数に応じた料金、¥400/人', 0, 0, true, false, NOW(), NOW());

-- Plan features (mô tả)
INSERT INTO plan_features (plan_id, feature_vi, feature_en, feature_ja, sort_order, is_highlighted, deleted, created_at, updated_at) VALUES
    -- Free Plan
    (0, 'Tối đa 200 nhân viên', 'Up to 200 employees', '最大200名', 1, true, false, NOW(), NOW()),
    (0, 'Đầy đủ tính năng trong 2 tháng', 'Full features for 2 months', '2ヶ月間全機能利用可能', 2, true, false, NOW(), NOW()),
    (0, 'Hỗ trợ email', 'Email support', 'メールサポート', 3, false, false, NOW(), NOW()),
    -- Basic Plan
    (1, 'Tối đa 10 nhân viên', 'Up to 10 employees', '最大10名', 1, true, false, NOW(), NOW()),
    (1, 'Chấm công cơ bản', 'Basic attendance', '基本勤怠管理', 2, false, false, NOW(), NOW()),
    (1, 'Hỗ trợ email', 'Email support', 'メールサポート', 3, false, false, NOW(), NOW()),
    -- Standard Plan
    (2, 'Tối đa 20 nhân viên', 'Up to 20 employees', '最大20名', 1, true, false, NOW(), NOW()),
    (2, 'Tính lương tự động', 'Auto payroll', '自動給与計算', 2, true, false, NOW(), NOW()),
    (2, 'Quản lý nghỉ phép', 'Leave management', '休暇管理', 3, false, false, NOW(), NOW()),
    (2, 'Hỗ trợ 24/7', '24/7 support', '24時間サポート', 4, false, false, NOW(), NOW()),
    -- Enterprise Plan
    (3, 'Tối đa 40 nhân viên', 'Up to 40 employees', '最大40名', 1, true, false, NOW(), NOW()),
    (3, 'Tất cả tính năng Standard', 'All Standard features', 'スタンダード全機能', 2, false, false, NOW(), NOW()),
    (3, 'Báo cáo nâng cao', 'Advanced reports', '高度なレポート', 3, true, false, NOW(), NOW()),
    (3, 'Hỗ trợ ưu tiên', 'Priority support', '優先サポート', 4, true, false, NOW(), NOW()),
    -- Custom Plan
    (4, 'Số nhân viên không giới hạn', 'Unlimited employees', '従業員数無制限', 1, true, false, NOW(), NOW()),
    (4, '¥400/nhân viên/tháng', '¥400/employee/month', '¥400/人/月', 2, true, false, NOW(), NOW()),
    (4, 'Tất cả tính năng', 'All features included', '全機能利用可能', 3, false, false, NOW(), NOW()),
    (4, 'Hỗ trợ chuyên biệt', 'Dedicated support', '専任サポート', 4, false, false, NOW(), NOW());

-- Reset sequences
SELECT setval('plans_id_seq', (SELECT MAX(id) FROM plans));

-- Plan feature codes (mapping)
INSERT INTO plan_feature_codes (plan_id, feature_code, created_at, deleted) VALUES
    -- Plan 0 (Free): All features during trial
    (0, 'ATTENDANCE', NOW(), FALSE),
    (0, 'PAYROLL', NOW(), FALSE),
    (0, 'OVERTIME', NOW(), FALSE),
    (0, 'LEAVE_MANAGEMENT', NOW(), FALSE),
    (0, 'GEO_LOCATION', NOW(), FALSE),
    (0, 'DEVICE_REGISTRATION', NOW(), FALSE),
    (0, 'REPORTS', NOW(), FALSE),
    (0, 'FLEXIBLE_SCHEDULE', NOW(), FALSE),
    -- Plan 1 (Basic): ATTENDANCE only
    (1, 'ATTENDANCE', NOW(), FALSE),
    -- Plan 2 (Standard): ATTENDANCE, PAYROLL, OVERTIME, LEAVE_MANAGEMENT
    (2, 'ATTENDANCE', NOW(), FALSE),
    (2, 'PAYROLL', NOW(), FALSE),
    (2, 'OVERTIME', NOW(), FALSE),
    (2, 'LEAVE_MANAGEMENT', NOW(), FALSE),
    -- Plan 3 (Enterprise): All features
    (3, 'ATTENDANCE', NOW(), FALSE),
    (3, 'PAYROLL', NOW(), FALSE),
    (3, 'OVERTIME', NOW(), FALSE),
    (3, 'LEAVE_MANAGEMENT', NOW(), FALSE),
    (3, 'GEO_LOCATION', NOW(), FALSE),
    (3, 'DEVICE_REGISTRATION', NOW(), FALSE),
    (3, 'REPORTS', NOW(), FALSE),
    (3, 'FLEXIBLE_SCHEDULE', NOW(), FALSE),
    -- Plan 4 (Custom): All features
    (4, 'ATTENDANCE', NOW(), FALSE),
    (4, 'PAYROLL', NOW(), FALSE),
    (4, 'OVERTIME', NOW(), FALSE),
    (4, 'LEAVE_MANAGEMENT', NOW(), FALSE),
    (4, 'GEO_LOCATION', NOW(), FALSE),
    (4, 'DEVICE_REGISTRATION', NOW(), FALSE),
    (4, 'REPORTS', NOW(), FALSE),
    (4, 'FLEXIBLE_SCHEDULE', NOW(), FALSE);

-- Fix Tamabee company
UPDATE companies 
SET plan_id = 0, 
    name = 'Tamabee株式会社', 
    email = 'contact@tamabee.vn',
    status = 'ACTIVE',
    locale = 'ja',
    language = 'ja',
    owner_name = 'Tamabee Admin'
WHERE tenant_domain = 'tamabee' AND (plan_id IS NULL OR plan_id != 0);

-- Update existing plans data
UPDATE plans SET max_employees = 200, monthly_price = 0, description_vi = 'Dùng thử miễn phí trong 2 tháng', description_en = 'Free trial for 2 months', description_ja = '2ヶ月間無料トライアル' WHERE id = 0;
UPDATE plans SET max_employees = 10, monthly_price = 5000 WHERE id = 1;
UPDATE plans SET max_employees = 20, monthly_price = 15000, description_vi = 'Phù hợp cho doanh nghiệp vừa', description_en = 'Suitable for medium businesses', description_ja = '中堅企業向け' WHERE id = 2;
UPDATE plans SET max_employees = 40, monthly_price = 20000, description_vi = 'Phù hợp cho doanh nghiệp lớn', description_en = 'Suitable for large businesses', description_ja = '大企業向け' WHERE id = 3;
UPDATE plans SET max_employees = 0, monthly_price = 0, description_vi = 'Tính theo số nhân viên, ¥400/người', description_en = 'Pay per employee, ¥400/person', description_ja = '従業員数に応じた料金、¥400/人' WHERE id = 4;

-- Add CUSTOM_PRICE_PER_EMPLOYEE setting if not exists
INSERT INTO tamabee_settings (setting_key, setting_value, description, value_type, deleted, created_at, updated_at)
SELECT 'CUSTOM_PRICE_PER_EMPLOYEE', '400', 'Giá mỗi nhân viên cho gói Custom (JPY)', 'INTEGER', false, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM tamabee_settings WHERE setting_key = 'CUSTOM_PRICE_PER_EMPLOYEE');
