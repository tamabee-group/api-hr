-- =====================================================
-- TAMABEE HR - CREATE TAMABEE TENANT DATABASE
-- Script để tạo database tamabee_tamabee cho Tamabee company
-- Chạy script này trước khi khởi động application
-- =====================================================

-- Tạo database tamabee_tamabee
CREATE DATABASE tamabee_tamabee
    WITH 
    OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.UTF-8'
    LC_CTYPE = 'en_US.UTF-8'
    TEMPLATE = template0;

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE tamabee_tamabee TO postgres;

-- =====================================================
-- HƯỚNG DẪN SỬ DỤNG:
-- 1. Kết nối PostgreSQL với user có quyền CREATE DATABASE
-- 2. Chạy script này: psql -U postgres -f create_tamabee_database.sql
-- 3. Sau đó Flyway sẽ tự động chạy migration V1__init.sql
-- =====================================================
