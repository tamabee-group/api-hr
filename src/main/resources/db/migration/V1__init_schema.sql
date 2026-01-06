-- =====================================================
-- TAMABEE HR - DATABASE SCHEMA
-- Version: 1.0
-- Currency: JPY (Japanese Yen)
-- =====================================================

-- =====================================================
-- 1. EMAIL VERIFICATIONS - Xác thực email đăng ký
-- =====================================================
CREATE TABLE email_verifications (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    code VARCHAR(6) NOT NULL,
    company_name VARCHAR(255),
    expired_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_email_verifications_email ON email_verifications(email);
CREATE INDEX idx_email_verifications_expired ON email_verifications(expired_at);
CREATE INDEX idx_email_verifications_deleted ON email_verifications(deleted);

-- =====================================================
-- 2. PLANS - Gói dịch vụ subscription (JPY)
-- =====================================================
CREATE TABLE plans (
    id BIGSERIAL PRIMARY KEY,
    name_vi VARCHAR(255) NOT NULL,
    name_en VARCHAR(255) NOT NULL,
    name_ja VARCHAR(255) NOT NULL,
    description_vi TEXT,
    description_en TEXT,
    description_ja TEXT,
    monthly_price DECIMAL(15,0) NOT NULL,
    max_employees INTEGER NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_plans_deleted ON plans(deleted);
CREATE INDEX idx_plans_is_active ON plans(is_active);

-- Plan features - Tính năng của gói dịch vụ
CREATE TABLE plan_features (
    id BIGSERIAL PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    feature_vi VARCHAR(500) NOT NULL,
    feature_en VARCHAR(500) NOT NULL,
    feature_ja VARCHAR(500) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    is_highlighted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_plan_features_plan_id ON plan_features(plan_id);
CREATE INDEX idx_plan_features_deleted ON plan_features(deleted);


-- =====================================================
-- 3. COMPANIES - Công ty khách hàng
-- =====================================================
CREATE TABLE companies (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    owner_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(50) NOT NULL,
    address TEXT NOT NULL,
    industry VARCHAR(100) NOT NULL,
    zipcode VARCHAR(20),
    locale VARCHAR(50) NOT NULL,
    language VARCHAR(10) NOT NULL,
    logo VARCHAR(500),
    tenant_domain VARCHAR(50) UNIQUE NOT NULL,
    plan_id BIGINT,
    referred_by_employee_id BIGINT,
    owner_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    deactivated_at TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_companies_email ON companies(email);
CREATE UNIQUE INDEX idx_companies_tenant_domain ON companies(tenant_domain);
CREATE INDEX idx_companies_plan_id ON companies(plan_id);
CREATE INDEX idx_companies_referred_by ON companies(referred_by_employee_id);
CREATE INDEX idx_companies_owner_id ON companies(owner_id);
CREATE INDEX idx_companies_status ON companies(status);
CREATE INDEX idx_companies_deleted ON companies(deleted);
CREATE INDEX idx_companies_deactivated_at ON companies(deactivated_at);

-- =====================================================
-- 4. WALLETS - Ví tiền công ty (JPY)
-- =====================================================
CREATE TABLE wallets (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT UNIQUE NOT NULL,
    balance DECIMAL(15,0) NOT NULL DEFAULT 0,
    total_billing DECIMAL(15,0) NOT NULL DEFAULT 0,
    last_billing_date TIMESTAMP NOT NULL,
    next_billing_date TIMESTAMP NOT NULL,
    free_trial_end_date TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_wallets_company_id ON wallets(company_id);
CREATE INDEX idx_wallets_free_trial ON wallets(free_trial_end_date);
CREATE INDEX idx_wallets_deleted ON wallets(deleted);

-- =====================================================
-- 5. WALLET_TRANSACTIONS - Lịch sử giao dịch ví (JPY)
-- =====================================================
CREATE TABLE wallet_transactions (
    id BIGSERIAL PRIMARY KEY,
    wallet_id BIGINT NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    amount DECIMAL(15,0) NOT NULL,
    balance_before DECIMAL(15,0) NOT NULL,
    balance_after DECIMAL(15,0) NOT NULL,
    description VARCHAR(500),
    reference_id BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_wallet_transactions_wallet_id ON wallet_transactions(wallet_id);
CREATE INDEX idx_wallet_transactions_type ON wallet_transactions(transaction_type);
CREATE INDEX idx_wallet_transactions_deleted ON wallet_transactions(deleted);
CREATE INDEX idx_wallet_transactions_created_at ON wallet_transactions(created_at DESC);


-- =====================================================
-- 6. DEPOSIT_REQUESTS - Yêu cầu nạp tiền (JPY)
-- =====================================================
CREATE TABLE deposit_requests (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL,
    amount DECIMAL(15,0) NOT NULL,
    transfer_proof_url VARCHAR(500) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    requested_by VARCHAR(50) NOT NULL,
    approved_by VARCHAR(50),
    rejection_reason VARCHAR(500),
    processed_at TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_deposit_requests_company_id ON deposit_requests(company_id);
CREATE INDEX idx_deposit_requests_status ON deposit_requests(status);
CREATE INDEX idx_deposit_requests_deleted ON deposit_requests(deleted);
CREATE INDEX idx_deposit_requests_created_at ON deposit_requests(created_at DESC);

-- =====================================================
-- 7. USERS - Người dùng hệ thống
-- =====================================================
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    employee_code VARCHAR(10) UNIQUE,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    locale VARCHAR(50) NOT NULL,
    language VARCHAR(10) NOT NULL,
    company_id BIGINT NOT NULL DEFAULT 0,
    profile_completeness INTEGER NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_employee_code ON users(employee_code);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_company_id ON users(company_id);
CREATE INDEX idx_users_deleted ON users(deleted);


-- =====================================================
-- 8. USER_PROFILES - Thông tin cá nhân người dùng
-- =====================================================
CREATE TABLE user_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    name VARCHAR(255),
    phone VARCHAR(50),
    address TEXT,
    zip_code VARCHAR(20),
    date_of_birth VARCHAR(20),
    gender VARCHAR(20),
    avatar VARCHAR(500),
    referral_code VARCHAR(10) UNIQUE,
    -- Bank info - Common
    bank_account_type VARCHAR(10) DEFAULT 'VN',
    japan_bank_type VARCHAR(10) DEFAULT 'normal',
    bank_name VARCHAR(255),
    bank_account VARCHAR(255),
    bank_account_name VARCHAR(255),
    -- Bank info - Japan (ngân hàng thông thường)
    bank_code VARCHAR(10),
    bank_branch_code VARCHAR(10),
    bank_branch_name VARCHAR(255),
    bank_account_category VARCHAR(20),
    -- Bank info - Japan Post Bank (ゆうちょ銀行)
    bank_symbol VARCHAR(10),
    bank_number VARCHAR(15),
    -- Emergency contact
    emergency_contact_name VARCHAR(255),
    emergency_contact_phone VARCHAR(50),
    emergency_contact_relation VARCHAR(100),
    emergency_contact_address TEXT,
    -- Soft delete & timestamps
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_profiles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_user_profiles_user_id ON user_profiles(user_id);
CREATE INDEX idx_user_profiles_referral_code ON user_profiles(referral_code);
CREATE INDEX idx_user_profiles_bank_type ON user_profiles(bank_account_type);
CREATE INDEX idx_user_profiles_deleted ON user_profiles(deleted);

-- =====================================================
-- 9. EMPLOYEE_COMMISSIONS - Hoa hồng nhân viên Tamabee (JPY)
-- Status flow: PENDING -> ELIGIBLE -> PAID
-- =====================================================
CREATE TABLE employee_commissions (
    id BIGSERIAL PRIMARY KEY,
    employee_code VARCHAR(50) NOT NULL,
    company_id BIGINT NOT NULL,
    amount DECIMAL(15,0) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    company_billing_at_creation DECIMAL(15,0) NOT NULL DEFAULT 0,
    paid_at TIMESTAMP,
    paid_by VARCHAR(50),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_employee_commissions_employee_code ON employee_commissions(employee_code);
CREATE INDEX idx_employee_commissions_company_id ON employee_commissions(company_id);
CREATE INDEX idx_employee_commissions_status ON employee_commissions(status);
CREATE INDEX idx_employee_commissions_deleted ON employee_commissions(deleted);
CREATE INDEX idx_employee_commissions_created_at ON employee_commissions(created_at DESC);

-- =====================================================
-- 10. TAMABEE_SETTINGS - Cấu hình hệ thống
-- =====================================================
CREATE TABLE tamabee_settings (
    id BIGSERIAL PRIMARY KEY,
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    value_type VARCHAR(50) NOT NULL DEFAULT 'STRING',
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_tamabee_settings_key ON tamabee_settings(setting_key);
CREATE INDEX idx_tamabee_settings_deleted ON tamabee_settings(deleted);

-- =====================================================
-- 11. COMPANY_SETTINGS - Cấu hình chấm công/lương của công ty
-- =====================================================
CREATE TABLE company_settings (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL UNIQUE,
    attendance_config JSONB,
    payroll_config JSONB,
    overtime_config JSONB,
    allowance_config JSONB,
    deduction_config JSONB,
    break_config JSONB,
    -- Work mode settings
    work_mode VARCHAR(20) NOT NULL DEFAULT 'FLEXIBLE_SHIFT',
    default_work_start_time TIME,
    default_work_end_time TIME,
    default_break_minutes INTEGER,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50)
);

CREATE UNIQUE INDEX idx_company_settings_company_id ON company_settings(company_id);
CREATE INDEX idx_company_settings_deleted ON company_settings(deleted);
CREATE INDEX idx_company_settings_work_mode ON company_settings(work_mode);

COMMENT ON COLUMN company_settings.work_mode IS 'Chế độ làm việc: FIXED_HOURS (giờ cố định) hoặc FLEXIBLE_SHIFT (linh hoạt/theo ca)';
COMMENT ON COLUMN company_settings.default_work_start_time IS 'Giờ bắt đầu làm việc mặc định (dùng cho FIXED_HOURS mode)';
COMMENT ON COLUMN company_settings.default_work_end_time IS 'Giờ kết thúc làm việc mặc định (dùng cho FIXED_HOURS mode)';
COMMENT ON COLUMN company_settings.default_break_minutes IS 'Thời gian nghỉ giải lao mặc định (phút) (dùng cho FIXED_HOURS mode)';

-- =====================================================
-- 12. WORK_SCHEDULES - Lịch làm việc
-- =====================================================
CREATE TABLE work_schedules (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    schedule_data JSONB,
    description VARCHAR(500),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50)
);

CREATE INDEX idx_work_schedules_deleted ON work_schedules(deleted);
CREATE INDEX idx_work_schedules_company_id ON work_schedules(company_id);
CREATE INDEX idx_work_schedules_type ON work_schedules(type);
CREATE INDEX idx_work_schedules_is_default ON work_schedules(is_default);
CREATE INDEX idx_work_schedules_is_active ON work_schedules(company_id, is_active);

COMMENT ON COLUMN work_schedules.is_active IS 'Trạng thái hoạt động của lịch làm việc (FALSE khi switch sang FIXED_HOURS)';

-- =====================================================
-- 13. WORK_SCHEDULE_ASSIGNMENTS - Gán lịch làm việc cho nhân viên
-- =====================================================
CREATE TABLE work_schedule_assignments (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    schedule_id BIGINT NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50)
);

CREATE INDEX idx_work_schedule_assignments_deleted ON work_schedule_assignments(deleted);
CREATE INDEX idx_work_schedule_assignments_employee_id ON work_schedule_assignments(employee_id);
CREATE INDEX idx_work_schedule_assignments_schedule_id ON work_schedule_assignments(schedule_id);
CREATE INDEX idx_work_schedule_assignments_effective_from ON work_schedule_assignments(effective_from);
CREATE INDEX idx_work_schedule_assignments_effective_to ON work_schedule_assignments(effective_to);

-- =====================================================
-- 14. ATTENDANCE_RECORDS - Bản ghi chấm công
-- =====================================================
CREATE TABLE attendance_records (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    work_date DATE NOT NULL,
    -- Thời gian gốc
    original_check_in TIMESTAMP,
    original_check_out TIMESTAMP,
    -- Thời gian sau khi làm tròn
    rounded_check_in TIMESTAMP,
    rounded_check_out TIMESTAMP,
    -- Tính toán (phút)
    working_minutes INTEGER DEFAULT 0,
    overtime_minutes INTEGER DEFAULT 0,
    late_minutes INTEGER DEFAULT 0,
    early_leave_minutes INTEGER DEFAULT 0,
    -- Break time fields
    total_break_minutes INTEGER DEFAULT 0,
    effective_break_minutes INTEGER DEFAULT 0,
    break_type VARCHAR(20),
    break_compliant BOOLEAN,
    -- Trạng thái
    status VARCHAR(50) NOT NULL DEFAULT 'PRESENT',
    -- Device & Location
    check_in_device_id VARCHAR(255),
    check_out_device_id VARCHAR(255),
    check_in_latitude DOUBLE PRECISION,
    check_in_longitude DOUBLE PRECISION,
    check_out_latitude DOUBLE PRECISION,
    check_out_longitude DOUBLE PRECISION,
    -- Audit
    adjustment_reason VARCHAR(500),
    adjusted_by BIGINT,
    adjusted_at TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50)
);

CREATE INDEX idx_attendance_records_deleted ON attendance_records(deleted);
CREATE INDEX idx_attendance_records_employee_id ON attendance_records(employee_id);
CREATE INDEX idx_attendance_records_company_id ON attendance_records(company_id);
CREATE INDEX idx_attendance_records_work_date ON attendance_records(work_date);
CREATE INDEX idx_attendance_records_status ON attendance_records(status);
CREATE INDEX idx_attendance_records_employee_date ON attendance_records(employee_id, work_date);
CREATE INDEX idx_attendance_records_company_date ON attendance_records(company_id, work_date);

-- =====================================================
-- 14.1. BREAK_RECORDS - Bản ghi giờ giải lao
-- =====================================================
CREATE TABLE break_records (
    id BIGSERIAL PRIMARY KEY,
    attendance_record_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    work_date DATE NOT NULL,
    -- Số thứ tự break trong ngày (1, 2, 3, ...)
    break_number INTEGER NOT NULL DEFAULT 1,
    -- Thời gian giải lao
    break_start TIMESTAMP,
    break_end TIMESTAMP,
    -- Thời gian tính toán (phút)
    actual_break_minutes INTEGER DEFAULT 0,
    effective_break_minutes INTEGER DEFAULT 0,
    -- Ghi chú
    notes VARCHAR(500),
    -- Audit
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50)
);

CREATE INDEX idx_break_records_deleted ON break_records(deleted);
CREATE INDEX idx_break_records_attendance_id ON break_records(attendance_record_id) WHERE deleted = FALSE;
CREATE INDEX idx_break_records_employee_id ON break_records(employee_id);
CREATE INDEX idx_break_records_company_id ON break_records(company_id);
CREATE INDEX idx_break_records_work_date ON break_records(work_date);
CREATE INDEX idx_break_records_employee_date ON break_records(employee_id, work_date) WHERE deleted = FALSE;
CREATE INDEX idx_break_records_company_date ON break_records(company_id, work_date) WHERE deleted = FALSE;
CREATE INDEX idx_break_records_break_number ON break_records(attendance_record_id, break_number) WHERE deleted = FALSE;

-- =====================================================
-- 15. PAYROLL_RECORDS - Bản ghi lương
-- =====================================================
CREATE TABLE payroll_records (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    year INTEGER NOT NULL,
    month INTEGER NOT NULL,
    -- Lương cơ bản
    salary_type VARCHAR(50) NOT NULL,
    base_salary DECIMAL(15, 2) NOT NULL DEFAULT 0,
    -- Số ngày/giờ làm việc thực tế
    working_days INTEGER,
    working_hours INTEGER,
    -- Tăng ca (tiền)
    regular_overtime_pay DECIMAL(15, 2) DEFAULT 0,
    night_overtime_pay DECIMAL(15, 2) DEFAULT 0,
    holiday_overtime_pay DECIMAL(15, 2) DEFAULT 0,
    weekend_overtime_pay DECIMAL(15, 2) DEFAULT 0,
    total_overtime_pay DECIMAL(15, 2) DEFAULT 0,
    -- Số giờ tăng ca
    regular_overtime_hours INTEGER,
    night_overtime_hours INTEGER,
    holiday_overtime_hours INTEGER,
    weekend_overtime_hours INTEGER,
    -- Phụ cấp
    allowance_details JSONB,
    total_allowances DECIMAL(15, 2) DEFAULT 0,
    -- Khấu trừ
    deduction_details JSONB,
    total_deductions DECIMAL(15, 2) DEFAULT 0,
    -- Break time tracking
    total_break_minutes INTEGER DEFAULT 0,
    break_type VARCHAR(20),
    break_deduction_amount DECIMAL(15, 2) DEFAULT 0,
    -- Tổng kết
    gross_salary DECIMAL(15, 2) NOT NULL DEFAULT 0,
    net_salary DECIMAL(15, 2) NOT NULL DEFAULT 0,
    -- Trạng thái
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    -- Payment tracking
    payment_status VARCHAR(50) DEFAULT 'PENDING',
    paid_at TIMESTAMP,
    payment_reference VARCHAR(255),
    -- Notification
    notification_sent BOOLEAN NOT NULL DEFAULT FALSE,
    notification_sent_at TIMESTAMP,
    -- Finalization
    finalized_at TIMESTAMP,
    finalized_by BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50)
);

CREATE INDEX idx_payroll_records_deleted ON payroll_records(deleted);
CREATE INDEX idx_payroll_records_employee_id ON payroll_records(employee_id);
CREATE INDEX idx_payroll_records_company_id ON payroll_records(company_id);
CREATE INDEX idx_payroll_records_year ON payroll_records(year);
CREATE INDEX idx_payroll_records_month ON payroll_records(month);
CREATE INDEX idx_payroll_records_status ON payroll_records(status);
CREATE INDEX idx_payroll_records_payment_status ON payroll_records(payment_status);
CREATE INDEX idx_payroll_records_company_period ON payroll_records(company_id, year, month);
CREATE INDEX idx_payroll_records_employee_period ON payroll_records(employee_id, year, month);

-- =====================================================
-- 15.1. EMPLOYEE_SALARIES - Thông tin lương của nhân viên
-- =====================================================
CREATE TABLE employee_salaries (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    -- Loại lương
    salary_type VARCHAR(50) NOT NULL,
    -- Lương theo loại
    monthly_salary DECIMAL(15, 2),
    daily_rate DECIMAL(15, 2),
    hourly_rate DECIMAL(15, 2),
    shift_rate DECIMAL(15, 2),
    -- Thời gian hiệu lực
    effective_from DATE NOT NULL,
    effective_to DATE,
    -- Ghi chú
    note VARCHAR(500),
    -- Audit
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50)
);

CREATE INDEX idx_emp_salary_deleted ON employee_salaries(deleted);
CREATE INDEX idx_emp_salary_employee_id ON employee_salaries(employee_id);
CREATE INDEX idx_emp_salary_company_id ON employee_salaries(company_id);
CREATE INDEX idx_emp_salary_effective ON employee_salaries(employee_id, effective_from);

-- =====================================================
-- 16. ATTENDANCE_ADJUSTMENT_REQUESTS - Yêu cầu điều chỉnh chấm công
-- =====================================================
CREATE TABLE attendance_adjustment_requests (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    attendance_record_id BIGINT,  -- Nullable khi tạo yêu cầu cho ngày chưa có chấm công
    work_date DATE,  -- Ngày làm việc cần điều chỉnh (dùng khi không có attendance_record_id)
    -- ID của break record cần điều chỉnh (cho multiple breaks support)
    break_record_id BIGINT,
    -- ID người được gán xử lý yêu cầu (manager/admin)
    assigned_to BIGINT,
    -- Thời gian gốc
    original_check_in TIMESTAMP,
    original_check_out TIMESTAMP,
    -- Thời gian break gốc
    original_break_start TIMESTAMP,
    original_break_end TIMESTAMP,
    -- Thời gian yêu cầu thay đổi
    requested_check_in TIMESTAMP,
    requested_check_out TIMESTAMP,
    -- Thời gian break yêu cầu thay đổi
    requested_break_start TIMESTAMP,
    requested_break_end TIMESTAMP,
    -- Lý do
    reason VARCHAR(500),
    -- Trạng thái
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    -- Approval info
    approved_by BIGINT,
    approved_at TIMESTAMP,
    approver_comment VARCHAR(500),
    rejection_reason VARCHAR(500),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50)
);

CREATE INDEX idx_adjustment_requests_deleted ON attendance_adjustment_requests(deleted);
CREATE INDEX idx_adjustment_requests_employee_id ON attendance_adjustment_requests(employee_id);
CREATE INDEX idx_adjustment_requests_company_id ON attendance_adjustment_requests(company_id);
CREATE INDEX idx_adjustment_requests_attendance_record_id ON attendance_adjustment_requests(attendance_record_id);
CREATE INDEX idx_adjustment_requests_work_date ON attendance_adjustment_requests(work_date);
CREATE INDEX idx_adjustment_requests_break_record_id ON attendance_adjustment_requests(break_record_id);
CREATE INDEX idx_adjustment_requests_assigned_to ON attendance_adjustment_requests(assigned_to);
CREATE INDEX idx_adjustment_requests_status ON attendance_adjustment_requests(status);

-- =====================================================
-- 17. SCHEDULE_SELECTIONS - Lựa chọn lịch làm việc của nhân viên
-- =====================================================
CREATE TABLE schedule_selections (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    schedule_id BIGINT NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE,
    -- Trạng thái
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    -- Approval info
    approved_by BIGINT,
    approved_at TIMESTAMP,
    rejection_reason VARCHAR(500),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50)
);

