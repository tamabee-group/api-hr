-- Xóa constraint cũ
ALTER TABLE user_profiles DROP CONSTRAINT IF EXISTS fkjcad5nfve11khsnpwj1mv8frj;

-- Thêm lại constraint với ON DELETE CASCADE
ALTER TABLE user_profiles
    ADD CONSTRAINT fk_user_profiles_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE;
