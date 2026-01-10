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
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_plan_features_plan FOREIGN KEY (plan_id) REFERENCES plans(id)
);

CREATE INDEX idx_plan_features_plan_id ON plan_features(plan_id);
CREATE INDEX idx_plan_features_deleted ON plan_features(deleted);

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
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_companies_plan FOREIGN KEY (plan_id) REFERENCES plans(id)
);

CREATE UNIQUE INDEX idx_companies_email ON companies(email);
CREATE UNIQUE INDEX idx_companies_tenant_domain ON companies(tenant_domain);
CREATE INDEX idx_companies_plan_id ON companies(plan_id);
CREATE INDEX idx_companies_referred_by ON companies(referred_by_employee_id);
CREATE INDEX idx_companies_owner_id ON companies(owner_id);
CREATE INDEX idx_companies_status ON companies(status);
CREATE INDEX idx_companies_deleted ON companies(deleted);
CREATE INDEX idx_companies_deactivated_at ON companies(deactivated_at);

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
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wallets_company FOREIGN KEY (company_id) REFERENCES companies(id)
);

CREATE UNIQUE INDEX idx_wallets_company_id ON wallets(company_id);
CREATE INDEX idx_wallets_free_trial ON wallets(free_trial_end_date);
CREATE INDEX idx_wallets_deleted ON wallets(deleted);

CREATE TABLE wallet_transactions (
    id BIGSERIAL PRIMARY KEY,
    wallet_id BIGINT NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    amount DECIMAL(15,0) NOT NULL,
    balance_before DECIMAL(15,0) NOT NULL,
    balance_after DECIMAL(15,0) NOT NULL,
    description VARCHAR(500),
    reference_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wallet_transactions_wallet FOREIGN KEY (wallet_id) REFERENCES wallets(id)
);

CREATE INDEX idx_wallet_transactions_wallet_id ON wallet_transactions(wallet_id);
CREATE INDEX idx_wallet_transactions_type ON wallet_transactions(transaction_type);
CREATE INDEX idx_wallet_transactions_created_at ON wallet_transactions(created_at DESC);

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
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_deposit_requests_company FOREIGN KEY (company_id) REFERENCES companies(id)
);

CREATE INDEX idx_deposit_requests_company_id ON deposit_requests(company_id);
CREATE INDEX idx_deposit_requests_status ON deposit_requests(status);
CREATE INDEX idx_deposit_requests_deleted ON deposit_requests(deleted);
CREATE INDEX idx_deposit_requests_created_at ON deposit_requests(created_at DESC);

CREATE TABLE employee_commissions (
    id BIGSERIAL PRIMARY KEY,
    employee_code VARCHAR(50) NOT NULL,
    company_id BIGINT NOT NULL,
    amount DECIMAL(15,0) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    company_billing_at_creation DECIMAL(15,0) NOT NULL DEFAULT 0,
    paid_at TIMESTAMP,
    paid_by VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_employee_commissions_company FOREIGN KEY (company_id) REFERENCES companies(id)
);

CREATE INDEX idx_employee_commissions_employee_code ON employee_commissions(employee_code);
CREATE INDEX idx_employee_commissions_company_id ON employee_commissions(company_id);
CREATE INDEX idx_employee_commissions_status ON employee_commissions(status);
CREATE INDEX idx_employee_commissions_created_at ON employee_commissions(created_at DESC);

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

CREATE TABLE mail_history (
    id BIGSERIAL PRIMARY KEY,
    recipient_email VARCHAR(255) NOT NULL,
    subject VARCHAR(500) NOT NULL,
    template_name VARCHAR(100),
    status VARCHAR(50) NOT NULL DEFAULT 'SENT',
    error_message TEXT,
    sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_mail_history_recipient ON mail_history(recipient_email);
CREATE INDEX idx_mail_history_status ON mail_history(status);
CREATE INDEX idx_mail_history_sent_at ON mail_history(sent_at DESC);

INSERT INTO companies (id, name, owner_name, email, phone, address, industry, zipcode, locale, language, tenant_domain, status, deleted, created_at, updated_at)
VALUES (0, 'Tamabee', 'Tamabee Admin', 'admin@tamabee.vn', '0311111111', 'Tokyo', 'technology', '1000001', 'Asia/Tokyo', 'ja', 'tamabee', 'ACTIVE', false, NOW(), NOW());

SELECT setval('companies_id_seq', 1, false);