CREATE INDEX idx_schedule_selections_deleted ON schedule_selections(deleted);
CREATE INDEX idx_schedule_selections_employee_id ON schedule_selections(employee_id);
CREATE INDEX idx_schedule_selections_company_id ON schedule_selections(company_id);
CREATE INDEX idx_schedule_selections_schedule_id ON schedule_selections(schedule_id);
CREATE INDEX idx_schedule_selections_status ON schedule_selections(status);

-- =====================================================
-- 18. HOLIDAYS - Ngày lễ
-- =====================================================
CREATE TABLE holidays (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT,
    date DATE NOT NULL,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    is_paid BOOLEAN NOT NULL DEFAULT TRUE,
    description VARCHAR(500),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50)
);

CREATE INDEX idx_holidays_deleted ON holidays(deleted);
CREATE INDEX idx_holidays_company_id ON holidays(company_id);
CREATE INDEX idx_holidays_date ON holidays(date);
CREATE INDEX idx_holidays_type ON holidays(type);

-- =====================================================
-- 19. LEAVE_REQUESTS - Yêu cầu nghỉ phép
-- =====================================================
CREATE TABLE leave_requests (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    leave_type VARCHAR(50) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    total_days INTEGER,
    reason VARCHAR(500),
    -- Trạng thái
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    -- Approval info
    approved_by BIGINT,
    approved_at TIMESTAMP,
    rejection_reason VARCHAR(500),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50)
);

