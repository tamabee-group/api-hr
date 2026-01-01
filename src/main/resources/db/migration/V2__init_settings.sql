-- =====================================================
-- TAMABEE HR - DEFAULT SETTINGS
-- Currency: JPY (Japanese Yen)
-- =====================================================

-- System settings
INSERT INTO tamabee_settings (setting_key, setting_value, description, value_type, deleted, created_at, updated_at) VALUES
    ('FREE_TRIAL_MONTHS', '2', 'Số tháng miễn phí cho company mới đăng ký', 'INTEGER', false, NOW(), NOW()),
    ('REFERRAL_BONUS_MONTHS', '1', 'Số tháng miễn phí thêm khi có mã giới thiệu', 'INTEGER', false, NOW(), NOW()),
    ('COMMISSION_AMOUNT', '5000', 'Số tiền hoa hồng giới thiệu cố định (JPY)', 'INTEGER', false, NOW(), NOW()),
    ('MIN_DEPOSIT_AMOUNT', '5000', 'Số tiền nạp tối thiểu (JPY)', 'INTEGER', false, NOW(), NOW()),
    ('BANK_NAME', 'MUFG', 'Tên ngân hàng nhận tiền', 'STRING', false, NOW(), NOW()),
    ('BANK_ACCOUNT', '1234567', 'Số tài khoản nhận tiền', 'STRING', false, NOW(), NOW()),
    ('BANK_ACCOUNT_NAME', 'タマビー株式会社', 'Tên chủ tài khoản', 'STRING', false, NOW(), NOW());

-- Default plans (JPY)
INSERT INTO plans (id, name_vi, name_en, name_ja, description_vi, description_en, description_ja, monthly_price, max_employees, is_active, deleted, created_at, updated_at) VALUES
    (1, 'Gói Cơ bản', 'Basic Plan', 'ベーシックプラン', 'Phù hợp cho doanh nghiệp nhỏ', 'Suitable for small businesses', '中小企業向け', 5000, 10, true, false, NOW(), NOW()),
    (2, 'Gói Tiêu chuẩn', 'Standard Plan', 'スタンダードプラン', 'Phù hợp cho doanh nghiệp vừa', 'Suitable for medium businesses', '中堅企業向け', 10000, 50, true, false, NOW(), NOW()),
    (3, 'Gói Doanh nghiệp', 'Enterprise Plan', 'エンタープライズプラン', 'Phù hợp cho doanh nghiệp lớn', 'Suitable for large businesses', '大企業向け', 20000, 200, true, false, NOW(), NOW());

-- Plan features
INSERT INTO plan_features (plan_id, feature_vi, feature_en, feature_ja, sort_order, is_highlighted, deleted, created_at, updated_at) VALUES
    -- Basic Plan
    (1, 'Quản lý tối đa 10 nhân viên', 'Manage up to 10 employees', '最大10名の従業員管理', 1, true, false, NOW(), NOW()),
    (1, 'Báo cáo cơ bản', 'Basic reports', '基本レポート', 2, false, false, NOW(), NOW()),
    (1, 'Hỗ trợ email', 'Email support', 'メールサポート', 3, false, false, NOW(), NOW()),
    -- Standard Plan
    (2, 'Quản lý tối đa 50 nhân viên', 'Manage up to 50 employees', '最大50名の従業員管理', 1, true, false, NOW(), NOW()),
    (2, 'Báo cáo nâng cao', 'Advanced reports', '高度なレポート', 2, false, false, NOW(), NOW()),
    (2, 'Hỗ trợ 24/7', '24/7 support', '24時間サポート', 3, true, false, NOW(), NOW()),
    -- Enterprise Plan
    (3, 'Quản lý tối đa 200 nhân viên', 'Manage up to 200 employees', '最大200名の従業員管理', 1, true, false, NOW(), NOW()),
    (3, 'Báo cáo tùy chỉnh', 'Custom reports', 'カスタムレポート', 2, false, false, NOW(), NOW()),
    (3, 'Hỗ trợ ưu tiên', 'Priority support', '優先サポート', 3, true, false, NOW(), NOW()),
    (3, 'API tích hợp', 'API integration', 'API連携', 4, false, false, NOW(), NOW());

-- Reset sequences
SELECT setval('plans_id_seq', (SELECT MAX(id) FROM plans));

-- =====================================================
-- PLAN FEATURE CODES - Mapping Plan với Feature Code
-- =====================================================
INSERT INTO plan_feature_codes (plan_id, feature_code, created_at, deleted) VALUES
    -- Plan 1 (Basic): ATTENDANCE only
    (1, 'ATTENDANCE', NOW(), FALSE),
    -- Plan 2 (Standard): ATTENDANCE, PAYROLL, OVERTIME, LEAVE_MANAGEMENT
    (2, 'ATTENDANCE', NOW(), FALSE),
    (2, 'PAYROLL', NOW(), FALSE),
    (2, 'OVERTIME', NOW(), FALSE),
    (2, 'LEAVE_MANAGEMENT', NOW(), FALSE),
    -- Plan 3 (Premium): All features
    (3, 'ATTENDANCE', NOW(), FALSE),
    (3, 'PAYROLL', NOW(), FALSE),
    (3, 'OVERTIME', NOW(), FALSE),
    (3, 'LEAVE_MANAGEMENT', NOW(), FALSE),
    (3, 'GEO_LOCATION', NOW(), FALSE),
    (3, 'DEVICE_REGISTRATION', NOW(), FALSE),
    (3, 'REPORTS', NOW(), FALSE),
    (3, 'FLEXIBLE_SCHEDULE', NOW(), FALSE);
