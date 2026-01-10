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
    locale VARCHAR(50) NOT NULL,
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
    date_of_birth VARCHAR(20),
    gender VARCHAR(20),
    avatar VARCHAR(500),
    referral_code VARCHAR(10) UNIQUE,
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
    emergency_contact_name VARCHAR(255),
    emergency_contact_phone VARCHAR(50),
    emergency_contact_relation VARCHAR(100),
    emergency_contact_address TEXT,
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
-- COMPANY SETTINGS - Cấu hình công ty (không cần company_id)
-- =====================================================
CREATE TABLE company_settings (
    id BIGSERIAL PRIMARY KEY,
    attendance_config JSONB,
    payroll_config JSONB,
    overtime_config JSONB,
    allowance_config JSONB,
    deduction_config JSONB,
    break_config JSONB,
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

CREATE INDEX idx_company_settings_deleted ON company_settings(deleted);
CREATE INDEX idx_company_settings_work_mode ON company_settings(work_mode);

CREATE TABLE work_schedules (
    id BIGSERIAL PRIMARY KEY,
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
CREATE INDEX idx_work_schedules_type ON work_schedules(type);
CREATE INDEX idx_work_schedules_is_default ON work_schedules(is_default);
CREATE INDEX idx_work_schedules_is_active ON work_schedules(is_active);

CREATE TABLE work_schedule_assignments (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    schedule_id BIGINT NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50)
);

CREATE INDEX idx_work_schedule_assignments_employee_id ON work_schedule_assignments(employee_id);
CREATE INDEX idx_work_schedule_assignments_schedule_id ON work_schedule_assignments(schedule_id);
CREATE INDEX idx_work_schedule_assignments_effective_from ON work_schedule_assignments(effective_from);
CREATE INDEX idx_work_schedule_assignments_effective_to ON work_schedule_assignments(effective_to);

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
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
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
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
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
    check_in_device_id VARCHAR(255),
    check_out_device_id VARCHAR(255),
    check_in_latitude DOUBLE PRECISION,
    check_in_longitude DOUBLE PRECISION,
    check_out_latitude DOUBLE PRECISION,
    check_out_longitude DOUBLE PRECISION,
    adjustment_reason VARCHAR(500),
    adjusted_by BIGINT,
    adjusted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50)
);

CREATE INDEX idx_attendance_records_employee_id ON attendance_records(employee_id);
CREATE INDEX idx_attendance_records_work_date ON attendance_records(work_date);
CREATE INDEX idx_attendance_records_status ON attendance_records(status);
CREATE INDEX idx_attendance_records_employee_date ON attendance_records(employee_id, work_date);


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
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50)
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
    updated_by VARCHAR(50)
);

CREATE INDEX idx_adjustment_requests_employee_id ON attendance_adjustment_requests(employee_id);
CREATE INDEX idx_adjustment_requests_attendance_record_id ON attendance_adjustment_requests(attendance_record_id);
CREATE INDEX idx_adjustment_requests_work_date ON attendance_adjustment_requests(work_date);
CREATE INDEX idx_adjustment_requests_break_record_id ON attendance_adjustment_requests(break_record_id);
CREATE INDEX idx_adjustment_requests_assigned_to ON attendance_adjustment_requests(assigned_to);
CREATE INDEX idx_adjustment_requests_status ON attendance_adjustment_requests(status);

CREATE TABLE schedule_selections (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    schedule_id BIGINT NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    approved_by BIGINT,
    approved_at TIMESTAMP,
    rejection_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50)
);

CREATE INDEX idx_schedule_selections_employee_id ON schedule_selections(employee_id);
CREATE INDEX idx_schedule_selections_schedule_id ON schedule_selections(schedule_id);
CREATE INDEX idx_schedule_selections_status ON schedule_selections(status);

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
    updated_by VARCHAR(50)
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
    updated_by VARCHAR(50)
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
    note VARCHAR(500),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50)
);

CREATE INDEX idx_emp_salary_deleted ON employee_salaries(deleted);
CREATE INDEX idx_emp_salary_employee_id ON employee_salaries(employee_id);
CREATE INDEX idx_emp_salary_effective ON employee_salaries(employee_id, effective_from);

