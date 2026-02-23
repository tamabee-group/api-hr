-- =====================================================
-- DEPARTMENTS - Phòng ban trong công ty
-- =====================================================
CREATE TABLE departments (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL,
    description TEXT,
    parent_id BIGINT,
    manager_id BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50)
);

CREATE INDEX idx_departments_parent_id ON departments(parent_id);
CREATE INDEX idx_departments_manager_id ON departments(manager_id);
CREATE INDEX idx_departments_deleted ON departments(deleted);
CREATE INDEX idx_departments_code ON departments(code);

-- =====================================================
-- USERS & USER PROFILES - Người dùng trong tenant
-- Không cần company_id vì mỗi tenant DB đại diện cho 1 company
-- =====================================================
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    employee_code VARCHAR(10) UNIQUE,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    language VARCHAR(10) NOT NULL,
    tenant_domain VARCHAR(50),
    profile_completeness INTEGER NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_employee_code ON users(employee_code);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_tenant_domain ON users(tenant_domain);
CREATE INDEX idx_users_deleted ON users(deleted);

CREATE TABLE user_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    name VARCHAR(255),
    phone VARCHAR(50),
    address TEXT,
    zip_code VARCHAR(20),
    date_of_birth DATE,
    gender VARCHAR(20),
    avatar VARCHAR(500),
    referral_code VARCHAR(10) UNIQUE,
    -- Basic info
    nationality VARCHAR(100),
    marital_status VARCHAR(50),
    national_id VARCHAR(50),
    -- Work info
    job_title VARCHAR(255),
    department_id BIGINT,
    employment_type VARCHAR(50),
    joining_date DATE,
    work_location VARCHAR(255),
    -- Bank info - Common
    bank_account_type VARCHAR(10) DEFAULT 'VN',
    japan_bank_type VARCHAR(10) DEFAULT 'normal',
    bank_name VARCHAR(255),
    bank_account VARCHAR(255),
    bank_account_name VARCHAR(255),
    bank_code VARCHAR(10),
    bank_branch_code VARCHAR(10),
    bank_branch_name VARCHAR(255),
    bank_account_category VARCHAR(20),
    bank_symbol VARCHAR(10),
    bank_number VARCHAR(15),
    -- Emergency contact
    emergency_contact_name VARCHAR(255),
    emergency_contact_phone VARCHAR(50),
    emergency_contact_relation VARCHAR(100),
    emergency_contact_address TEXT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_profiles_user FOREIGN KEY (user_id) REFERENCES users(id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE INDEX idx_user_profiles_user_id ON user_profiles(user_id);
CREATE INDEX idx_user_profiles_referral_code ON user_profiles(referral_code);
CREATE INDEX idx_user_profiles_bank_type ON user_profiles(bank_account_type);
CREATE INDEX idx_user_profiles_department_id ON user_profiles(department_id);
CREATE INDEX idx_user_profiles_deleted ON user_profiles(deleted);

-- =====================================================
-- COMPANY SETTINGS - Cấu hình công ty (không cần company_id)
-- Các config đã migrate sang bảng riêng bên dưới
-- =====================================================
CREATE TABLE company_settings (
    id BIGSERIAL PRIMARY KEY,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50)
);

CREATE INDEX idx_company_settings_deleted ON company_settings(deleted);

-- =====================================================
-- ATTENDANCE SETTINGS - Cấu hình chấm công
-- =====================================================
CREATE TABLE attendance_settings (
    id BIGSERIAL PRIMARY KEY,
    default_work_start_time TIME DEFAULT '09:00',
    default_work_end_time TIME DEFAULT '18:00',
    default_break_minutes INTEGER DEFAULT 60,
    enable_rounding BOOLEAN DEFAULT FALSE,
    enable_check_in_rounding BOOLEAN DEFAULT FALSE,
    enable_check_out_rounding BOOLEAN DEFAULT FALSE,
    enable_break_start_rounding BOOLEAN DEFAULT FALSE,
    enable_break_end_rounding BOOLEAN DEFAULT FALSE,
    check_in_rounding_interval VARCHAR(20),
    check_in_rounding_direction VARCHAR(20),
    check_out_rounding_interval VARCHAR(20),
    check_out_rounding_direction VARCHAR(20),
    break_start_rounding_interval VARCHAR(20),
    break_start_rounding_direction VARCHAR(20),
    break_end_rounding_interval VARCHAR(20),
    break_end_rounding_direction VARCHAR(20),
    late_grace_minutes INTEGER DEFAULT 0,
    early_leave_grace_minutes INTEGER DEFAULT 0,
    require_geo_location BOOLEAN DEFAULT FALSE,
    geo_fence_radius_meters INTEGER DEFAULT 500,
    allow_web_check_in BOOLEAN DEFAULT TRUE,
    saturday_off BOOLEAN DEFAULT TRUE,
    sunday_off BOOLEAN DEFAULT TRUE,
    holiday_off BOOLEAN DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50)
);