CREATE INDEX idx_leave_requests_deleted ON leave_requests(deleted);
CREATE INDEX idx_leave_requests_employee_id ON leave_requests(employee_id);
CREATE INDEX idx_leave_requests_company_id ON leave_requests(company_id);
CREATE INDEX idx_leave_requests_leave_type ON leave_requests(leave_type);
CREATE INDEX idx_leave_requests_status ON leave_requests(status);
CREATE INDEX idx_leave_requests_start_date ON leave_requests(start_date);
CREATE INDEX idx_leave_requests_end_date ON leave_requests(end_date);

-- =====================================================
-- 20. LEAVE_BALANCES - Số ngày nghỉ phép còn lại
-- =====================================================
CREATE TABLE leave_balances (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    year INTEGER NOT NULL,
    leave_type VARCHAR(50) NOT NULL,
    total_days INTEGER NOT NULL DEFAULT 0,
    used_days INTEGER NOT NULL DEFAULT 0,
    remaining_days INTEGER NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50)
);

CREATE INDEX idx_leave_balances_deleted ON leave_balances(deleted);
CREATE INDEX idx_leave_balances_employee_id ON leave_balances(employee_id);
CREATE INDEX idx_leave_balances_year ON leave_balances(year);
CREATE INDEX idx_leave_balances_leave_type ON leave_balances(leave_type);
CREATE INDEX idx_leave_balances_employee_year ON leave_balances(employee_id, year);


