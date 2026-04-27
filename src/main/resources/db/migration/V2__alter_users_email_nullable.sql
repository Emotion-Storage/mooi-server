-- users.email 을 NULL 허용으로 보정 (Apple 가입 지원)
ALTER TABLE users
    MODIFY COLUMN email varchar(255) NULL;

-- 소셜 계정 중복 방지 (soft delete 고려)
ALTER TABLE users
    ADD UNIQUE KEY uk_users_social_deleted_at (social_type, social_id, deleted_at);