CREATE INDEX idx_attendance_settings_deleted ON attendance_settings(deleted);

-- =====================================================
-- BREAK SETTINGS - Cấu hình giờ giải lao
-- =====================================================
CREATE TABLE break_settings (
    id BIGSERIAL PRIMARY KEY,
    break_enabled BOOLEAN DEFAULT TRUE,
    break_type VARCHAR(20) DEFAULT 'UNPAID',
    default_break_minutes INTEGER DEFAULT 60,
    minimum_break_minutes INTEGER DEFAULT 45,
    maximum_break_minutes INTEGER DEFAULT 90,
    use_legal_minimum BOOLEAN DEFAULT TRUE,
    region VARCHAR(10) DEFAULT 'ja',
    fixed_break_mode BOOLEAN DEFAULT FALSE,
    break_periods_per_attendance INTEGER DEFAULT 1,
    max_breaks_per_day INTEGER DEFAULT 3,
    fixed_break_periods JSONB DEFAULT '[]',
    night_shift_start_time TIME DEFAULT '22:00',
    night_shift_end_time TIME DEFAULT '05:00',
    night_shift_minimum_break_minutes INTEGER DEFAULT 45,
    night_shift_default_break_minutes INTEGER DEFAULT 60,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50)
);

CREATE INDEX idx_break_settings_deleted ON break_settings(deleted);

-- =====================================================
-- PAYROLL SETTINGS - Cấu hình tính lương
-- =====================================================
CREATE TABLE payroll_settings (
    id BIGSERIAL PRIMARY KEY,
    default_salary_type VARCHAR(20) DEFAULT 'MONTHLY',
    pay_day INTEGER DEFAULT 25,
    cutoff_day INTEGER DEFAULT 20,
    salary_rounding VARCHAR(20) DEFAULT 'NEAREST',
    standard_working_days_per_month INTEGER DEFAULT 22,
    standard_working_hours_per_day INTEGER DEFAULT 8,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50)
);

CREATE INDEX idx_payroll_settings_deleted ON payroll_settings(deleted);

-- =====================================================
-- OVERTIME SETTINGS - Cấu hình tăng ca
-- =====================================================
CREATE TABLE overtime_settings (
    id BIGSERIAL PRIMARY KEY,
    overtime_enabled BOOLEAN DEFAULT TRUE,
    standard_working_hours INTEGER DEFAULT 8,
    night_start_time TIME DEFAULT '22:00',
    night_end_time TIME DEFAULT '05:00',
    regular_overtime_rate DECIMAL(5,2) DEFAULT 1.25,
    night_work_rate DECIMAL(5,2) DEFAULT 1.25,
    night_overtime_rate DECIMAL(5,2) DEFAULT 1.50,
    holiday_overtime_rate DECIMAL(5,2) DEFAULT 1.35,
    holiday_night_overtime_rate DECIMAL(5,2) DEFAULT 1.60,
    weekend_overtime_rate DECIMAL(5,2) DEFAULT 1.35,
    use_legal_minimum BOOLEAN DEFAULT TRUE,
    region VARCHAR(10) DEFAULT 'ja',
    max_overtime_hours_per_day INTEGER DEFAULT 4,
    max_overtime_hours_per_month INTEGER DEFAULT 45,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50)
);

CREATE INDEX idx_overtime_settings_deleted ON overtime_settings(deleted);

-- =====================================================
-- ATTENDANCE LOCATIONS - Vị trí chấm công
-- =====================================================
CREATE TABLE attendance_locations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    address VARCHAR(500),
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    radius_meters INTEGER NOT NULL DEFAULT 100,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50)
);

CREATE INDEX idx_attendance_locations_deleted ON attendance_locations(deleted);
CREATE INDEX idx_attendance_locations_active ON attendance_locations(is_active);