-- =====================================================
-- 21. PLAN_FEATURE_CODES - Mapping Plan với Feature Code
-- =====================================================
CREATE TABLE plan_feature_codes (
    id BIGSERIAL PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    feature_code VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(255),
    deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_plan_feature_codes_plan FOREIGN KEY (plan_id) REFERENCES plans(id)
);

CREATE INDEX idx_plan_feature_codes_plan_id ON plan_feature_codes(plan_id);
CREATE INDEX idx_plan_feature_codes_deleted ON plan_feature_codes(deleted);
CREATE INDEX idx_plan_feature_codes_plan_id_deleted ON plan_feature_codes(plan_id, deleted);
CREATE INDEX idx_plan_feature_codes_plan_feature ON plan_feature_codes(plan_id, feature_code);
CREATE UNIQUE INDEX idx_plan_feature_codes_unique ON plan_feature_codes(plan_id, feature_code) WHERE deleted = FALSE;

-- =====================================================
-- 22. AUDIT_LOGS - Audit log cho các thay đổi trong hệ thống
-- =====================================================
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT NOT NULL,
    action VARCHAR(20) NOT NULL,
    user_id BIGINT NOT NULL,
    user_name VARCHAR(255) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    before_value TEXT,
    after_value TEXT,
    description VARCHAR(500),
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_company_id ON audit_logs(company_id);
CREATE INDEX idx_audit_entity_type ON audit_logs(entity_type);
CREATE INDEX idx_audit_entity_id ON audit_logs(entity_id);
CREATE INDEX idx_audit_action ON audit_logs(action);
CREATE INDEX idx_audit_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_timestamp ON audit_logs(timestamp);
CREATE INDEX idx_audit_deleted ON audit_logs(deleted);
CREATE INDEX idx_audit_company_entity ON audit_logs(company_id, entity_type);
CREATE INDEX idx_audit_entity_type_id ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_company_timestamp ON audit_logs(company_id, timestamp DESC);

