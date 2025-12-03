-- Change role and status from enum to varchar with USING clause
ALTER TABLE users ALTER COLUMN role TYPE VARCHAR(50) USING role::VARCHAR;
ALTER TABLE users ALTER COLUMN status TYPE VARCHAR(50) USING status::VARCHAR;
