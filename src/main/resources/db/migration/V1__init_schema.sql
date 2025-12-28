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
    plan_id BIGINT,
    referred_by_employee_id BIGINT,
    owner_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_companies_email ON companies(email);
CREATE INDEX idx_companies_plan_id ON companies(plan_id);
CREATE INDEX idx_companies_referred_by ON companies(referred_by_employee_id);
CREATE INDEX idx_companies_owner_id ON companies(owner_id);
CREATE INDEX idx_companies_status ON companies(status);
CREATE INDEX idx_companies_deleted ON companies(deleted);

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