-- =====================================================
-- 23. FLEXIBLE WORKFORCE MANAGEMENT
-- Features: Shift Management, Individual Allowances/Deductions,
--           Payroll Period Workflow, Employment Contracts
-- =====================================================

-- =====================================================
-- 23.1. ENUMS - Các enum types cho Flexible Workforce
-- =====================================================

-- Trạng thái phân ca
DO $$ BEGIN
    CREATE TYPE shift_assignment_status AS ENUM ('SCHEDULED', 'COMPLETED', 'SWAPPED', 'CANCELLED');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Trạng thái yêu cầu đổi ca
DO $$ BEGIN
    CREATE TYPE swap_request_status AS ENUM ('PENDING', 'APPROVED', 'REJECTED');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Trạng thái kỳ lương
DO $$ BEGIN
    CREATE TYPE payroll_period_status AS ENUM ('DRAFT', 'REVIEWING', 'APPROVED', 'PAID');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Trạng thái chi tiết lương
DO $$ BEGIN
    CREATE TYPE payroll_item_status AS ENUM ('CALCULATED', 'ADJUSTED', 'CONFIRMED');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Loại hợp đồng
DO $$ BEGIN
    CREATE TYPE contract_type AS ENUM ('FULL_TIME', 'PART_TIME', 'SEASONAL', 'CONTRACT');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Trạng thái hợp đồng
