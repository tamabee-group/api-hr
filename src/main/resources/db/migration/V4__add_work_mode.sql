-- =====================================================
-- TAMABEE HR - WORK MODE FEATURE
-- Thêm chế độ làm việc cho công ty (FIXED_HOURS / FLEXIBLE_SHIFT)
-- =====================================================

-- =====================================================
-- 1. Thêm columns work mode vào company_settings
-- =====================================================
ALTER TABLE company_settings 
ADD COLUMN IF NOT EXISTS work_mode VARCHAR(20) NOT NULL DEFAULT 'FLEXIBLE_SHIFT';

ALTER TABLE company_settings 
ADD COLUMN IF NOT EXISTS default_work_start_time TIME;

ALTER TABLE company_settings 
ADD COLUMN IF NOT EXISTS default_work_end_time TIME;

ALTER TABLE company_settings 
ADD COLUMN IF NOT EXISTS default_break_minutes INTEGER;

-- Index cho work_mode
CREATE INDEX IF NOT EXISTS idx_company_settings_work_mode ON company_settings(work_mode);

-- =====================================================
-- 2. Tạo bảng work_mode_change_logs để audit
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

-- =====================================================
-- 3. Thêm column is_active vào work_schedules
-- Dùng để đánh dấu schedules inactive khi switch sang FIXED_HOURS
-- =====================================================
ALTER TABLE work_schedules 
ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX IF NOT EXISTS idx_work_schedules_is_active ON work_schedules(company_id, is_active);

-- =====================================================
-- COMMENTS
-- =====================================================
COMMENT ON COLUMN company_settings.work_mode IS 'Chế độ làm việc: FIXED_HOURS (giờ cố định) hoặc FLEXIBLE_SHIFT (linh hoạt/theo ca)';
COMMENT ON COLUMN company_settings.default_work_start_time IS 'Giờ bắt đầu làm việc mặc định (dùng cho FIXED_HOURS mode)';
COMMENT ON COLUMN company_settings.default_work_end_time IS 'Giờ kết thúc làm việc mặc định (dùng cho FIXED_HOURS mode)';
COMMENT ON COLUMN company_settings.default_break_minutes IS 'Thời gian nghỉ giải lao mặc định (phút) (dùng cho FIXED_HOURS mode)';
COMMENT ON TABLE work_mode_change_logs IS 'Audit log cho các thay đổi work mode của công ty';
COMMENT ON COLUMN work_schedules.is_active IS 'Trạng thái hoạt động của lịch làm việc (FALSE khi switch sang FIXED_HOURS)';