CREATE TABLE shift_templates (
    id BIGSERIAL PRIMARY KEY,
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

CREATE INDEX idx_shift_template_deleted ON shift_templates(deleted);
CREATE INDEX idx_shift_template_active ON shift_templates(is_active);

CREATE TABLE shift_assignments (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    shift_template_id BIGINT NOT NULL,
    work_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'SCHEDULED',
    swapped_with_employee_id BIGINT,
    swapped_from_assignment_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_shift_assignments_employee FOREIGN KEY (employee_id) REFERENCES users(id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_shift_assignments_template FOREIGN KEY (shift_template_id) REFERENCES shift_templates(id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE INDEX idx_shift_assign_employee_id ON shift_assignments(employee_id);
CREATE INDEX idx_shift_assign_work_date ON shift_assignments(work_date);
CREATE INDEX idx_shift_assign_employee_date ON shift_assignments(employee_id, work_date);

CREATE TABLE shift_swap_requests (
    id BIGSERIAL PRIMARY KEY,
    requester_id BIGINT NOT NULL,
    target_employee_id BIGINT NOT NULL,
    requester_assignment_id BIGINT NOT NULL,
    target_assignment_id BIGINT NOT NULL,
    reason VARCHAR(500),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    approved_by BIGINT,
    approved_at TIMESTAMP,
    rejection_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_shift_swap_requester FOREIGN KEY (requester_id) REFERENCES users(id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_shift_swap_target FOREIGN KEY (target_employee_id) REFERENCES users(id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_shift_swap_requester_assignment FOREIGN KEY (requester_assignment_id) REFERENCES shift_assignments(id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_shift_swap_target_assignment FOREIGN KEY (target_assignment_id) REFERENCES shift_assignments(id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE INDEX idx_swap_request_requester_id ON shift_swap_requests(requester_id);
CREATE INDEX idx_swap_request_target_id ON shift_swap_requests(target_employee_id);
CREATE INDEX idx_swap_request_status ON shift_swap_requests(status);

CREATE TABLE attendance_records (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    work_date DATE NOT NULL,
    original_check_in TIMESTAMP,
    original_check_out TIMESTAMP,
    rounded_check_in TIMESTAMP,
    rounded_check_out TIMESTAMP,
    working_minutes INTEGER DEFAULT 0,
    overtime_minutes INTEGER DEFAULT 0,
    late_minutes INTEGER DEFAULT 0,
    early_leave_minutes INTEGER DEFAULT 0,
    total_break_minutes INTEGER DEFAULT 0,
    effective_break_minutes INTEGER DEFAULT 0,
    break_type VARCHAR(20),
    break_compliant BOOLEAN,
    status VARCHAR(50) NOT NULL DEFAULT 'PRESENT',
    check_in_latitude DOUBLE PRECISION,
    check_in_longitude DOUBLE PRECISION,
    check_out_latitude DOUBLE PRECISION,
    check_out_longitude DOUBLE PRECISION,
    check_in_out_of_range BOOLEAN DEFAULT FALSE,
    check_out_out_of_range BOOLEAN DEFAULT FALSE,
    check_in_source VARCHAR(20) DEFAULT 'WEB',
    kiosk_id BIGINT,
    adjustment_reason VARCHAR(500),
    adjusted_by BIGINT,
    adjusted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    CONSTRAINT fk_attendance_records_employee FOREIGN KEY (employee_id) REFERENCES users(id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE INDEX idx_attendance_records_employee_id ON attendance_records(employee_id);
CREATE INDEX idx_attendance_records_work_date ON attendance_records(work_date);
CREATE INDEX idx_attendance_records_status ON attendance_records(status);
CREATE UNIQUE INDEX idx_attendance_records_employee_date ON attendance_records(employee_id, work_date);


CREATE TABLE break_records (
    id BIGSERIAL PRIMARY KEY,
    attendance_record_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    work_date DATE NOT NULL,
    break_number INTEGER NOT NULL DEFAULT 1,
    break_start TIMESTAMP,
    break_end TIMESTAMP,
    actual_break_minutes INTEGER DEFAULT 0,
    effective_break_minutes INTEGER DEFAULT 0,
    notes VARCHAR(500),
    break_start_latitude DOUBLE PRECISION,
    break_start_longitude DOUBLE PRECISION,
    break_end_latitude DOUBLE PRECISION,
    break_end_longitude DOUBLE PRECISION,
    break_start_out_of_range BOOLEAN DEFAULT FALSE,
    break_end_out_of_range BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    CONSTRAINT fk_break_records_attendance FOREIGN KEY (attendance_record_id) REFERENCES attendance_records(id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_break_records_employee FOREIGN KEY (employee_id) REFERENCES users(id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE INDEX idx_break_records_attendance_id ON break_records(attendance_record_id);
CREATE INDEX idx_break_records_employee_id ON break_records(employee_id);
CREATE INDEX idx_break_records_work_date ON break_records(work_date);
CREATE INDEX idx_break_records_employee_date ON break_records(employee_id, work_date);

CREATE TABLE attendance_adjustment_requests (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    attendance_record_id BIGINT,
    work_date DATE,
    request_type VARCHAR(50) NOT NULL DEFAULT 'ADJUST',
    break_record_id BIGINT,
    assigned_to BIGINT,
    original_check_in TIMESTAMP,
    original_check_out TIMESTAMP,
    original_break_start TIMESTAMP,
    original_break_end TIMESTAMP,
    requested_check_in TIMESTAMP,
    requested_check_out TIMESTAMP,
    requested_break_start TIMESTAMP,
    requested_break_end TIMESTAMP,
    reason VARCHAR(500),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    approved_by BIGINT,
    approved_at TIMESTAMP,
    approver_comment VARCHAR(500),
    rejection_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    CONSTRAINT fk_adjustment_requests_employee FOREIGN KEY (employee_id) REFERENCES users(id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_adjustment_requests_attendance FOREIGN KEY (attendance_record_id) REFERENCES attendance_records(id) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_adjustment_requests_break FOREIGN KEY (break_record_id) REFERENCES break_records(id) ON UPDATE CASCADE ON DELETE SET NULL
);

CREATE INDEX idx_adjustment_requests_employee_id ON attendance_adjustment_requests(employee_id);
CREATE INDEX idx_adjustment_requests_attendance_record_id ON attendance_adjustment_requests(attendance_record_id);
CREATE INDEX idx_adjustment_requests_work_date ON attendance_adjustment_requests(work_date);
CREATE INDEX idx_adjustment_requests_break_record_id ON attendance_adjustment_requests(break_record_id);
CREATE INDEX idx_adjustment_requests_assigned_to ON attendance_adjustment_requests(assigned_to);
CREATE INDEX idx_adjustment_requests_status ON attendance_adjustment_requests(status);
CREATE INDEX idx_adjustment_requests_request_type ON attendance_adjustment_requests(request_type);

-- =====================================================
-- ADJUSTMENT_BREAK_ITEMS - Chi tiết điều chỉnh break trong yêu cầu
-- =====================================================
CREATE TABLE adjustment_break_items (
    id BIGSERIAL PRIMARY KEY,
    adjustment_request_id BIGINT NOT NULL,
    break_record_id BIGINT,  -- NULL khi actionType = CREATE (tạo mới break)
    break_number INTEGER,
    action_type VARCHAR(20) NOT NULL DEFAULT 'ADJUST',
    original_break_start TIMESTAMP,
    original_break_end TIMESTAMP,
    requested_break_start TIMESTAMP,
    requested_break_end TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_adjustment_break_items_request FOREIGN KEY (adjustment_request_id) REFERENCES attendance_adjustment_requests(id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE INDEX idx_adjustment_break_items_request_id ON adjustment_break_items(adjustment_request_id);
CREATE INDEX idx_adjustment_break_items_break_record_id ON adjustment_break_items(break_record_id);

CREATE TABLE holidays (
    id BIGSERIAL PRIMARY KEY,
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
CREATE INDEX idx_holidays_date ON holidays(date);
CREATE INDEX idx_holidays_type ON holidays(type);
CREATE UNIQUE INDEX idx_holidays_date_name ON holidays(date, name) WHERE deleted = false;

CREATE TABLE leave_requests (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    leave_type VARCHAR(50) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    total_days INTEGER,
    reason VARCHAR(500),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    approved_by BIGINT,
    approved_at TIMESTAMP,
    rejection_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    CONSTRAINT fk_leave_requests_employee FOREIGN KEY (employee_id) REFERENCES users(id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE INDEX idx_leave_requests_employee_id ON leave_requests(employee_id);
CREATE INDEX idx_leave_requests_leave_type ON leave_requests(leave_type);
CREATE INDEX idx_leave_requests_status ON leave_requests(status);
CREATE INDEX idx_leave_requests_start_date ON leave_requests(start_date);
CREATE INDEX idx_leave_requests_end_date ON leave_requests(end_date);

CREATE TABLE leave_balances (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    year INTEGER NOT NULL,
    leave_type VARCHAR(50) NOT NULL,
    total_days INTEGER NOT NULL DEFAULT 0,
    used_days INTEGER NOT NULL DEFAULT 0,
    remaining_days INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    CONSTRAINT fk_leave_balances_employee FOREIGN KEY (employee_id) REFERENCES users(id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE INDEX idx_leave_balances_employee_id ON leave_balances(employee_id);
CREATE INDEX idx_leave_balances_year ON leave_balances(year);
CREATE INDEX idx_leave_balances_leave_type ON leave_balances(leave_type);
CREATE INDEX idx_leave_balances_employee_year ON leave_balances(employee_id, year);

CREATE TABLE employee_salaries (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    salary_type VARCHAR(50) NOT NULL,
    monthly_salary DECIMAL(15, 2),
    daily_rate DECIMAL(15, 2),
    hourly_rate DECIMAL(15, 2),
    shift_rate DECIMAL(15, 2),
    effective_from DATE NOT NULL,
    effective_to DATE,
    used_in_payroll BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    note VARCHAR(500),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    CONSTRAINT fk_employee_salaries_employee FOREIGN KEY (employee_id) REFERENCES users(id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE INDEX idx_emp_salary_deleted ON employee_salaries(deleted);
CREATE INDEX idx_emp_salary_employee_id ON employee_salaries(employee_id);
CREATE INDEX idx_emp_salary_effective ON employee_salaries(employee_id, effective_from);
CREATE INDEX idx_emp_salary_active ON employee_salaries(employee_id, active);

-- =====================================================
-- SALARY ITEM TEMPLATES - Template phụ cấp/khấu trừ
-- =====================================================
CREATE TABLE salary_item_templates (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    type VARCHAR(20) NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100)
);

CREATE INDEX idx_salary_item_template_type ON salary_item_templates(type);
CREATE INDEX idx_salary_item_template_deleted ON salary_item_templates(deleted);

-- =====================================================
-- EMPLOYEE SALARY ITEMS - Phụ cấp/khấu trừ của nhân viên
-- =====================================================
CREATE TABLE employee_salary_items (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    template_id BIGINT NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    CONSTRAINT fk_employee_salary_items_employee FOREIGN KEY (employee_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_employee_salary_items_template FOREIGN KEY (template_id) REFERENCES salary_item_templates(id)
);

CREATE INDEX idx_employee_salary_item_employee ON employee_salary_items(employee_id);
CREATE INDEX idx_employee_salary_item_template ON employee_salary_items(template_id);
CREATE INDEX idx_employee_salary_item_deleted ON employee_salary_items(deleted);

CREATE TABLE payroll_periods (
    id BIGSERIAL PRIMARY KEY,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    year INTEGER NOT NULL,
    month INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    created_by BIGINT NOT NULL,
    rejection_reason VARCHAR(500),
    submitted_by BIGINT,
    approved_by BIGINT,
    approved_at TIMESTAMP,
    paid_at TIMESTAMP,
    payment_reference VARCHAR(100),
    total_gross_salary DECIMAL(15,2),
    total_net_salary DECIMAL(15,2),
    total_employees INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payroll_period_status ON payroll_periods(status);
CREATE INDEX idx_payroll_period_year_month ON payroll_periods(year, month);

CREATE TABLE payroll_items (
    id BIGSERIAL PRIMARY KEY,
    payroll_period_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
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
    -- Commission fields for Tamabee employees
    commission_amount DECIMAL(15,2) DEFAULT 0,
    commission_details JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payroll_items_period FOREIGN KEY (payroll_period_id) REFERENCES payroll_periods(id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_payroll_items_employee FOREIGN KEY (employee_id) REFERENCES users(id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE INDEX idx_payroll_item_period_id ON payroll_items(payroll_period_id);
CREATE INDEX idx_payroll_item_employee_id ON payroll_items(employee_id);
CREATE INDEX idx_payroll_item_period_employee ON payroll_items(payroll_period_id, employee_id);

CREATE TABLE employment_contracts (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
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
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_employment_contracts_employee FOREIGN KEY (employee_id) REFERENCES users(id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_employment_contracts_salary FOREIGN KEY (salary_config_id) REFERENCES employee_salaries(id) ON UPDATE CASCADE ON DELETE SET NULL
);

CREATE INDEX idx_contract_employee_id ON employment_contracts(employee_id);
CREATE INDEX idx_contract_deleted ON employment_contracts(deleted);
CREATE INDEX idx_contract_status ON employment_contracts(status);
CREATE INDEX idx_contract_end_date ON employment_contracts(end_date);
CREATE INDEX idx_contract_employee_status ON employment_contracts(employee_id, status);

CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
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
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_entity_type ON audit_logs(entity_type);
CREATE INDEX idx_audit_entity_id ON audit_logs(entity_id);
CREATE INDEX idx_audit_action ON audit_logs(action);
CREATE INDEX idx_audit_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_timestamp ON audit_logs(timestamp);
CREATE INDEX idx_audit_entity_type_id ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_timestamp_desc ON audit_logs(timestamp DESC);


-- =====================================================
-- EMAIL VERIFICATIONS - Xác thực email (forgot password)
-- =====================================================
CREATE TABLE email_verifications (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    code VARCHAR(64) NOT NULL,
    company_name VARCHAR(255),
    expired_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_email_verifications_email ON email_verifications(email);
CREATE INDEX idx_email_verifications_expired ON email_verifications(expired_at);

-- =====================================================
-- EMPLOYEE DOCUMENTS - Tài liệu nhân viên
-- Không có soft delete (data lớn, xóa thẳng)
-- =====================================================
CREATE TABLE employee_documents (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    file_type VARCHAR(20) NOT NULL,
    file_size BIGINT NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    CONSTRAINT fk_employee_documents_employee FOREIGN KEY (employee_id) REFERENCES users(id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE INDEX idx_employee_documents_employee_id ON employee_documents(employee_id);
CREATE INDEX idx_employee_documents_document_type ON employee_documents(document_type);


-- =====================================================
-- NOTIFICATIONS - Thông báo cho người dùng
-- Không có soft delete (data lớn, xóa thẳng)
-- =====================================================
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    code VARCHAR(50) NOT NULL,
    params TEXT,
    target_url VARCHAR(255),
    type VARCHAR(20) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    title VARCHAR(255),
    content TEXT,
    system_notification_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_user_read ON notifications(user_id, is_read);
CREATE INDEX idx_notifications_created_at ON notifications(created_at DESC);

-- =====================================================
-- SHIFT PREFERENCES - Nguyện vọng ca làm việc của nhân viên
-- Cho phép employee gửi nguyện vọng ca theo tuần,
-- hỗ trợ chọn shift template có sẵn hoặc nhập custom time.
-- =====================================================
CREATE TABLE shift_preferences (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    year INTEGER NOT NULL,
    week_number INTEGER NOT NULL,
    day_of_week INTEGER NOT NULL, -- 1=Monday..7=Sunday (ISO 8601)
    shift_template_id BIGINT,     -- NULL nếu custom time
    custom_start_time TIME,        -- NULL nếu chọn template
    custom_end_time TIME,          -- NULL nếu chọn template
    reason TEXT,
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL', -- NORMAL, HIGH (có reason)
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING, APPLIED, EXPIRED
    applied_assignment_id BIGINT,  -- ID assignment tạo khi apply (dùng cho undo)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_preference_employee FOREIGN KEY (employee_id) REFERENCES users(id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_preference_template FOREIGN KEY (shift_template_id) REFERENCES shift_templates(id) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT chk_preference_shift CHECK (
        (shift_template_id IS NOT NULL AND custom_start_time IS NULL AND custom_end_time IS NULL) OR
        (shift_template_id IS NULL AND custom_start_time IS NOT NULL AND custom_end_time IS NOT NULL)
    )
);

CREATE INDEX idx_preference_employee ON shift_preferences(employee_id);
CREATE INDEX idx_preference_week ON shift_preferences(year, week_number);
CREATE INDEX idx_preference_employee_week ON shift_preferences(employee_id, year, week_number);
CREATE INDEX idx_preference_status ON shift_preferences(status);


-- =====================================================
-- ATTENDANCE KIOSKS - Máy chấm công cố định
-- =====================================================
CREATE TABLE attendance_kiosks (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    pin_code VARCHAR(10) NOT NULL,
    location_id BIGINT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_active_at TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    CONSTRAINT fk_kiosk_location FOREIGN KEY (location_id) REFERENCES attendance_locations(id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE INDEX idx_kiosk_deleted ON attendance_kiosks(deleted);
CREATE INDEX idx_kiosk_active ON attendance_kiosks(is_active);
CREATE INDEX idx_kiosk_pin_code ON attendance_kiosks(pin_code);
