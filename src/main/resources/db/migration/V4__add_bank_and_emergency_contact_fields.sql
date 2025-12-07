-- Add bank info and emergency contact fields to user_profiles table
ALTER TABLE user_profiles
ADD COLUMN IF NOT EXISTS bank_name VARCHAR(255),
ADD COLUMN IF NOT EXISTS bank_account VARCHAR(255),
ADD COLUMN IF NOT EXISTS bank_account_name VARCHAR(255),
ADD COLUMN IF NOT EXISTS emergency_contact_name VARCHAR(255),
ADD COLUMN IF NOT EXISTS emergency_contact_phone VARCHAR(50),
ADD COLUMN IF NOT EXISTS emergency_contact_relation VARCHAR(100),
ADD COLUMN IF NOT EXISTS emergency_contact_address TEXT;