DO $$ BEGIN
    CREATE TYPE contract_status AS ENUM ('ACTIVE', 'EXPIRED', 'TERMINATED');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- =====================================================
-- 23.2. SHIFT TEMPLATES - Mẫu ca làm việc
-- =====================================================
CREATE TABLE IF NOT EXISTS shift_templates (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    break_minutes INTEGER,
    multiplier DECIMAL(5,2),
    description VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_shift_template_company_id ON shift_templates(company_id);
CREATE INDEX IF NOT EXISTS idx_shift_template_deleted ON shift_templates(deleted);
CREATE INDEX IF NOT EXISTS idx_shift_template_active ON shift_templates(company_id, is_active);

-- =====================================================
-- 23.3. SHIFT ASSIGNMENTS - Phân ca cho nhân viên
-- =====================================================
CREATE TABLE IF NOT EXISTS shift_assignments (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    shift_template_id BIGINT NOT NULL,
    work_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'SCHEDULED',
    swapped_with_employee_id BIGINT,
    swapped_from_assignment_id BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_shift_assign_employee_id ON shift_assignments(employee_id);
CREATE INDEX IF NOT EXISTS idx_shift_assign_company_id ON shift_assignments(company_id);
CREATE INDEX IF NOT EXISTS idx_shift_assign_work_date ON shift_assignments(work_date);
CREATE INDEX IF NOT EXISTS idx_shift_assign_deleted ON shift_assignments(deleted);
CREATE INDEX IF NOT EXISTS idx_shift_assign_employee_date ON shift_assignments(employee_id, work_date);
CREATE INDEX IF NOT EXISTS idx_shift_assign_company_date ON shift_assignments(company_id, work_date);

-- =====================================================
-- 23.4. SHIFT SWAP REQUESTS - Yêu cầu đổi ca
-- =====================================================
CREATE TABLE IF NOT EXISTS shift_swap_requests (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL,
    requester_id BIGINT NOT NULL,
    target_employee_id BIGINT NOT NULL,
    requester_assignment_id BIGINT NOT NULL,
    target_assignment_id BIGINT NOT NULL,
    reason VARCHAR(500),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    approved_by BIGINT,
    approved_at TIMESTAMP,
    rejection_reason VARCHAR(500),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_swap_request_requester_id ON shift_swap_requests(requester_id);
CREATE INDEX IF NOT EXISTS idx_swap_request_target_id ON shift_swap_requests(target_employee_id);
CREATE INDEX IF NOT EXISTS idx_swap_request_company_id ON shift_swap_requests(company_id);
CREATE INDEX IF NOT EXISTS idx_swap_request_status ON shift_swap_requests(status);
CREATE INDEX IF NOT EXISTS idx_swap_request_deleted ON shift_swap_requests(deleted);
CREATE INDEX IF NOT EXISTS idx_swap_request_company_status ON shift_swap_requests(company_id, status);

-- =====================================================
-- 23.5. EMPLOYEE ALLOWANCES - Phụ cấp cá nhân
-- =====================================================
CREATE TABLE IF NOT EXISTS employee_allowances (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    allowance_code VARCHAR(50) NOT NULL,
    allowance_name VARCHAR(200) NOT NULL,
    allowance_type VARCHAR(50) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    taxable BOOLEAN NOT NULL DEFAULT TRUE,
    effective_from DATE NOT NULL,
    effective_to DATE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_emp_allowance_employee_id ON employee_allowances(employee_id);
CREATE INDEX IF NOT EXISTS idx_emp_allowance_company_id ON employee_allowances(company_id);
CREATE INDEX IF NOT EXISTS idx_emp_allowance_deleted ON employee_allowances(deleted);
CREATE INDEX IF NOT EXISTS idx_emp_allowance_active ON employee_allowances(employee_id, is_active);
CREATE INDEX IF NOT EXISTS idx_emp_allowance_effective ON employee_allowances(employee_id, effective_from, effective_to);

-- =====================================================
-- 23.6. EMPLOYEE DEDUCTIONS - Khấu trừ cá nhân
-- =====================================================
CREATE TABLE IF NOT EXISTS employee_deductions (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    deduction_code VARCHAR(50) NOT NULL,
    deduction_name VARCHAR(200) NOT NULL,
    deduction_type VARCHAR(50) NOT NULL,
    amount DECIMAL(15,2),
    percentage DECIMAL(5,2),
    effective_from DATE NOT NULL,
    effective_to DATE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_emp_deduction_employee_id ON employee_deductions(employee_id);
CREATE INDEX IF NOT EXISTS idx_emp_deduction_company_id ON employee_deductions(company_id);
CREATE INDEX IF NOT EXISTS idx_emp_deduction_deleted ON employee_deductions(deleted);
CREATE INDEX IF NOT EXISTS idx_emp_deduction_active ON employee_deductions(employee_id, is_active);
CREATE INDEX IF NOT EXISTS idx_emp_deduction_effective ON employee_deductions(employee_id, effective_from, effective_to);

-- =====================================================
-- 23.7. PAYROLL PERIODS - Kỳ lương
-- =====================================================
CREATE TABLE IF NOT EXISTS payroll_periods (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    year INTEGER NOT NULL,
    month INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    created_by BIGINT NOT NULL,
    approved_by BIGINT,
    approved_at TIMESTAMP,
    paid_at TIMESTAMP,
    payment_reference VARCHAR(100),
    total_gross_salary DECIMAL(15,2),
    total_net_salary DECIMAL(15,2),
    total_employees INTEGER,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_payroll_period_company_id ON payroll_periods(company_id);
CREATE INDEX IF NOT EXISTS idx_payroll_period_deleted ON payroll_periods(deleted);
CREATE INDEX IF NOT EXISTS idx_payroll_period_status ON payroll_periods(status);
CREATE INDEX IF NOT EXISTS idx_payroll_period_year_month ON payroll_periods(company_id, year, month);
CREATE INDEX IF NOT EXISTS idx_payroll_period_company_status ON payroll_periods(company_id, status);

-- =====================================================
-- 23.8. PAYROLL ITEMS - Chi tiết lương nhân viên
-- =====================================================
CREATE TABLE IF NOT EXISTS payroll_items (
    id BIGSERIAL PRIMARY KEY,
    payroll_period_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    salary_type VARCHAR(20) NOT NULL,
    base_salary DECIMAL(15,2),
    calculated_base_salary DECIMAL(15,2),
    working_days INTEGER,
    working_hours INTEGER,
    working_minutes INTEGER,
    regular_overtime_minutes INTEGER,
    night_overtime_minutes INTEGER,
    holiday_overtime_minutes INTEGER,
    weekend_overtime_minutes INTEGER,
    total_overtime_pay DECIMAL(15,2),
    total_break_minutes INTEGER,
    break_type VARCHAR(20),
    break_deduction_amount DECIMAL(15,2),
    allowance_details JSONB,
    total_allowances DECIMAL(15,2),
    deduction_details JSONB,
    total_deductions DECIMAL(15,2),
    gross_salary DECIMAL(15,2),
    net_salary DECIMAL(15,2),
    adjustment_amount DECIMAL(15,2),
    adjustment_reason VARCHAR(500),
    adjusted_by BIGINT,
    adjusted_at TIMESTAMP,
    status VARCHAR(50) NOT NULL DEFAULT 'CALCULATED',
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_payroll_item_period_id ON payroll_items(payroll_period_id);
CREATE INDEX IF NOT EXISTS idx_payroll_item_employee_id ON payroll_items(employee_id);
CREATE INDEX IF NOT EXISTS idx_payroll_item_company_id ON payroll_items(company_id);
CREATE INDEX IF NOT EXISTS idx_payroll_item_deleted ON payroll_items(deleted);
CREATE INDEX IF NOT EXISTS idx_payroll_item_period_employee ON payroll_items(payroll_period_id, employee_id);

-- =====================================================
-- 23.9. EMPLOYMENT CONTRACTS - Hợp đồng lao động
-- =====================================================
CREATE TABLE IF NOT EXISTS employment_contracts (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    contract_type VARCHAR(50) NOT NULL,
    contract_number VARCHAR(100),
    start_date DATE NOT NULL,
    end_date DATE,
    salary_config_id BIGINT,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    termination_reason VARCHAR(500),
    terminated_at DATE,
    notes VARCHAR(1000),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_contract_employee_id ON employment_contracts(employee_id);
CREATE INDEX IF NOT EXISTS idx_contract_company_id ON employment_contracts(company_id);
CREATE INDEX IF NOT EXISTS idx_contract_deleted ON employment_contracts(deleted);
CREATE INDEX IF NOT EXISTS idx_contract_status ON employment_contracts(status);
CREATE INDEX IF NOT EXISTS idx_contract_end_date ON employment_contracts(end_date);
CREATE INDEX IF NOT EXISTS idx_contract_employee_status ON employment_contracts(employee_id, status);
CREATE INDEX IF NOT EXISTS idx_contract_company_status ON employment_contracts(company_id, status);

-- =====================================================
-- COMMENTS - Mô tả các bảng Flexible Workforce
-- =====================================================
COMMENT ON TABLE shift_templates IS 'Mẫu ca làm việc của công ty';
COMMENT ON TABLE shift_assignments IS 'Phân ca làm việc cho nhân viên';
COMMENT ON TABLE shift_swap_requests IS 'Yêu cầu đổi ca giữa các nhân viên';
COMMENT ON TABLE employee_allowances IS 'Phụ cấp cá nhân của nhân viên';
COMMENT ON TABLE employee_deductions IS 'Khấu trừ cá nhân của nhân viên';
COMMENT ON TABLE payroll_periods IS 'Kỳ lương với workflow DRAFT → REVIEWING → APPROVED → PAID';
COMMENT ON TABLE payroll_items IS 'Chi tiết lương của từng nhân viên trong kỳ lương';
COMMENT ON TABLE employment_contracts IS 'Hợp đồng lao động của nhân viên';

-- =====================================================
-- 24. WORK MODE CHANGE LOGS - Audit log thay đổi work mode
-- =====================================================
CREATE TABLE IF NOT EXISTS work_mode_change_logs (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL,
    previous_mode VARCHAR(20) NOT NULL,
    new_mode VARCHAR(20) NOT NULL,
    changed_by VARCHAR(255) NOT NULL,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reason VARCHAR(500),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_work_mode_change_logs_company_id ON work_mode_change_logs(company_id);
CREATE INDEX IF NOT EXISTS idx_work_mode_change_logs_deleted ON work_mode_change_logs(deleted);
CREATE INDEX IF NOT EXISTS idx_work_mode_change_logs_changed_at ON work_mode_change_logs(changed_at DESC);

COMMENT ON TABLE work_mode_change_logs IS 'Audit log cho các thay đổi work mode của công ty';