CREATE TABLE employee_allowances (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
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

CREATE INDEX idx_emp_allowance_employee_id ON employee_allowances(employee_id);
CREATE INDEX idx_emp_allowance_deleted ON employee_allowances(deleted);
CREATE INDEX idx_emp_allowance_active ON employee_allowances(employee_id, is_active);
CREATE INDEX idx_emp_allowance_effective ON employee_allowances(employee_id, effective_from, effective_to);

CREATE TABLE employee_deductions (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
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

CREATE INDEX idx_emp_deduction_employee_id ON employee_deductions(employee_id);
CREATE INDEX idx_emp_deduction_deleted ON employee_deductions(deleted);
CREATE INDEX idx_emp_deduction_active ON employee_deductions(employee_id, is_active);
CREATE INDEX idx_emp_deduction_effective ON employee_deductions(employee_id, effective_from, effective_to);

CREATE TABLE payroll_periods (
    id BIGSERIAL PRIMARY KEY,
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
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payroll_item_period_id ON payroll_items(payroll_period_id);
CREATE INDEX idx_payroll_item_employee_id ON payroll_items(employee_id);
CREATE INDEX idx_payroll_item_period_employee ON payroll_items(payroll_period_id, employee_id);

CREATE TABLE payroll_records (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    year INTEGER NOT NULL,
    month INTEGER NOT NULL,
    salary_type VARCHAR(50) NOT NULL,
    base_salary DECIMAL(15, 2) NOT NULL DEFAULT 0,
    working_days INTEGER,
    working_hours INTEGER,
    regular_overtime_pay DECIMAL(15, 2) DEFAULT 0,
    night_overtime_pay DECIMAL(15, 2) DEFAULT 0,
    holiday_overtime_pay DECIMAL(15, 2) DEFAULT 0,
    weekend_overtime_pay DECIMAL(15, 2) DEFAULT 0,
    total_overtime_pay DECIMAL(15, 2) DEFAULT 0,
    regular_overtime_hours INTEGER,
    night_overtime_hours INTEGER,
    holiday_overtime_hours INTEGER,
    weekend_overtime_hours INTEGER,
    allowance_details JSONB,
    total_allowances DECIMAL(15, 2) DEFAULT 0,
    deduction_details JSONB,
    total_deductions DECIMAL(15, 2) DEFAULT 0,
    total_break_minutes INTEGER DEFAULT 0,
    break_type VARCHAR(20),
    break_deduction_amount DECIMAL(15, 2) DEFAULT 0,
    gross_salary DECIMAL(15, 2) NOT NULL DEFAULT 0,
    net_salary DECIMAL(15, 2) NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    payment_status VARCHAR(50) DEFAULT 'PENDING',
    paid_at TIMESTAMP,
    payment_reference VARCHAR(255),
    notification_sent BOOLEAN NOT NULL DEFAULT FALSE,
    notification_sent_at TIMESTAMP,
    finalized_at TIMESTAMP,
    finalized_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50)
);

CREATE INDEX idx_payroll_records_employee_id ON payroll_records(employee_id);
CREATE INDEX idx_payroll_records_year ON payroll_records(year);
CREATE INDEX idx_payroll_records_month ON payroll_records(month);
CREATE INDEX idx_payroll_records_status ON payroll_records(status);
CREATE INDEX idx_payroll_records_payment_status ON payroll_records(payment_status);
CREATE INDEX idx_payroll_records_period ON payroll_records(year, month);
CREATE INDEX idx_payroll_records_employee_period ON payroll_records(employee_id, year, month);

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
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
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

CREATE TABLE work_mode_change_logs (
    id BIGSERIAL PRIMARY KEY,
    previous_mode VARCHAR(20) NOT NULL,
    new_mode VARCHAR(20) NOT NULL,
    changed_by VARCHAR(255) NOT NULL,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_work_mode_change_logs_changed_at ON work_mode_change_logs(changed_at DESC);
